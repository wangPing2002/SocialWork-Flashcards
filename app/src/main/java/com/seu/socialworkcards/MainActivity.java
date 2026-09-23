package com.seu.socialworkcards;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(246,250,248), SURFACE=Color.WHITE, INK=Color.rgb(20,43,39), MUTED=Color.rgb(110,126,123);
    final int TEAL=Color.rgb(20,166,143), TEAL_DARK=Color.rgb(11,132,116), TEAL_SOFT=Color.rgb(229,247,242);
    final int LINE=Color.rgb(227,236,233), ORANGE=Color.rgb(231,111,47), ORANGE_SOFT=Color.rgb(255,242,232), RED=Color.rgb(224,78,82), RED_SOFT=Color.rgb(253,235,236), BLUE_SOFT=Color.rgb(233,245,255), PURPLE_SOFT=Color.rgb(240,238,255), GOLD_SOFT=Color.rgb(252,245,226);
    CardRepository repo; StudyStore store; ReviewEngine engine; ContentFeedbackStore feedback;
    LinearLayout root,body,nav; List<Card> queue=new ArrayList<>(); int qIndex=0; boolean revealed=false;
    String pendingExport=null; static final int EXPORT_FEEDBACK_REQ=91;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        try{feedback=new ContentFeedbackStore(this);repo=new CardRepository(this,feedback);store=new StudyStore(this);engine=new ReviewEngine(store);showHome();}
        catch(Exception e){TextView t=new TextView(this);t.setPadding(24,24,24,24);t.setText("启动失败："+e);setContentView(t);}
    }

    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable strokeShape(int color,int radius,int strokeColor){GradientDrawable g=shape(color,radius);g.setStroke(dp(1),strokeColor);return g;}
    TextView tv(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setLineSpacing(0,1.15f);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    Button btn(String s,int bgc,int tc){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(tc);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(bgc,15));b.setPadding(dp(12),0,dp(12),0);return b;}
    LinearLayout box(int color,int pad,int radius){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(pad),dp(pad),dp(pad),dp(pad));l.setBackground(shape(color,radius));return l;}
    void margin(View v,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);}
    Space gap(int w){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return s;}

    void shell(String title,String subtitle,String active){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);setContentView(root);
        LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setPadding(dp(22),dp(18),dp(22),dp(12));
        h.addView(tv(title,29,INK,true));if(subtitle!=null&&!subtitle.isEmpty()){TextView s=tv(subtitle,14,MUTED,false);margin(s,0,4,0,0);h.addView(s);}root.addView(h);
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),0,dp(16),dp(92));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(8),dp(4),dp(8),dp(7));nav.setBackgroundColor(Color.WHITE);
        addNav("⌂\n学习","学习".equals(active),v->showHome());addNav("▣\n卡片库","卡片库".equals(active),v->showLibrary());addNav("▥\n统计","统计".equals(active),v->showStats());addNav("◯\n我的","我的".equals(active),v->showSettings());
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(68)));
    }
    void addNav(String label,boolean active,View.OnClickListener click){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(active?TEAL_DARK:MUTED);b.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);b.setBackgroundColor(Color.TRANSPARENT);b.setGravity(Gravity.CENTER);b.setOnClickListener(click);nav.addView(b,new LinearLayout.LayoutParams(0,-1,1));}

    int learnedCount(){int n=0;for(Card c:repo.cards)if(engine.learned(c))n++;return n;}
    int masteryCount(String tag){int n=0;for(Card c:repo.cards)if(store.state(c.id).tag.equals(tag))n++;return n;}
    int remainingToday(){return Math.max(0,store.dailyGoal()-store.todayDone());}

    void showHome(){
        shell("社会工作闪卡","社会工作考研智能学习","学习");
        LinearLayout hero=box(Color.rgb(239,250,247),18,22);margin(hero,0,2,0,12);
        TextView quote=tv("“ 社会工作，让更多人被看见。 ”",15,Color.rgb(78,121,114),false);hero.addView(quote);
        TextView slogan=tv("积累 · 理解 · 上岸",13,TEAL_DARK,true);slogan.setGravity(Gravity.RIGHT);margin(slogan,0,8,0,0);hero.addView(slogan);body.addView(hero);

        LinearLayout summary=box(SURFACE,18,22);summary.setBackground(strokeShape(SURFACE,22,LINE));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);left.addView(tv("今日学习量",18,INK,true));LinearLayout nrow=new LinearLayout(this);nrow.setGravity(Gravity.BOTTOM);TextView n=tv(String.valueOf(store.dailyGoal()),48,TEAL_DARK,true);nrow.addView(n);TextView z=tv(" 张",15,TEAL_DARK,true);z.setPadding(0,0,0,dp(7));nrow.addView(z);left.addView(nrow);left.addView(tv("已完成 "+store.todayDone()+" / "+store.dailyGoal(),13,MUTED,false));
        top.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout mini=new LinearLayout(this);mini.setOrientation(LinearLayout.HORIZONTAL);mini.addView(miniStat("已学习",learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));mini.addView(miniStat("待学习",repo.cards.size()-learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));mini.addView(miniStat("连续",store.streakDays(),"天"),new LinearLayout.LayoutParams(0,-2,1));top.addView(mini,new LinearLayout.LayoutParams(0,-2,2));summary.addView(top);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(store.dailyGoal());p.setProgress(Math.min(store.todayDone(),store.dailyGoal()));p.getProgressDrawable().setTint(TEAL);margin(p,0,12,0,12);summary.addView(p,new LinearLayout.LayoutParams(-1,dp(7)));
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);Button start=btn("▶  开始学习",TEAL,Color.WHITE);Button cont=btn("▣  继续学习",TEAL_SOFT,TEAL_DARK);cont.setBackground(strokeShape(TEAL_SOFT,15,Color.rgb(176,228,215)));start.setOnClickListener(v->startNewSession());cont.setOnClickListener(v->continueSession());buttons.addView(start,new LinearLayout.LayoutParams(0,dp(58),1));buttons.addView(gap(10));buttons.addView(cont,new LinearLayout.LayoutParams(0,dp(58),1));summary.addView(buttons);
        TextView rule=tv("🎓  复习仅从已学习内容中抽取；薄弱卡可重复出现。",12,TEAL_DARK,false);rule.setBackground(shape(Color.rgb(237,249,246),12));rule.setPadding(dp(12),dp(9),dp(12),dp(9));margin(rule,0,12,0,0);summary.addView(rule);body.addView(summary);

        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);TextView st=tv("学习专题",20,INK,true);titleRow.addView(st,new LinearLayout.LayoutParams(0,-2,1));TextView more=tv("全部专题  ›",13,MUTED,false);more.setOnClickListener(v->showLibrary());titleRow.addView(more);margin(titleRow,2,16,2,8);body.addView(titleRow);
        LinkedHashMap<String,List<Card>> cats=categories();int shown=0;for(Map.Entry<String,List<Card>> e:cats.entrySet()){if(shown++>=4)break;body.addView(topicRow(e.getKey(),e.getValue()));}
    }
    View miniStat(String label,int num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);l.addView(tv(label,11,MUTED,false));TextView n=tv(num+"",22,INK,true);n.setGravity(Gravity.CENTER);l.addView(n);TextView u=tv(unit,10,MUTED,false);u.setGravity(Gravity.CENTER);l.addView(u);return l;}

    void startNewSession(){
        int count=remainingToday();if(count<=0)count=store.dailyGoal();
        queue=engine.learningQueue(repo.cards,count);qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();
    }
    void continueSession(){
        if(!store.hasActiveSession()){Toast.makeText(this,"当前没有中断的学习进度，将开始新的学习。",Toast.LENGTH_SHORT).show();startNewSession();return;}
        queue=new ArrayList<>();for(String id:store.sessionIds()){Card c=repo.byId(id);if(c!=null)queue.add(c);}qIndex=Math.min(store.sessionIndex(),queue.size());revealed=false;showStudy();
    }

    void showStudy(){
        shell("学习中","仅复习已学习内容 · 新卡只在首次学习时出现","学习");
        if(queue.isEmpty()||qIndex>=queue.size()){store.clearSession();LinearLayout done=box(SURFACE,24,22);done.addView(tv("✓ 本轮学习完成",22,INK,true));TextView d=tv("今天已完成 "+store.todayDone()+" 张。薄弱卡会依据最近10次记录继续提高后续出现概率。",14,MUTED,false);margin(d,0,8,0,14);done.addView(d);Button again=btn("继续加练",TEAL,Color.WHITE);again.setOnClickListener(v->startNewSession());done.addView(again,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(done);return;}
        Card c=queue.get(qIndex);StudyStore.State s=store.state(c.id);
        LinearLayout progress=box(SURFACE,14,18);TextView pt=tv("今日学习量  "+store.todayDone()+" / "+store.dailyGoal(),16,INK,true);progress.addView(pt);ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,queue.size()));pb.setProgress(qIndex);pb.getProgressDrawable().setTint(TEAL);margin(pb,0,8,0,0);progress.addView(pb,new LinearLayout.LayoutParams(-1,dp(6)));margin(progress,0,0,0,12);body.addView(progress);

        LinearLayout card=box(SURFACE,18,24);card.setBackground(strokeShape(SURFACE,24,LINE));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(510));cp.setMargins(0,0,0,dp(12));card.setLayoutParams(cp);
        LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);TextView kind=tv(c.kind,12,MUTED,false);meta.addView(kind,new LinearLayout.LayoutParams(0,-2,1));TextView status=tv(engine.learned(c)?"已学习":"新学习",11,engine.learned(c)?TEAL_DARK:ORANGE,true);status.setBackground(shape(engine.learned(c)?TEAL_SOFT:ORANGE_SOFT,14));status.setPadding(dp(10),dp(5),dp(10),dp(5));meta.addView(status);card.addView(meta);
        TextView topic=tv(c.topic,13,Color.rgb(126,143,139),false);margin(topic,0,12,0,0);card.addView(topic);
        TextView q=tv(c.question,24,INK,true);q.setGravity(Gravity.CENTER);margin(q,dp(4),dp(28),dp(4),dp(12));card.addView(q);
        ScrollView mid=new ScrollView(this);LinearLayout inner=new LinearLayout(this);inner.setOrientation(LinearLayout.VERTICAL);inner.setGravity(Gravity.CENTER_HORIZONTAL);inner.setPadding(dp(3),dp(8),dp(3),dp(8));
        if(!revealed){TextView hint=tv("✎  先回忆，再点击显示答案",15,MUTED,false);hint.setGravity(Gravity.CENTER);inner.addView(hint);}else{for(String x:c.bullets){TextView b=tv("•  "+x,15,INK,false);margin(b,0,0,0,8);inner.addView(b);}TextView src=tv("出处："+c.origin,11,MUTED,false);margin(src,0,12,0,0);inner.addView(src);inner.addView(historyMini(c));}
        mid.addView(inner);card.addView(mid,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);Button ex=btn("例子",Color.rgb(247,249,248),MUTED),full=btn("完整答案",Color.rgb(247,249,248),MUTED),hist=btn("最近10次",Color.rgb(247,249,248),MUTED);ex.setEnabled(c.hasExample);full.setEnabled(c.hasLong);ex.setOnClickListener(v->showExample(c));full.setOnClickListener(v->showFull(c));hist.setOnClickListener(v->showHistory(c));tools.addView(ex,new LinearLayout.LayoutParams(0,dp(46),1));tools.addView(gap(7));tools.addView(full,new LinearLayout.LayoutParams(0,dp(46),1));tools.addView(gap(7));tools.addView(hist,new LinearLayout.LayoutParams(0,dp(46),1));card.addView(tools);
        LinearLayout correct=new LinearLayout(this);correct.setOrientation(LinearLayout.HORIZONTAL);Button report=btn("⚑ 内容有误",RED_SOFT,Color.rgb(164,70,70)),edit=btn("✎ 本地修正",TEAL_SOFT,TEAL_DARK);report.setOnClickListener(v->reportCard(c));edit.setOnClickListener(v->editCard(c));correct.addView(report,new LinearLayout.LayoutParams(0,dp(44),1));correct.addView(gap(7));correct.addView(edit,new LinearLayout.LayoutParams(0,dp(44),1));margin(correct,0,7,0,0);card.addView(correct);body.addView(card);
        if(!revealed){Button show=btn("◉  显示答案",TEAL,Color.WHITE);show.setOnClickListener(v->{revealed=true;showStudy();});body.addView(show,new LinearLayout.LayoutParams(-1,dp(56)));}
        else{LinearLayout rates=new LinearLayout(this);rates.setOrientation(LinearLayout.HORIZONTAL);Button bad=btn("?  不清楚\n再多看几遍",ORANGE_SOFT,ORANGE),ok=btn("✓  熟悉\n我已经记住了",TEAL_SOFT,TEAL_DARK);bad.setOnClickListener(v->rate(c,false));ok.setOnClickListener(v->rate(c,true));rates.addView(bad,new LinearLayout.LayoutParams(0,dp(68),1));rates.addView(gap(10));rates.addView(ok,new LinearLayout.LayoutParams(0,dp(68),1));body.addView(rates);}
    }
    View historyMini(Card c){StudyStore.State s=store.state(c.id);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);TextView label=tv("最近10次",11,MUTED,false);margin(label,0,10,0,4);wrap.addView(label);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);for(StudyStore.Rec r:s.history){TextView x=tv(r.ok?"熟":"模",10,r.ok?TEAL_DARK:ORANGE,true);x.setGravity(Gravity.CENTER);x.setBackground(shape(r.ok?TEAL_SOFT:ORANGE_SOFT,11));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(31),dp(25));p.setMargins(dp(2),0,dp(2),0);row.addView(x,p);}wrap.addView(row);return wrap;}
    void rate(Card c,boolean ok){engine.rate(c,ok);qIndex++;revealed=false;store.saveSession(queue,qIndex);showStudy();}

    void showLibrary(){
        shell("卡片库","按专题管理学习内容","卡片库");
        LinearLayout sum=box(SURFACE,16,20);sum.setBackground(strokeShape(SURFACE,20,LINE));LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.addView(tv("今日学习量",16,INK,true));l.addView(tv(store.dailyGoal()+" 张",34,TEAL_DARK,true));l.addView(tv("已完成 "+store.todayDone()+" / "+store.dailyGoal(),12,MUTED,false));r.addView(l,new LinearLayout.LayoutParams(0,-2,1));Button cont=btn("▶  继续学习",TEAL,Color.WHITE);cont.setOnClickListener(v->continueSession());r.addView(cont,new LinearLayout.LayoutParams(dp(170),dp(58)));sum.addView(r);body.addView(sum);
        TextView rule=tv("🎓  复习仅从已学习的卡片中抽取，不包含未学习内容。",12,TEAL_DARK,false);rule.setBackground(shape(TEAL_SOFT,14));rule.setPadding(dp(12),dp(9),dp(12),dp(9));margin(rule,0,10,0,8);body.addView(rule);
        LinearLayout quality=box(SURFACE,14,16);quality.setBackground(strokeShape(SURFACE,16,LINE));LinearLayout qr=new LinearLayout(this);qr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout qtxt=new LinearLayout(this);qtxt.setOrientation(LinearLayout.VERTICAL);qtxt.addView(tv("内容校对与反馈",16,INK,true));qtxt.addView(tv("待处理标记 "+feedback.pendingCount()+" 条 · 可本地修正，也可导出给我统一校对",11,MUTED,false));qr.addView(qtxt,new LinearLayout.LayoutParams(0,-2,1));Button exp=btn("导出反馈",TEAL_SOFT,TEAL_DARK);exp.setOnClickListener(v->exportFeedback());qr.addView(exp,new LinearLayout.LayoutParams(dp(105),dp(44)));quality.addView(qr);margin(quality,0,0,0,9);body.addView(quality);
        EditText search=new EditText(this);search.setHint("搜索题目或知识点");search.setSingleLine(true);search.setTextSize(14);search.setBackground(strokeShape(Color.WHITE,14,LINE));search.setPadding(dp(14),0,dp(14),0);body.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);
        Runnable fill=()->{list.removeAllViews();String key=search.getText().toString().trim();for(Map.Entry<String,List<Card>> e:categories().entrySet()){List<Card> filtered=new ArrayList<>();for(Card c:e.getValue())if(key.isEmpty()||c.topic.contains(key)||c.question.contains(key))filtered.add(c);if(filtered.isEmpty())continue;list.addView(topicRow(e.getKey(),filtered));}};fill.run();search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){fill.run();}public void afterTextChanged(android.text.Editable e){}});
    }

    View topicRow(String name,List<Card> cards){
        LinearLayout row=box(SURFACE,15,18);row.setBackground(strokeShape(SURFACE,18,LINE));int learned=0,mast=0,fam=0,fuz=0;for(Card c:cards){String tag=store.state(c.id).tag;if(!tag.equals("未学习"))learned++;if(tag.equals("掌握"))mast++;else if(tag.equals("熟悉"))fam++;else if(tag.equals("模糊"))fuz++;}
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView icon=tv(categoryIcon(name),24,categoryColor(name),true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(categorySoft(name),15));top.addView(icon,new LinearLayout.LayoutParams(dp(54),dp(54)));LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(12),0,0,0);txt.addView(tv(name,18,INK,true));txt.addView(tv("已学习 "+learned+" / "+cards.size(),12,MUTED,false));top.addView(txt,new LinearLayout.LayoutParams(0,-2,1));String st=fuz>Math.max(2,learned/4)?"模糊较多":learned==0?"待学习":learned==cards.size()?"已学习":"继续学习";TextView chip=tv(st,11,st.equals("模糊较多")?ORANGE:TEAL_DARK,true);chip.setBackground(shape(st.equals("模糊较多")?ORANGE_SOFT:TEAL_SOFT,13));chip.setPadding(dp(9),dp(5),dp(9),dp(5));top.addView(chip);row.addView(top);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,cards.size()));pb.setProgress(learned);pb.getProgressDrawable().setTint(TEAL);margin(pb,dp(66),8,0,5);row.addView(pb,new LinearLayout.LayoutParams(-1,dp(6)));TextView small=tv("掌握 "+mast+"    熟悉 "+fam+"    模糊 "+fuz,11,MUTED,false);small.setPadding(dp(66),0,0,0);row.addView(small);row.setOnClickListener(v->showCategoryCards(name,cards));margin(row,0,0,0,9);return row;
    }
    void openCategory(String name,List<Card> cards){
        List<Card> learned=engine.reviewOnlyQueue(cards,Math.max(store.dailyGoal(),cards.size()));
        if(learned.isEmpty()){
            new AlertDialog.Builder(this).setTitle(name).setMessage("这个专题还没有已学习卡片。是否从本专题开始学习？").setNegativeButton("取消",null).setPositiveButton("开始学习",(d,w)->{List<Card> unseen=new ArrayList<>();for(Card c:cards)if(engine.unseen(c))unseen.add(c);Collections.shuffle(unseen);queue=new ArrayList<>(unseen.subList(0,Math.min(store.dailyGoal(),unseen.size())));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();}).show();
        }else{queue=learned;qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();}
    }

    LinkedHashMap<String,List<Card>> categories(){LinkedHashMap<String,List<Card>> m=new LinkedHashMap<>();String[] order={"社会工作原理","社会工作实务","督导与管理","个案工作","小组工作","社区工作"};for(String x:order)m.put(x,new ArrayList<>());for(Card c:repo.cards)m.get(categoryOf(c)).add(c);return m;}
    String categoryOf(Card c){String t=c.topic; if(t.contains("督导")||t.contains("项目")||t.contains("管理")||t.contains("预算")||t.contains("机构")||t.contains("需求评估")||t.contains("质量控制"))return "督导与管理";if(t.contains("小组"))return "小组工作";if(t.contains("社区"))return "社区工作";if(t.contains("危机")||t.contains("任务中心")||t.contains("行为治疗")||t.contains("叙事")||t.contains("个案")||t.contains("家庭治疗")||t.contains("理性情绪")||t.contains("社会心理")||t.contains("诊断学派"))return "个案工作";if(t.contains("企业")||t.contains("农村")||t.contains("退役")||t.contains("新就业")||t.contains("老年")||t.contains("儿童")||t.contains("妇女")||t.contains("残疾")||t.contains("医务")||t.contains("矫正")||t.contains("基层治理")||t.contains("信访"))return "社会工作实务";return "社会工作原理";}
    String categoryIcon(String n){if(n.equals("社会工作原理"))return "▣";if(n.equals("社会工作实务"))return "●";if(n.equals("督导与管理"))return "▤";if(n.equals("个案工作"))return "♥";if(n.equals("小组工作"))return "◎";return "⌂";}
    int categoryColor(String n){if(n.equals("社会工作原理"))return TEAL_DARK;if(n.equals("社会工作实务"))return Color.rgb(36,145,218);if(n.equals("督导与管理"))return Color.rgb(112,100,220);if(n.equals("个案工作"))return Color.rgb(226,139,58);if(n.equals("小组工作"))return Color.rgb(223,95,105);return TEAL;}
    int categorySoft(String n){if(n.equals("社会工作原理"))return TEAL_SOFT;if(n.equals("社会工作实务"))return BLUE_SOFT;if(n.equals("督导与管理"))return PURPLE_SOFT;if(n.equals("个案工作"))return Color.rgb(255,242,226);if(n.equals("小组工作"))return Color.rgb(255,236,239);return TEAL_SOFT;}

    void showStats(){
        shell("统计","学习趋势与薄弱点","统计");
        LinearLayout head=box(SURFACE,16,20);head.setBackground(strokeShape(SURFACE,20,LINE));LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.addView(bigMetric("今日学习量",store.dailyGoal()+"","张"),new LinearLayout.LayoutParams(0,-2,1));r.addView(bigMetric("今日完成",store.todayDone()+"","张"),new LinearLayout.LayoutParams(0,-2,1));r.addView(bigMetric("连续学习",store.streakDays()+"","天"),new LinearLayout.LayoutParams(0,-2,1));Button c=btn("▶  继续学习",TEAL,Color.WHITE);c.setOnClickListener(v->continueSession());r.addView(c,new LinearLayout.LayoutParams(dp(150),dp(56)));head.addView(r);body.addView(head);
        TextView trend=tv("学习趋势 · 近7天",19,INK,true);margin(trend,2,18,0,7);body.addView(trend);LinearLayout chart=box(SURFACE,16,18);chart.setBackground(strokeShape(SURFACE,18,LINE));chart.addView(makeWeekChart());body.addView(chart);
        int mast=masteryCount("掌握"),fam=masteryCount("熟悉"),fuz=masteryCount("模糊"),un=repo.cards.size()-mast-fam-fuz;LinearLayout dist=box(SURFACE,16,18);margin(dist,0,12,0,0);dist.setBackground(strokeShape(SURFACE,18,LINE));dist.addView(tv("知识点掌握分布",18,INK,true));dist.addView(tv("掌握 "+mast+"    熟悉 "+fam+"    模糊 "+fuz+"    未学习 "+un,13,MUTED,false));body.addView(dist);
        TextView weakTitle=tv("薄弱卡片 TOP 10",19,INK,true);margin(weakTitle,2,18,0,8);body.addView(weakTitle);List<Card> weak=new ArrayList<>();for(Card x:repo.cards)if(engine.learned(x))weak.add(x);weak.sort((a,b)->Double.compare(engine.difficulty(b),engine.difficulty(a)));for(int i=0;i<Math.min(10,weak.size());i++){Card x=weak.get(i);LinearLayout wr=box(SURFACE,12,14);wr.setBackground(strokeShape(SURFACE,14,LINE));LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView rank=tv(String.valueOf(i+1),12,i<3?Color.WHITE:MUTED,true);rank.setGravity(Gravity.CENTER);rank.setBackground(shape(i==0?RED:i==1?ORANGE:i==2?Color.rgb(235,174,51):Color.rgb(238,242,240),14));line.addView(rank,new LinearLayout.LayoutParams(dp(30),dp(30)));TextView qq=tv(x.question,14,INK,true);qq.setPadding(dp(10),0,dp(10),0);line.addView(qq,new LinearLayout.LayoutParams(0,-2,1));TextView pct=tv(Math.round(engine.difficulty(x)*100)+"%",12,i<3?ORANGE:MUTED,true);line.addView(pct);wr.addView(line);wr.setOnClickListener(v->{queue=engine.reviewOnlyQueue(Collections.singletonList(x),Math.min(3,store.dailyGoal()));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});margin(wr,0,0,0,6);body.addView(wr);}TextView note=tv("🎓  智能复习：仅从已学习内容中抽取；多次不清楚的卡片会更频繁出现。",12,TEAL_DARK,false);note.setBackground(shape(TEAL_SOFT,14));note.setPadding(dp(12),dp(10),dp(12),dp(10));margin(note,0,12,0,0);body.addView(note);
    }
    View bigMetric(String label,String num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);TextView b=tv(num,25,INK,true);b.setGravity(Gravity.CENTER);l.addView(b);TextView c=tv(unit,10,MUTED,false);c.setGravity(Gravity.CENTER);l.addView(c);return l;}
    View makeWeekChart(){LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setGravity(Gravity.BOTTOM);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,-6);int max=1;int[] vals=new int[7];String[] labs=new String[7];SimpleDateFormat f=new SimpleDateFormat("M/d",Locale.getDefault());for(int i=0;i<7;i++){Date d=cal.getTime();vals[i]=store.dayDone(d);labs[i]=f.format(d);max=Math.max(max,vals[i]);cal.add(Calendar.DAY_OF_YEAR,1);}for(int i=0;i<7;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView val=tv(String.valueOf(vals[i]),10,MUTED,true);val.setGravity(Gravity.CENTER);col.addView(val);View bar=new View(this);bar.setBackground(shape(i==6?TEAL:Color.rgb(151,222,205),8));int h=dp(18+(int)(90.0*vals[i]/max));col.addView(bar,new LinearLayout.LayoutParams(dp(28),h));TextView lab=tv(labs[i],9,MUTED,false);lab.setGravity(Gravity.CENTER);col.addView(lab);outer.addView(col,new LinearLayout.LayoutParams(0,dp(145),1));}return outer;}

    void showSettings(){
        shell("我的","学习设置与数据","我的");LinearLayout s=box(SURFACE,18,20);s.setBackground(strokeShape(SURFACE,20,LINE));s.addView(tv("每日学习设置",19,INK,true));TextView desc=tv("统一设置每天希望完成的卡片数量。复习与新学习不再分别计数。",13,MUTED,false);margin(desc,0,6,0,10);s.addView(desc);EditText goal=new EditText(this);goal.setInputType(2);goal.setText(String.valueOf(store.dailyGoal()));goal.setHint("例如 30");goal.setBackground(strokeShape(Color.rgb(249,251,250),13,LINE));goal.setPadding(dp(12),0,dp(12),0);s.addView(goal,new LinearLayout.LayoutParams(-1,dp(50)));Button save=btn("保存每日学习量",TEAL,Color.WHITE);margin(save,0,10,0,0);save.setOnClickListener(v->{int g=parse(goal,30);store.setDailyGoal(g);Toast.makeText(this,"已保存为每天 "+store.dailyGoal()+" 张",Toast.LENGTH_SHORT).show();showSettings();});s.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(s);
        LinearLayout info=box(SURFACE,18,20);info.setBackground(strokeShape(SURFACE,20,LINE));margin(info,0,12,0,0);info.addView(tv("智能复习规则",18,INK,true));info.addView(tv("• 复习卡只能来自已学习内容\n• 已学习卡可重复复习\n• 每张卡保留最近10次熟悉/不清楚记录\n• 最近多次不清楚的卡片优先级更高\n• 薄弱卡在同一学习队列内可重复出现2—3次\n• 所有学习数据只保存在本机",14,MUTED,false));body.addView(info);
        LinearLayout fb=box(SURFACE,18,20);fb.setBackground(strokeShape(SURFACE,20,LINE));margin(fb,0,12,0,0);fb.addView(tv("内容反馈数据",18,INK,true));fb.addView(tv("已记录 "+feedback.pendingCount()+" 条待校对内容。应用中的本地修正和错误标记不会自动发送给 ChatGPT；请导出 JSON 后在聊天中上传，我就能逐卡核对并回写题库。",13,MUTED,false));Button export=btn("导出内容反馈 JSON",TEAL,Color.WHITE);margin(export,0,10,0,0);export.setOnClickListener(v->exportFeedback());fb.addView(export,new LinearLayout.LayoutParams(-1,dp(52)));body.addView(fb);
        Button reset=btn("清空学习进度",RED_SOFT,Color.rgb(150,70,70));margin(reset,0,16,0,0);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("将删除所有卡片学习记录、最近10次记录和每日设置。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{store.clearAll();queue.clear();showSettings();}).show());body.addView(reset,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void showExample(Card c){JSONObject d=repo.detail(c.topic);String x=d==null?"":d.optString("example");new AlertDialog.Builder(this).setTitle(c.topic+" · 例子").setMessage(x.isEmpty()?"当前资料没有单列例子。":x).setPositiveButton("关闭",null).show();}
    void showFull(Card c){JSONObject d=repo.detail(c.topic);if(d==null)return;String[] labels={"名词解释","简答","论述/案例"},keys={"name","simple","essay"};new AlertDialog.Builder(this).setTitle(c.topic+" · 完整答案").setItems(labels,(dlg,which)->new AlertDialog.Builder(this).setTitle(labels[which]).setMessage(d.optString(keys[which],"暂无")).setPositiveButton("关闭",null).show()).setNegativeButton("关闭",null).show();}
    void showHistory(Card c){StudyStore.State s=store.state(c.id);StringBuilder b=new StringBuilder();int i=1;SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault());for(StudyStore.Rec r:s.history)b.append(i++).append(". ").append(r.ok?"熟悉":"不清楚").append("  ").append(f.format(new Date(r.ts))).append("\n");if(s.history.isEmpty())b.append("暂无记录");b.append("\n薄弱度：").append(Math.round(engine.difficulty(c)*100)).append("%");new AlertDialog.Builder(this).setTitle("最近10次 · "+c.topic).setMessage(b.toString()).setPositiveButton("关闭",null).show();}

    void showCategoryCards(String name,List<Card> cards){
        shell(name,"查看、学习、校对本专题卡片","卡片库");
        TextView note=tv("点击“学习/复习”进入卡片；点击“编辑”可在本机覆盖内容；发现错误也可以只标记，不必当场修改。",12,MUTED,false);note.setBackground(shape(TEAL_SOFT,14));note.setPadding(dp(12),dp(10),dp(12),dp(10));margin(note,0,0,0,10);body.addView(note);
        for(Card c:cards){
            LinearLayout item=box(SURFACE,14,16);item.setBackground(strokeShape(SURFACE,16,LINE));
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.addView(tv(c.question,15,INK,true));txt.addView(tv(c.kind+" · "+(engine.learned(c)?"已学习":"未学习")+(feedback.hasCardOverride(c.id)?" · 已本地修正":""),11,MUTED,false));line.addView(txt,new LinearLayout.LayoutParams(0,-2,1));TextView badge=tv(feedback.hasCardOverride(c.id)?"已修正":"",10,TEAL_DARK,true);line.addView(badge);item.addView(line);
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button learn=btn(engine.learned(c)?"学习/复习":"开始学习",TEAL_SOFT,TEAL_DARK),edit=btn("编辑",Color.rgb(247,249,248),MUTED),report=btn("标记有误",RED_SOFT,Color.rgb(164,70,70));learn.setOnClickListener(v->{queue=engine.learned(c)?engine.reviewOnlyQueue(Collections.singletonList(c),Math.min(3,store.dailyGoal())):new ArrayList<>(Collections.singletonList(c));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});edit.setOnClickListener(v->editCard(c));report.setOnClickListener(v->reportCard(c));actions.addView(learn,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(gap(6));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(gap(6));actions.addView(report,new LinearLayout.LayoutParams(0,dp(42),1));margin(actions,0,9,0,0);item.addView(actions);margin(item,0,0,0,8);body.addView(item);
        }
    }

    void reportCard(Card c){
        String[] types={"答案内容有误","题目表述有误","出处/章节有误","例子或扩展答案有误","知识点重复或冲突","其他"};
        new AlertDialog.Builder(this).setTitle("标记内容有误").setItems(types,(d,which)->{
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

    void exportFeedback(){
        pendingExport=feedback.exportBundle();Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"社会工作闪卡_内容反馈_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.getDefault()).format(new Date())+".json");startActivityForResult(i,EXPORT_FEEDBACK_REQ);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);if(requestCode==EXPORT_FEEDBACK_REQ&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&pendingExport!=null){try{java.io.OutputStream out=getContentResolver().openOutputStream(data.getData());out.write(pendingExport.getBytes("UTF-8"));out.close();pendingExport=null;Toast.makeText(this,"反馈 JSON 已导出。把该文件上传到聊天即可继续校对。",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"导出失败："+e.getMessage(),Toast.LENGTH_LONG).show();}}
    }

    int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return def;}}
}
