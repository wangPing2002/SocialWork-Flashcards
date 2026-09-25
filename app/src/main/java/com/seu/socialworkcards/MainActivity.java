            // 多关键词采用 AND 逻辑：每个关键词至少命中一个字段，避免结果过泛。
            if(tokenScore==0)return null;
            total+=tokenScore;
            if(tokenScore>bestField){bestField=tokenScore;bestReason=reason;}
        }

        String full=raw.trim().toLowerCase(Locale.ROOT);
        if(c.question!=null&&c.question.toLowerCase(Locale.ROOT).contains(full))total+=80;
        if(c.topic!=null&&c.topic.toLowerCase(Locale.ROOT).contains(full))total+=60;
        return new SearchHit(c,total,bestReason);
    }

    CharSequence highlightMatches(String text,String raw){
        if(text==null)return "";
        android.text.SpannableString s=new android.text.SpannableString(text);
        String lower=text.toLowerCase(Locale.ROOT);
        for(String token:queryTokens(raw)){
            for(String v:tokenVariants(token)){
                if(v.isEmpty())continue;
                String vv=v.toLowerCase(Locale.ROOT);int from=0;
                while(true){
                    int at=lower.indexOf(vv,from);if(at<0)break;
                    s.setSpan(new android.text.style.ForegroundColorSpan(TEAL_DARK),at,at+vv.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    s.setSpan(new android.text.style.StyleSpan(Typeface.BOLD),at,at+vv.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    from=at+vv.length();
                }
            }
        }
        return s;
    }

    View searchCardRow(SearchHit hit,String key){
        Card c=hit.card;
        LinearLayout item=box(SURFACE,13,15);item.setBackground(strokeShape(SURFACE,16,LINE));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text=new LinearLayout(this);text.setOrientation(LinearLayout.VERTICAL);

        TextView title=tv("",15,INK,true);title.setText(highlightMatches(c.question,key));text.addView(title);
        String state=engine.learned(c)?store.state(c.id).tag:"未学习";
        TextView topic=tv("",11,MUTED,false);topic.setText(highlightMatches(c.topic+" · "+c.kind+" · "+state+(store.isFavorite(c.id)?" · ★ 收藏":""),key));margin(topic,0,4,0,0);text.addView(topic);
        TextView reason=tv("匹配："+hit.reason,10,TEAL_DARK,true);margin(reason,0,4,0,0);text.addView(reason);

        row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=tv("›",24,MUTED,false);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(44)));
        item.addView(row);
        item.setOnClickListener(v->showCategoryCards(c.topic,new ArrayList<>(Collections.singletonList(c))));
        margin(item,0,0,0,7);
        return item;
    }

    int favoriteCount(){int n=0;for(Card c:repo.cards)if(store.isFavorite(c.id))n++;return n;}


View topicRow(String name,List<Card> cards){
    LinearLayout row=box(SURFACE,13,18);row.setBackground(strokeShape(SURFACE,18,LINE));int learned=0,mast=0,fam=0,fuz=0;for(Card c:cards){String tag=store.state(c.id).tag;if(!tag.equals("未学习"))learned++;if(tag.equals("掌握"))mast++;else if(tag.equals("熟悉"))fam++;else if(tag.equals("模糊"))fuz++;}
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout iconBox=new LinearLayout(this);iconBox.setGravity(Gravity.CENTER);iconBox.setBackground(shape(categorySoft(name),14));iconBox.addView(iconView(categoryIconRes(name),22,categoryColor(name)),new LinearLayout.LayoutParams(dp(24),dp(24)));top.addView(iconBox,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(11),0,0,0);txt.addView(tv(name,17,INK,true));txt.addView(tv("已学习 "+learned+" / "+cards.size(),11,MUTED,false));top.addView(txt,new LinearLayout.LayoutParams(0,-2,1));
    String st=fuz>Math.max(2,learned/4)?"模糊较多":learned==0?"待学习":learned==cards.size()?"已学习":"继续学习";TextView status=tv(st,10,st.equals("模糊较多")?ORANGE:TEAL_DARK,true);status.setBackground(shape(st.equals("模糊较多")?ORANGE_SOFT:TEAL_SOFT,12));status.setPadding(dp(8),dp(4),dp(8),dp(4));top.addView(status);row.addView(top);
    ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,cards.size()));pb.setProgress(learned);pb.getProgressDrawable().setTint(TEAL);margin(pb,dp(59),7,0,4);row.addView(pb,new LinearLayout.LayoutParams(-1,dp(5)));
    TextView small=tv("掌握 "+mast+"   熟悉 "+fam+"   模糊 "+fuz,10,MUTED,false);small.setPadding(dp(59),0,0,0);row.addView(small);row.setOnClickListener(v->showCategoryCards(name,cards));margin(row,0,0,0,8);return row;
}
    void openCategory(String name,List<Card> cards){
        List<Card> learned=engine.reviewOnlyQueue(cards,Math.max(store.dailyGoal(),cards.size()));
        if(learned.isEmpty()){
            new AlertDialog.Builder(this).setTitle(name).setMessage("这个专题还没有已学习卡片。是否从本专题开始学习？").setNegativeButton("取消",null).setPositiveButton("开始学习",(d,w)->{List<Card> unseen=new ArrayList<>();for(Card c:cards)if(engine.unseen(c))unseen.add(c);Collections.shuffle(unseen);queue=new ArrayList<>(unseen.subList(0,Math.min(store.dailyGoal(),unseen.size())));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();}).show();
        }else{queue=learned;qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();}
    }

    LinkedHashMap<String,List<Card>> categories(){LinkedHashMap<String,List<Card>> m=new LinkedHashMap<>();String[] order={"社会工作原理","社会工作实务","督导与管理","个案工作","小组工作","社区工作"};for(String x:order)m.put(x,new ArrayList<>());for(Card c:repo.cards)m.get(categoryOf(c)).add(c);return m;}
    String categoryOf(Card c){String t=c.topic; if(t.contains("督导")||t.contains("项目")||t.contains("管理")||t.contains("预算")||t.contains("机构")||t.contains("需求评估")||t.contains("质量控制"))return "督导与管理";if(t.contains("小组"))return "小组工作";if(t.contains("社区"))return "社区工作";if(t.contains("危机")||t.contains("任务中心")||t.contains("行为治疗")||t.contains("叙事")||t.contains("个案")||t.contains("家庭治疗")||t.contains("理性情绪")||t.contains("社会心理")||t.contains("诊断学派"))return "个案工作";if(t.contains("企业")||t.contains("农村")||t.contains("退役")||t.contains("新就业")||t.contains("老年")||t.contains("儿童")||t.contains("妇女")||t.contains("残疾")||t.contains("医务")||t.contains("矫正")||t.contains("基层治理")||t.contains("信访"))return "社会工作实务";return "社会工作原理";}
    int categoryColor(String n){if(n.equals("社会工作原理"))return TEAL_DARK;if(n.equals("社会工作实务"))return Color.rgb(36,145,218);if(n.equals("督导与管理"))return Color.rgb(112,100,220);if(n.equals("个案工作"))return Color.rgb(226,139,58);if(n.equals("小组工作"))return Color.rgb(223,95,105);return TEAL;}
    int categorySoft(String n){if(n.equals("社会工作原理"))return TEAL_SOFT;if(n.equals("社会工作实务"))return BLUE_SOFT;if(n.equals("督导与管理"))return PURPLE_SOFT;if(n.equals("个案工作"))return Color.rgb(255,242,226);if(n.equals("小组工作"))return Color.rgb(255,236,239);return TEAL_SOFT;}


void showStats(){
    currentScreen="stats";categoryOpenedFromStudy=false;
    shell("学习统计","看见节奏，而不是只看数量。","统计");

    LinearLayout overview=box(SURFACE,15,20);overview.setBackground(strokeShape(SURFACE,20,LINE));
    overview.addView(tv("学习概览",17,INK,true));
    LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);margin(metrics,0,12,0,2);
    metrics.addView(bigMetric("已学习",String.valueOf(learnedCount()),"张"),new LinearLayout.LayoutParams(0,-2,1));
    metrics.addView(bigMetric("收藏",String.valueOf(favoriteCount()),"张"),new LinearLayout.LayoutParams(0,-2,1));
    metrics.addView(bigMetric("连续",String.valueOf(store.streakDays()),"天"),new LinearLayout.LayoutParams(0,-2,1));overview.addView(metrics);body.addView(overview);

    View trendTitle=sectionHeader("近 7 天",null,null);margin(trendTitle,2,18,0,8);body.addView(trendTitle);
    LinearLayout chart=box(SURFACE,14,18);chart.setBackground(strokeShape(SURFACE,18,LINE));chart.addView(makeWeekChart());body.addView(chart);

    int mast=masteryCount("掌握"),fam=masteryCount("熟悉"),fuz=masteryCount("模糊"),un=Math.max(0,repo.cards.size()-mast-fam-fuz);
    View distTitle=sectionHeader("掌握分布",null,null);margin(distTitle,2,18,0,8);body.addView(distTitle);
    LinearLayout dist=box(SURFACE,14,18);dist.setBackground(strokeShape(SURFACE,18,LINE));
    dist.addView(masteryRow("掌握",mast,repo.cards.size(),TEAL_DARK));View s1=new Space(this);dist.addView(s1,new LinearLayout.LayoutParams(1,dp(8)));
    dist.addView(masteryRow("熟悉",fam,repo.cards.size(),TEAL));View s2=new Space(this);dist.addView(s2,new LinearLayout.LayoutParams(1,dp(8)));
    dist.addView(masteryRow("模糊",fuz,repo.cards.size(),ORANGE));View s3=new Space(this);dist.addView(s3,new LinearLayout.LayoutParams(1,dp(8)));
    dist.addView(masteryRow("未学习",un,repo.cards.size(),Color.rgb(190,201,197)));body.addView(dist);

    View weakTitle=sectionHeader("薄弱卡片", "前 10 张", null);margin(weakTitle,2,18,0,8);body.addView(weakTitle);
    List<Card> weak=new ArrayList<>();for(Card x:repo.cards)if(engine.learned(x))weak.add(x);weak.sort((a,b)->Double.compare(engine.difficulty(b),engine.difficulty(a)));
    if(weak.isEmpty()){LinearLayout empty=box(SURFACE,18,18);empty.setBackground(strokeShape(SURFACE,18,LINE));TextView e=tv("完成一些学习后，这里会出现需要优先复习的卡片。",13,MUTED,false);e.setGravity(Gravity.CENTER);empty.addView(e);body.addView(empty);}else{
        for(int i=0;i<Math.min(10,weak.size());i++){Card x=weak.get(i);LinearLayout wr=box(SURFACE,12,16);wr.setBackground(strokeShape(SURFACE,16,LINE));LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView rank=tv(String.valueOf(i+1),11,i<3?Color.WHITE:MUTED,true);rank.setGravity(Gravity.CENTER);rank.setBackground(shape(i==0?RED:i==1?ORANGE:i==2?Color.rgb(224,164,54):Color.rgb(238,242,240),12));line.addView(rank,new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,dp(8),0);TextView qq=tv(x.question,13,INK,true);qq.setMaxLines(2);tx.addView(qq);tx.addView(tv(x.topic,10,MUTED,false));line.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView pct=tv(Math.round(engine.difficulty(x)*100)+"%",11,i<3?ORANGE:MUTED,true);line.addView(pct);wr.addView(line);wr.setOnClickListener(v->{queue=engine.reviewOnlyQueue(Collections.singletonList(x),Math.min(3,store.dailyGoal()));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});margin(wr,0,0,0,7);body.addView(wr);}
    }
}

    View bigMetric(String label,String num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);TextView b=tv(num,25,INK,true);b.setGravity(Gravity.CENTER);l.addView(b);TextView c=tv(unit,10,MUTED,false);c.setGravity(Gravity.CENTER);l.addView(c);return l;}
    View makeWeekChart(){LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setGravity(Gravity.BOTTOM);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,-6);int max=1;int[] vals=new int[7];String[] labs=new String[7];SimpleDateFormat f=new SimpleDateFormat("M/d",Locale.getDefault());for(int i=0;i<7;i++){Date d=cal.getTime();vals[i]=store.dayDone(d);labs[i]=f.format(d);max=Math.max(max,vals[i]);cal.add(Calendar.DAY_OF_YEAR,1);}for(int i=0;i<7;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView val=tv(String.valueOf(vals[i]),10,MUTED,true);val.setGravity(Gravity.CENTER);col.addView(val);View bar=new View(this);bar.setBackground(shape(i==6?TEAL:Color.rgb(151,222,205),8));int h=dp(18+(int)(90.0*vals[i]/max));col.addView(bar,new LinearLayout.LayoutParams(dp(28),h));TextView lab=tv(labs[i],9,MUTED,false);lab.setGravity(Gravity.CENTER);col.addView(lab);outer.addView(col,new LinearLayout.LayoutParams(0,dp(145),1));}return outer;}


void showSettings(){
    currentScreen="settings";categoryOpenedFromStudy=false;
    shell("我的","学习偏好、数据与内容管理。","我的");

    View learnTitle=sectionHeader("学习设置",null,null);margin(learnTitle,2,4,0,8);body.addView(learnTitle);
    LinearLayout goalCard=box(SURFACE,15,18);goalCard.setBackground(strokeShape(SURFACE,18,LINE));
    LinearLayout goalTop=new LinearLayout(this);goalTop.setGravity(Gravity.CENTER_VERTICAL);LinearLayout gt=new LinearLayout(this);gt.setOrientation(LinearLayout.VERTICAL);gt.addView(tv("每日学习量",16,INK,true));gt.addView(tv("每天计划完成的卡片数量",11,MUTED,false));goalTop.addView(gt,new LinearLayout.LayoutParams(0,-2,1));
    EditText goal=new EditText(this);goal.setInputType(2);goal.setText(String.valueOf(store.dailyGoal()));goal.setGravity(Gravity.CENTER);goal.setTextSize(17);goal.setTextColor(INK);goal.setBackground(strokeShape(Color.rgb(249,251,250),13,LINE));goalTop.addView(goal,new LinearLayout.LayoutParams(dp(76),dp(46)));goalCard.addView(goalTop);
    Button save=btn("保存设置",TEAL,Color.WHITE);margin(save,0,12,0,0);save.setOnClickListener(v->{int g=parse(goal,30);store.setDailyGoal(g);Toast.makeText(this,"已保存为每天 "+store.dailyGoal()+" 张",Toast.LENGTH_SHORT).show();showSettings();});goalCard.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));body.addView(goalCard);

    View dataTitle=sectionHeader("数据与迁移",null,null);margin(dataTitle,2,18,0,8);body.addView(dataTitle);
    LinearLayout data=box(SURFACE,15,18);data.setBackground(strokeShape(SURFACE,18,LINE));data.addView(tv("完整备份",16,INK,true));TextView d=tv("包含学习进度、最近10次记录、收藏、本地修正和内容反馈。安装正式新版前建议先导出。",12,MUTED,false);margin(d,0,5,0,10);data.addView(d);LinearLayout dr=new LinearLayout(this);dr.setOrientation(LinearLayout.HORIZONTAL);Button backup=btn("导出备份",TEAL,Color.WHITE),restore=btn("恢复备份",TEAL_SOFT,TEAL_DARK);backup.setOnClickListener(v->exportBackup());restore.setOnClickListener(v->confirmImportBackup());dr.addView(backup,new LinearLayout.LayoutParams(0,dp(46),1));dr.addView(gap(8));dr.addView(restore,new LinearLayout.LayoutParams(0,dp(46),1));data.addView(dr);body.addView(data);

    View contentTitle=sectionHeader("内容管理",null,null);margin(contentTitle,2,18,0,8);body.addView(contentTitle);
    LinearLayout fb=box(SURFACE,15,18);fb.setBackground(strokeShape(SURFACE,18,LINE));LinearLayout fr=new LinearLayout(this);fr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout ft=new LinearLayout(this);ft.setOrientation(LinearLayout.VERTICAL);ft.addView(tv("内容反馈",16,INK,true));ft.addView(tv("待校对 "+feedback.pendingCount()+" 条 · 可导出 JSON 统一修正",11,MUTED,false));fr.addView(ft,new LinearLayout.LayoutParams(0,-2,1));Button export=btn("导出",TEAL_SOFT,TEAL_DARK);export.setOnClickListener(v->exportFeedback());fr.addView(export,new LinearLayout.LayoutParams(dp(76),dp(42)));fb.addView(fr);body.addView(fb);

    LinearLayout algo=box(Color.rgb(242,248,245),14,18);margin(algo,0,12,0,0);algo.addView(tv("复习机制",15,TEAL_DARK,true));TextView rules=tv("已学习卡可重复出现；最近10次中多次“不清楚”的卡片会获得更高优先级；所有学习数据默认只保存在本机。",12,MUTED,false);margin(rules,0,5,0,0);algo.addView(rules);body.addView(algo);

    View dangerTitle=sectionHeader("其他",null,null);margin(dangerTitle,2,18,0,8);body.addView(dangerTitle);
    Button reset=btn("清空学习进度",RED_SOFT,Color.rgb(160,75,76));reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("将删除所有卡片学习记录、最近10次记录和每日设置。收藏、内容反馈与本地修正不会被此按钮删除。建议先导出完整备份。").setNegativeButton("取消",null).setPositiveButton("清空",(dialog,which)->{store.clearAll();queue.clear();showSettings();}).show());body.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));
}

    void showExample(Card c){JSONObject d=repo.detail(c.topic);String x=d==null?"":d.optString("example");new AlertDialog.Builder(this).setTitle(c.topic+" · 例子").setMessage(x.isEmpty()?"当前资料没有单列例子。":x).setPositiveButton("关闭",null).show();}
    void showFull(Card c){JSONObject d=repo.detail(c.topic);if(d==null)return;String[] labels={"名词解释","简答","论述/案例"},keys={"name","simple","essay"};new AlertDialog.Builder(this).setTitle(c.topic+" · 完整答案").setItems(labels,(dlg,which)->new AlertDialog.Builder(this).setTitle(labels[which]).setMessage(d.optString(keys[which],"暂无")).setPositiveButton("关闭",null).show()).setNegativeButton("关闭",null).show();}
    void showHistory(Card c){StudyStore.State s=store.state(c.id);StringBuilder b=new StringBuilder();int i=1;SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault());for(StudyStore.Rec r:s.history)b.append(i++).append(". ").append(r.ok?"熟悉":"不清楚").append("  ").append(f.format(new Date(r.ts))).append("\n");if(s.history.isEmpty())b.append("暂无记录");b.append("\n薄弱度：").append(Math.round(engine.difficulty(c)*100)).append("%");new AlertDialog.Builder(this).setTitle("最近10次 · "+c.topic).setMessage(b.toString()).setPositiveButton("关闭",null).show();}


void showCategoryCards(String name,List<Card> cards){
    currentScreen=categoryOpenedFromStudy?"categoryFromStudy":"category";
    shell(name,cards.size()+" 张卡片 · 查看与学习","卡片库");
    int learned=0;for(Card c:cards)if(engine.learned(c))learned++;
    LinearLayout summary=box(Color.rgb(240,248,245),14,18);LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout st=new LinearLayout(this);st.setOrientation(LinearLayout.VERTICAL);st.addView(tv("专题进度",14,TEAL_DARK,true));st.addView(tv("已学习 "+learned+" / "+cards.size(),12,MUTED,false));sr.addView(st,new LinearLayout.LayoutParams(0,-2,1));Button begin=btn(learned==0?"开始专题":"继续专题",TEAL,Color.WHITE);begin.setTextSize(12);begin.setOnClickListener(v->openCategory(name,cards));sr.addView(begin,new LinearLayout.LayoutParams(dp(104),dp(42)));summary.addView(sr);margin(summary,0,0,0,12);body.addView(summary);

    for(Card c:cards){
        LinearLayout item=box(SURFACE,13,16);item.setBackground(strokeShape(SURFACE,16,LINE));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);TextView q=tv(c.question,14,INK,true);q.setMaxLines(3);txt.addView(q);txt.addView(tv(c.kind+" · "+(engine.learned(c)?store.state(c.id).tag:"未学习")+(store.isFavorite(c.id)?" · 已收藏":"")+(feedback.hasCardOverride(c.id)?" · 已修正":""),10,MUTED,false));line.addView(txt,new LinearLayout.LayoutParams(0,-2,1));item.addView(line);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button learn=btn(engine.learned(c)?"复习":"学习",TEAL_SOFT,TEAL_DARK),edit=btn("编辑",Color.rgb(247,249,248),MUTED),report=btn("反馈",Color.rgb(247,249,248),MUTED);learn.setTextSize(12);edit.setTextSize(12);report.setTextSize(12);learn.setOnClickListener(v->{categoryOpenedFromStudy=false;queue=engine.learned(c)?engine.reviewOnlyQueue(Collections.singletonList(c),Math.min(3,store.dailyGoal())):new ArrayList<>(Collections.singletonList(c));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});edit.setOnClickListener(v->editCard(c));report.setOnClickListener(v->reportCard(c));actions.addView(learn,new LinearLayout.LayoutParams(0,dp(38),1));actions.addView(gap(5));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(38),1));actions.addView(gap(5));actions.addView(report,new LinearLayout.LayoutParams(0,dp(38),1));margin(actions,0,9,0,0);item.addView(actions);margin(item,0,0,0,7);body.addView(item);
    }
}

    void reportCard(Card c){
        String[] types={"概念或答案错误","答案不完整","题目表述不准确","真题信息有误","出处/章节有误","例子或扩展答案有误","知识点重复或冲突","其他"};
        new AlertDialog.Builder(this).setTitle("内容反馈").setItems(types,(d,which)->{
            final EditText note=new EditText(this);note.setHint("可补充：哪里错了、你认为应如何修改、对应教材页码等");note.setMinLines(4);note.setGravity(Gravity.TOP);note.setPadding(dp(14),dp(10),dp(14),dp(10));
            new AlertDialog.Builder(this).setTitle(types[which]).setView(note).setNegativeButton("取消",null).setPositiveButton("保存标记",(x,w)->{feedback.addReport(c,types[which],note.getText().toString().trim());Toast.makeText(this,"已保存到本地内容反馈库",Toast.LENGTH_SHORT).show();}).show();
        }).setNegativeButton("取消",null).show();
    }

    void editCard(Card c){
        ScrollView sv=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),dp(8),dp(16),dp(8));
        form.addView(tv("题目",13,MUTED,true));EditText q=new EditText(this);q.setText(c.question);q.setMinLines(2);form.addView(q);
        TextView la=tv("答案要点（每行一个要点）",13,MUTED,true);margin(la,0,10,0,0);form.addView(la);EditText a=new EditText(this);StringBuilder ab=new StringBuilder();for(String x:c.bullets){if(ab.length()>0)ab.append("\n");ab.append(x);}a.setText(ab.toString());a.setMinLines(6);a.setGravity(Gravity.TOP);form.addView(a);
        TextView lt=tv("记忆提示",13,MUTED,true);margin(lt,0,10,0,0);form.addView(lt);EditText tip=new EditText(this);tip.setText(c.tip);form.addView(tip);
        TextView lo=tv("出处",13,MUTED,true);margin(lo,0,10,0,0);form.addView(lo);EditText origin=new EditText(this);origin.setText(c.origin);form.addView(origin);
        Button ext=btn("编辑例子 / 完整答案",TEAL_SOFT,TEAL_DARK);margin(ext,0,12,0,0);ext.setOnClickListener(v->editDetail(c));form.addView(ext,new LinearLayout.LayoutParams(-1,dp(46)));sv.addView(form);
        new AlertDialog.Builder(this).setTitle("本地修正 · "+c.topic).setView(sv).setNegativeButton("取消",null).setNeutralButton("标记有误",(d,w)->reportCard(c)).setPositiveButton("保存",(d,w)->{
            List<String> bullets=new ArrayList<>();for(String x:a.getText().toString().split("\n"))if(!x.trim().isEmpty())bullets.add(x.trim());
            feedback.saveCardOverride(c,q.getText().toString().trim(),bullets,tip.getText().toString().trim(),origin.getText().toString().trim());
            Toast.makeText(this,"已本地修正。建议同时导出反馈，以便后续统一修正源题库。",Toast.LENGTH_LONG).show();
        }).show();
    }

    void editDetail(Card c){
        JSONObject d=repo.detail(c.topic);String[] labels={"例子","名词解释完整答案","简答完整答案","论述/案例完整答案"},keys={"example","name","simple","essay"};
        new AlertDialog.Builder(this).setTitle("选择要修改的扩展内容").setItems(labels,(x,which)->{EditText e=new EditText(this);e.setText(d==null?"":d.optString(keys[which],""));e.setMinLines(10);e.setGravity(Gravity.TOP);e.setPadding(dp(14),dp(10),dp(14),dp(10));new AlertDialog.Builder(this).setTitle(labels[which]).setView(e).setNegativeButton("取消",null).setPositiveButton("保存",(dd,ww)->{feedback.saveDetailOverride(c.topic,keys[which],e.getText().toString());Toast.makeText(this,"扩展内容已本地修正",Toast.LENGTH_SHORT).show();}).show();}).setNegativeButton("取消",null).show();
    }

    void exportBackup(){
        pendingExport=backupManager.exportAll();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"社会工作闪卡_完整备份_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.getDefault()).format(new Date())+".json");startActivityForResult(i,EXPORT_BACKUP_REQ);
    }

    void confirmImportBackup(){
        new AlertDialog.Builder(this).setTitle("恢复完整备份").setMessage("恢复会用备份中的学习记录、设置、本地修正和内容反馈覆盖当前同类数据。建议先导出当前数据作为保险。是否继续？").setNegativeButton("取消",null).setPositiveButton("选择备份文件",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,IMPORT_BACKUP_REQ);}).show();
    }

    String readText(android.net.Uri uri) throws Exception {
        java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();return out.toString("UTF-8");
    }

    void exportFeedback(){
        pendingExport=feedback.exportBundle();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"社会工作闪卡_内容反馈_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.getDefault()).format(new Date())+".json");startActivityForResult(i,EXPORT_FEEDBACK_REQ);
    }

    @Override public void onBackPressed(){