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
    final int BG=Color.rgb(247,248,244), SURFACE=Color.WHITE, INK=Color.rgb(28,55,50), MUTED=Color.rgb(111,128,123);
    final int TEAL=Color.rgb(31,158,137), TEAL_DARK=Color.rgb(19,118,104), TEAL_SOFT=Color.rgb(232,246,241);
    final int LINE=Color.rgb(226,234,230), ORANGE=Color.rgb(239,126,101), ORANGE_SOFT=Color.rgb(255,241,236), RED=Color.rgb(220,84,95), RED_SOFT=Color.rgb(255,237,240), BLUE_SOFT=Color.rgb(237,245,250), PURPLE_SOFT=Color.rgb(243,240,249), GOLD_SOFT=Color.rgb(250,245,231);
    CardRepository repo; StudyStore store; ReviewEngine engine; ContentFeedbackStore feedback; DataBackupManager backupManager;
    LinearLayout root,body,nav; List<Card> queue=new ArrayList<>(); int qIndex=0; boolean revealed=false; int answerTab=0;
    String pendingExport=null; static final int EXPORT_FEEDBACK_REQ=91, EXPORT_BACKUP_REQ=92, IMPORT_BACKUP_REQ=93;
    String currentScreen="home"; boolean categoryOpenedFromStudy=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        try{feedback=new ContentFeedbackStore(this);repo=new CardRepository(this,feedback);store=new StudyStore(this);engine=new ReviewEngine(store);backupManager=new DataBackupManager(this,store,feedback);showHome();}
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
    ImageView iconView(int resId,int size,int color){ImageView v=new ImageView(this);v.setImageResource(resId);v.setColorFilter(color);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setPadding(dp(2),dp(2),dp(2),dp(2));v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return v;}

    // targetSdk 35 在 Android 15+ 默认 edge-to-edge。用系统栏/刘海安全区保护标题、按钮与底部导航。
    void applySafeInsets(View v){
        v.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;
            }else{
                left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left,top,right,bottom);
            return insets;
        });
        v.post(v::requestApplyInsets);
    }

    void shell(String title,String subtitle,String active){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);setContentView(root);applySafeInsets(root);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(18),dp(10),dp(18),dp(10));
        LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(TEAL_SOFT,18));mark.addView(iconView(R.drawable.ic_brand_nodes,30,TEAL_DARK),new LinearLayout.LayoutParams(dp(30),dp(30)));header.addView(mark,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(dp(12),0,0,0);titles.addView(tv(title,28,INK,true));if(subtitle!=null&&!subtitle.isEmpty()){TextView sub=tv(subtitle,13,MUTED,false);margin(sub,0,3,0,0);titles.addView(sub);}header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);

        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setClipToPadding(false);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(2),dp(16),dp(96));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(10),dp(7),dp(10),dp(8));nav.setBackgroundColor(Color.WHITE);
        addNav(R.drawable.ic_nav_home,"学习","学习".equals(active),v->showHome());addNav(R.drawable.ic_nav_cards,"卡片库","卡片库".equals(active),v->showLibrary());addNav(R.drawable.ic_nav_stats,"统计","统计".equals(active),v->showStats());addNav(R.drawable.ic_nav_profile,"我的","我的".equals(active),v->showSettings());
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(72)));
    }
    void addNav(int resId,String label,boolean active,View.OnClickListener click){
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setOnClickListener(click);
        LinearLayout iconBox=new LinearLayout(this);iconBox.setGravity(Gravity.CENTER);if(active)iconBox.setBackground(shape(TEAL_SOFT,16));iconBox.addView(iconView(resId,22,active?TEAL_DARK:MUTED));item.addView(iconBox,new LinearLayout.LayoutParams(dp(48),dp(30)));
        TextView t=tv(label,11,active?TEAL_DARK:MUTED,active);t.setGravity(Gravity.CENTER);margin(t,0,3,0,0);item.addView(t);nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
    }

    // Study screen uses a fixed viewport: header + fixed card area + fixed rating controls + navigation.
    // Only the card's text area scrolls, preventing long answers from pushing controls off-screen.
    void studyShell(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);setContentView(root);applySafeInsets(root);
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(16),dp(10),dp(16),dp(8));
        TextView back=tv("‹",30,INK,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->onBackPressed());h.addView(back,new LinearLayout.LayoutParams(dp(42),dp(42)));
        TextView title=tv("学习中",20,INK,true);title.setGravity(Gravity.CENTER);h.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=tv("⋮",24,MUTED,true);menu.setGravity(Gravity.CENTER);h.addView(menu,new LinearLayout.LayoutParams(dp(42),dp(42)));root.addView(h);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),0,dp(16),dp(16));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
    }
    int learnedCount(){int n=0;for(Card c:repo.cards)if(engine.learned(c))n++;return n;}
    int masteryCount(String tag){int n=0;for(Card c:repo.cards)if(store.state(c.id).tag.equals(tag))n++;return n;}
    int remainingToday(){return Math.max(0,store.dailyGoal()-store.todayDone());}

    void showHome(){
        currentScreen="home";categoryOpenedFromStudy=false;
        shell("社会工作闪卡","把知识连起来，把理解留下来。","学习");

        LinearLayout task=box(SURFACE,18,24);task.setBackground(strokeShape(SURFACE,24,LINE));
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);titleRow.addView(tv("今日任务",20,INK,true),new LinearLayout.LayoutParams(0,-2,1));TextView day=tv("DAY  "+store.streakDays(),11,TEAL_DARK,true);day.setBackground(shape(TEAL_SOFT,12));day.setPadding(dp(9),dp(5),dp(9),dp(5));titleRow.addView(day);task.addView(titleRow);
        LinearLayout numberRow=new LinearLayout(this);numberRow.setGravity(Gravity.BOTTOM);TextView done=tv(String.valueOf(store.todayDone()),46,INK,true);numberRow.addView(done);TextView total=tv(" / "+store.dailyGoal(),18,MUTED,true);total.setPadding(0,0,0,dp(7));numberRow.addView(total);TextView remain=tv("还差 "+remainingToday()+" 张",12,MUTED,false);remain.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);numberRow.addView(remain,new LinearLayout.LayoutParams(0,-1,1));margin(numberRow,0,8,0,0);task.addView(numberRow);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,store.dailyGoal()));p.setProgress(Math.min(store.todayDone(),store.dailyGoal()));p.getProgressDrawable().setTint(TEAL);margin(p,0,10,0,12);task.addView(p,new LinearLayout.LayoutParams(-1,dp(7)));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stats.addView(miniStat("已学习",learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));stats.addView(miniStat("待学习",repo.cards.size()-learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));stats.addView(miniStat("收藏",favoriteCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));task.addView(stats);
        Button start=btn("开始今日学习  →",TEAL,Color.WHITE);start.setTextSize(17);margin(start,0,14,0,0);start.setOnClickListener(v->startNewSession());task.addView(start,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView cont=tv("继续上一次",13,TEAL_DARK,true);cont.setGravity(Gravity.CENTER);cont.setPadding(dp(8),dp(10),dp(8),dp(8));cont.setOnClickListener(v->continueSession());task.addView(cont);body.addView(task);

        LinearLayout guide=box(TEAL_SOFT,14,20);margin(guide,0,12,0,0);LinearLayout gr=new LinearLayout(this);gr.setGravity(Gravity.CENTER_VERTICAL);gr.addView(iconView(R.drawable.ic_brand_nodes,28,TEAL_DARK));TextView gt=tv("连接 · 生长",15,TEAL_DARK,true);gt.setPadding(dp(10),0,0,0);gr.addView(gt,new LinearLayout.LayoutParams(0,-2,1));TextView gd=tv("人 · 关系 · 资源",11,MUTED,false);gr.addView(gd);guide.addView(gr);body.addView(guide);

        LinearLayout section=new LinearLayout(this);section.setGravity(Gravity.CENTER_VERTICAL);TextView st=tv("学习专题",20,INK,true);section.addView(st,new LinearLayout.LayoutParams(0,-2,1));TextView more=tv("查看全部  ›",13,MUTED,false);more.setOnClickListener(v->showLibrary());section.addView(more);margin(section,2,18,2,8);body.addView(section);
        LinkedHashMap<String,List<Card>> cats=categories();int shown=0;for(Map.Entry<String,List<Card>> e:cats.entrySet()){if(shown++>=4)break;body.addView(topicRow(e.getKey(),e.getValue()));}
    }
    View miniStat(String label,int num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);TextView n=tv(num+"",23,INK,true);n.setGravity(Gravity.CENTER);margin(n,0,3,0,0);l.addView(n);TextView u=tv(unit,10,MUTED,false);u.setGravity(Gravity.CENTER);l.addView(u);return l;}

    void startNewSession(){
        int count=remainingToday();if(count<=0)count=store.dailyGoal();
        queue=engine.learningQueue(repo.cards,count);qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();
    }
    void continueSession(){
        if(!store.hasActiveSession()){Toast.makeText(this,"当前没有中断的学习进度，将开始新的学习。",Toast.LENGTH_SHORT).show();startNewSession();return;}
        queue=new ArrayList<>();for(String id:store.sessionIds()){Card c=repo.byId(id);if(c!=null)queue.add(c);}qIndex=Math.min(store.sessionIndex(),queue.size());revealed=false;showStudy();
    }

    void showStudy(){
        currentScreen="study";
        studyShell();
        if(!revealed)answerTab=0;
        if(queue.isEmpty()||qIndex>=queue.size()){
            store.clearSession();
            LinearLayout done=box(SURFACE,24,22);done.setBackground(strokeShape(SURFACE,22,LINE));
            LinearLayout.LayoutParams dpDone=new LinearLayout.LayoutParams(-1,0,1);dpDone.setMargins(0,0,0,dp(8));done.setLayoutParams(dpDone);
            done.setGravity(Gravity.CENTER);
            TextView title=tv("✓ 本轮学习完成",22,INK,true);title.setGravity(Gravity.CENTER);done.addView(title);
            TextView d=tv("今天已完成 "+store.todayDone()+" 张。薄弱卡会依据最近10次记录继续提高后续出现概率。",14,MUTED,false);d.setGravity(Gravity.CENTER);margin(d,0,8,0,16);done.addView(d);
            Button again=btn("继续加练",TEAL,Color.WHITE);again.setOnClickListener(v->startNewSession());done.addView(again,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(done);return;
        }
        Card c=queue.get(qIndex);
        LinearLayout progress=box(SURFACE,12,18);
        LinearLayout pr=new LinearLayout(this);pr.setGravity(Gravity.CENTER_VERTICAL);TextView pt=tv("今日学习量  "+store.todayDone()+" / "+store.dailyGoal(),15,INK,true);pr.addView(pt,new LinearLayout.LayoutParams(0,-2,1));TextView pos=tv((qIndex+1)+" / "+queue.size(),12,MUTED,false);pr.addView(pos);progress.addView(pr);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,queue.size()));pb.setProgress(Math.min(queue.size(),qIndex+1));pb.getProgressDrawable().setTint(TEAL);margin(pb,0,7,0,0);progress.addView(pb,new LinearLayout.LayoutParams(-1,dp(6)));margin(progress,0,0,0,9);body.addView(progress);

        LinearLayout card=box(SURFACE,17,24);card.setBackground(strokeShape(SURFACE,24,LINE));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1);cp.setMargins(0,0,0,revealed?dp(9):0);card.setLayoutParams(cp);
        LinearLayout metaRow=new LinearLayout(this);metaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView kind=tv(c.kind,12,MUTED,false);metaRow.addView(kind,new LinearLayout.LayoutParams(0,-2,1));
        if(store.isFavorite(c.id)){TextView star=tv("★",18,Color.rgb(222,164,46),true);star.setPadding(0,0,dp(7),0);metaRow.addView(star);}
        TextView status=tv(engine.learned(c)?"已学习":"新学习",11,engine.learned(c)?TEAL_DARK:ORANGE,true);status.setBackground(shape(engine.learned(c)?TEAL_SOFT:ORANGE_SOFT,14));status.setPadding(dp(10),dp(5),dp(10),dp(5));metaRow.addView(status);
        TextView more=tv("⋮",25,MUTED,true);more.setGravity(Gravity.CENTER);more.setPadding(dp(12),0,0,0);more.setOnClickListener(v->showCardMenu(c,more));metaRow.addView(more,new LinearLayout.LayoutParams(dp(42),dp(38)));card.addView(metaRow);
        TextView topic=tv(c.topic,13,Color.rgb(126,143,139),false);margin(topic,0,7,0,revealed?dp(8):dp(5));card.addView(topic);

        if(!revealed){
            ScrollView mid=new ScrollView(this);mid.setFillViewport(true);mid.setVerticalScrollBarEnabled(false);
            LinearLayout inner=new LinearLayout(this);inner.setOrientation(LinearLayout.VERTICAL);inner.setGravity(Gravity.CENTER);inner.setPadding(dp(7),dp(12),dp(7),dp(14));
            TextView q=tv(c.question,24,INK,true);q.setGravity(Gravity.CENTER);q.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);margin(q,dp(3),dp(18),dp(3),dp(18));inner.addView(q);
            TextView hint=tv("☝  点击显示答案",14,MUTED,true);hint.setGravity(Gravity.CENTER);hint.setBackground(shape(Color.rgb(246,249,248),14));hint.setPadding(dp(18),dp(12),dp(18),dp(12));margin(hint,dp(18),dp(12),dp(18),0);inner.addView(hint);
            TextView hint2=tv("先完整回忆，再揭晓答案",12,Color.rgb(151,163,160),false);hint2.setGravity(Gravity.CENTER);margin(hint2,0,dp(7),0,0);inner.addView(hint2);
            View.OnClickListener reveal=v->{revealed=true;answerTab=0;showStudy();};mid.setOnClickListener(reveal);inner.setOnClickListener(reveal);q.setOnClickListener(reveal);hint.setOnClickListener(reveal);hint2.setOnClickListener(reveal);
            mid.addView(inner,new ScrollView.LayoutParams(-1,-2));card.addView(mid,new LinearLayout.LayoutParams(-1,0,1));
            body.addView(card);
            return; // 问题页纯回忆：不显示评价按钮，也不显示例子/完整答案等提示性工具。
        }

        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(0,0,0,dp(8));
        tabs.addView(answerTab("核心答案",0,c),new LinearLayout.LayoutParams(0,dp(40),1));tabs.addView(gap(4));
        tabs.addView(answerTab("考频与真题",1,c),new LinearLayout.LayoutParams(0,dp(40),1));tabs.addView(gap(4));
        tabs.addView(answerTab("相关知识点",2,c),new LinearLayout.LayoutParams(0,dp(40),1));card.addView(tabs);

        ScrollView mid=new ScrollView(this);mid.setFillViewport(true);mid.setVerticalScrollBarEnabled(true);mid.setScrollbarFadingEnabled(true);
        LinearLayout inner=new LinearLayout(this);inner.setOrientation(LinearLayout.VERTICAL);inner.setPadding(dp(5),dp(8),dp(5),dp(12));
        if(answerTab==0)renderCoreAnswer(inner,c);else if(answerTab==1)renderExamInfo(inner,c);else renderRelatedKnowledge(inner,c);
        mid.addView(inner,new ScrollView.LayoutParams(-1,-2));card.addView(mid,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);
        Button ex=btn("例子",Color.rgb(247,249,248),MUTED),full=btn("完整答案",Color.rgb(247,249,248),MUTED),hist=btn("最近10次",Color.rgb(247,249,248),MUTED);
        ex.setEnabled(c.hasExample);full.setEnabled(c.hasLong);ex.setOnClickListener(v->showExample(c));full.setOnClickListener(v->showFull(c));hist.setOnClickListener(v->showHistory(c));
        tools.addView(ex,new LinearLayout.LayoutParams(0,dp(44),1));tools.addView(gap(7));tools.addView(full,new LinearLayout.LayoutParams(0,dp(44),1));tools.addView(gap(7));tools.addView(hist,new LinearLayout.LayoutParams(0,dp(44),1));card.addView(tools);body.addView(card);

        LinearLayout rates=new LinearLayout(this);rates.setOrientation(LinearLayout.HORIZONTAL);
        Button bad=btn("?  不清楚\n再多看几遍",ORANGE_SOFT,ORANGE);Button ok=btn("✓  熟悉\n我已经记住了",TEAL_SOFT,TEAL_DARK);
        bad.setOnClickListener(v->rate(c,false));ok.setOnClickListener(v->rate(c,true));
        rates.addView(bad,new LinearLayout.LayoutParams(0,dp(66),1));rates.addView(gap(10));rates.addView(ok,new LinearLayout.LayoutParams(0,dp(66),1));body.addView(rates);
    }

    TextView answerTab(String label,int tab,Card c){
        boolean active=answerTab==tab;TextView v=tv(label,12,active?TEAL_DARK:MUTED,active);v.setGravity(Gravity.CENTER);v.setBackground(active?strokeShape(TEAL_SOFT,13,Color.rgb(171,226,214)):shape(Color.rgb(247,249,248),13));v.setOnClickListener(x->{answerTab=tab;showStudy();});return v;
    }

    void renderCoreAnswer(LinearLayout inner,Card c){
        TextView questionLabel=tv("题目",11,MUTED,true);inner.addView(questionLabel);
        TextView q=tv(c.question,16,INK,true);q.setBackground(shape(Color.rgb(248,250,249),12));q.setPadding(dp(10),dp(9),dp(10),dp(9));margin(q,0,4,0,12);inner.addView(q);
        TextView answerTitle=tv("💡  核心答案",14,TEAL_DARK,true);margin(answerTitle,0,0,0,9);inner.addView(answerTitle);
        for(String x:c.bullets){TextView b=tv("•  "+x,15,INK,false);margin(b,0,0,0,9);inner.addView(b);}
        if(c.tip!=null&&!c.tip.trim().isEmpty()){TextView tip=tv("易记："+c.tip,13,TEAL_DARK,false);tip.setBackground(shape(TEAL_SOFT,12));tip.setPadding(dp(10),dp(8),dp(10),dp(8));margin(tip,0,4,0,10);inner.addView(tip);}
        TextView src=tv("出处："+c.origin,11,MUTED,false);margin(src,0,4,0,8);inner.addView(src);
        TextView scrollHint=tv("↕  上下滑动查看完整内容",11,Color.rgb(151,163,160),false);scrollHint.setGravity(Gravity.CENTER);inner.addView(scrollHint);
    }

    void renderExamInfo(LinearLayout inner,Card c){
        JSONObject m=repo.examMeta(c.topic);
        if(m==null){inner.addView(tv("暂无考频资料。",14,MUTED,false));return;}
        String level=m.optString("frequencyLevel","暂无直接命题");int years=m.optInt("explicitYearCount",0),qs=m.optInt("explicitQuestionCount",0),inferred=m.optInt("inferredQuestionCount",0);
        LinearLayout heat=box(level.equals("高频")?Color.rgb(235,247,255):level.equals("中频")?PURPLE_SOFT:level.equals("低频")?GOLD_SOFT:Color.rgb(247,249,248),12,15);
        LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);TextView ht=tv("▥  考试热度",14,INK,true);hr.addView(ht,new LinearLayout.LayoutParams(0,-2,1));TextView badge=tv(level,11,level.equals("高频")?Color.rgb(42,115,171):level.equals("中频")?Color.rgb(105,78,175):level.equals("低频")?Color.rgb(161,118,24):MUTED,true);badge.setBackground(shape(Color.WHITE,12));badge.setPadding(dp(9),dp(4),dp(9),dp(4));hr.addView(badge);heat.addView(hr);
        TextView sum=tv("2015—2026 · 明确年份直接命题 "+years+" 年 / "+qs+" 题",13,INK,true);margin(sum,0,7,0,0);heat.addView(sum);
        if(inferred>0){TextView inf=tv("另有 "+inferred+" 题来自年份按汇编顺序推定的回忆截图（不计入主考频）",11,ORANGE,false);margin(inf,0,4,0,0);heat.addView(inf);}inner.addView(heat);
        TextView title=tv("历年命题",14,INK,true);margin(title,0,14,0,7);inner.addView(title);
        JSONArray rs=m.optJSONArray("records");
        if(rs==null||rs.length()==0){
            TextView none=tv("在当前2015—2026真题汇编中，未发现与本专题直接对应的独立命题记录。\n这不等于‘不重要’：它仍可能作为其他真题的理论基础、答题工具或案例分析视角。",13,MUTED,false);none.setBackground(shape(Color.rgb(248,250,249),13));none.setPadding(dp(11),dp(10),dp(11),dp(10));inner.addView(none);
        }else{
            for(int i=0;i<rs.length();i++){
                JSONObject r=rs.optJSONObject(i);if(r==null)continue;
                LinearLayout item=box(Color.rgb(248,250,249),11,15);item.setBackground(strokeShape(Color.rgb(248,250,249),15,LINE));
                LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
                TextView y=tv(r.optString("yearLabel"),12,TEAL_DARK,true);top.addView(y,new LinearLayout.LayoutParams(0,-2,1));
                TextView ty=tv(r.optString("subject")+" · "+r.optString("type"),11,MUTED,true);top.addView(ty);item.addView(top);
                TextView qq=tv(r.optString("question"),13,INK,false);margin(qq,0,6,0,8);item.addView(qq);
                LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
                String sl=r.optString("sourceLabel");TextView src=tv(sl.isEmpty()?"真题记录":sl,10,r.optString("yearBasis").equals("inferred")?ORANGE:MUTED,false);actions.addView(src,new LinearLayout.LayoutParams(0,-2,1));
                JSONObject ans=repo.examAnswer(r.optString("answerKey"));
                if(ans!=null&&"verified".equals(ans.optString("status"))){
                    Button view=btn("查看已核验答案  ›",TEAL_SOFT,TEAL_DARK);view.setTextSize(11);view.setOnClickListener(v->showExamAnswer(r,ans));actions.addView(view,new LinearLayout.LayoutParams(dp(132),dp(38)));
                }else{
                    TextView pending=tv("答案待核验",10,Color.rgb(145,154,152),true);pending.setBackground(shape(Color.rgb(238,242,240),12));pending.setPadding(dp(9),dp(5),dp(9),dp(5));actions.addView(pending);
                }
                item.addView(actions);margin(item,0,0,0,8);inner.addView(item);
            }
        }
        TextView rule=tv("答案规则：只为已完成来源核验、且题干与专题答案直接对应的真题开放“查看答案”。其余题目宁可显示‘待核验’，也不使用泛化答案代替。",11,MUTED,false);rule.setBackground(shape(Color.rgb(246,249,248),12));rule.setPadding(dp(10),dp(9),dp(10),dp(9));margin(rule,0,7,0,0);inner.addView(rule);
    }

    void showExamAnswer(JSONObject record,JSONObject ans){
        ScrollView sv=new ScrollView(this);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(17),dp(10),dp(17),dp(12));
        TextView verified=tv("✓  已核验参考答案",12,TEAL_DARK,true);verified.setBackground(shape(TEAL_SOFT,14));verified.setPadding(dp(10),dp(6),dp(10),dp(6));wrap.addView(verified);
        TextView q=tv(record.optString("question"),16,INK,true);q.setBackground(shape(Color.rgb(247,249,248),13));q.setPadding(dp(12),dp(10),dp(12),dp(10));margin(q,0,10,0,12);wrap.addView(q);
        TextView at=tv("参考答案",14,TEAL_DARK,true);wrap.addView(at);
        TextView body=tv(ans.optString("answer"),14,INK,false);body.setLineSpacing(dp(2),1.17f);margin(body,0,7,0,12);wrap.addView(body);
        String source=ans.optString("source");if(!source.isEmpty()){TextView src=tv("核验依据："+source,11,MUTED,false);src.setBackground(shape(Color.rgb(248,250,249),12));src.setPadding(dp(10),dp(9),dp(10),dp(9));wrap.addView(src);}
        TextView note=tv("说明：这是依据教材、真题和已核验题库整理的参考答案，不宣称为东南大学官方标准答案。若来源或题干尚未达到核验门槛，App不会显示答案按钮。",11,ORANGE,false);note.setBackground(shape(ORANGE_SOFT,12));note.setPadding(dp(10),dp(9),dp(10),dp(9));margin(note,0,9,0,0);wrap.addView(note);
        sv.addView(wrap);String title=record.optString("yearLabel")+" · "+record.optString("subject")+" · "+record.optString("type");
        new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("关闭",null).show();
    }

    void renderRelatedKnowledge(LinearLayout inner,Card c){
        JSONObject m=repo.examMeta(c.topic);if(m==null){inner.addView(tv("暂无关联知识点。",14,MUTED,false));return;}
        TextView h1=tv("⌘  本卡关键关联",14,INK,true);inner.addView(h1);JSONArray core=m.optJSONArray("relatedCore");renderRelatedRows(inner,core,true);
        JSONArray ext=m.optJSONArray("relatedExtended");if(ext!=null&&ext.length()>0){TextView h2=tv("↗  延伸知识点",14,INK,true);margin(h2,0,13,0,0);inner.addView(h2);renderRelatedRows(inner,ext,false);}
        TextView note=tv("关联知识点按当前题库的理论—方法—实务结构组织，用于串联理解；它们不会被计入本卡的真题考频。点击条目先就地预览，需要时再进入专题。",11,MUTED,false);note.setBackground(shape(Color.rgb(246,249,248),12));note.setPadding(dp(10),dp(9),dp(10),dp(9));margin(note,0,9,0,0);inner.addView(note);
    }

    void renderRelatedRows(LinearLayout inner,JSONArray arr,boolean core){
        if(arr==null)return;int[] cs={Color.rgb(239,110,93),Color.rgb(232,164,50),Color.rgb(117,96,201),Color.rgb(71,137,211),Color.rgb(57,166,126)};
        for(int i=0;i<arr.length();i++){
            String topic=arr.optString(i);if(topic.isEmpty())continue;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(9),dp(8),dp(8),dp(8));row.setBackground(shape(Color.rgb(248,250,249),11));
            TextView icon=tv(String.valueOf(i+1),10,Color.WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(cs[i%cs.length],9));row.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
            TextView label=tv(topic,13,INK,core);label.setPadding(dp(9),0,dp(6),0);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=tv("›",21,MUTED,false);row.addView(arrow);
            row.setOnClickListener(v->showRelatedTopicPreview(topic));margin(row,0,5,0,0);inner.addView(row);
        }
    }

    // 相关知识点先在学习页上方就地预览，避免为了查看关联内容被强制带离当前记忆卡。
    void showRelatedTopicPreview(String topic){
        List<Card> list=new ArrayList<>();for(Card x:repo.cards)if(x.topic.equals(topic))list.add(x);
        ScrollView sv=new ScrollView(this);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(17),dp(8),dp(17),dp(12));
        TextView chip=tv("关联知识点",11,TEAL_DARK,true);chip.setBackground(shape(TEAL_SOFT,13));chip.setPadding(dp(9),dp(5),dp(9),dp(5));wrap.addView(chip);
        TextView title=tv(topic,19,INK,true);margin(title,0,8,0,8);wrap.addView(title);
        JSONObject d=repo.detail(topic);String intro=d==null?"":d.optString("name");if(!intro.isEmpty()){TextView desc=tv(intro,13,INK,false);desc.setBackground(shape(Color.rgb(248,250,249),12));desc.setPadding(dp(11),dp(9),dp(11),dp(9));wrap.addView(desc);}
        TextView count=tv("本专题 · "+list.size()+" 张卡片",12,MUTED,true);margin(count,0,12,0,5);wrap.addView(count);
        for(int i=0;i<Math.min(4,list.size());i++){Card x=list.get(i);TextView q=tv("•  "+x.question,13,INK,false);q.setPadding(dp(3),dp(5),dp(3),dp(5));wrap.addView(q);}
        if(list.size()>4){TextView more=tv("另有 "+(list.size()-4)+" 张卡片",11,MUTED,false);margin(more,0,5,0,0);wrap.addView(more);}
        TextView hint=tv("手机返回键会先关闭此预览，仍停留在当前学习卡。只有主动选择“进入专题”才离开学习页。",11,MUTED,false);hint.setBackground(shape(Color.rgb(246,249,248),12));hint.setPadding(dp(10),dp(9),dp(10),dp(9));margin(hint,0,12,0,0);wrap.addView(hint);
        sv.addView(wrap);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("相关知识点").setView(sv).setNegativeButton("关闭",null).setPositiveButton("进入专题",null).create();
        dialog.setOnShowListener(x->{Button go=dialog.getButton(AlertDialog.BUTTON_POSITIVE);go.setTextColor(TEAL_DARK);go.setOnClickListener(v->{dialog.dismiss();if(list.isEmpty()){Toast.makeText(this,"暂无该专题卡片",Toast.LENGTH_SHORT).show();return;}categoryOpenedFromStudy=true;showCategoryCards(topic,list);});});dialog.show();
    }

    void showCardMenu(Card c,View anchor){
        PopupMenu popup=new PopupMenu(this,anchor);Menu menu=popup.getMenu();
        menu.add(0,1,0,store.isFavorite(c.id)?"★ 取消收藏":"☆ 添加收藏");
        menu.add(0,2,1,"✎ 本地修正");
        menu.add(0,3,2,"⚑ 内容反馈");
        boolean hasOverride=feedback.hasCardOverride(c.id)||feedback.hasDetailOverride(c.topic);
        if(hasOverride)menu.add(0,4,3,"↺ 恢复官方内容");
        popup.setOnMenuItemClickListener(item->{
            if(item.getItemId()==1){boolean now=!store.isFavorite(c.id);store.setFavorite(c.id,now);Toast.makeText(this,now?"已加入收藏":"已取消收藏",Toast.LENGTH_SHORT).show();showStudy();return true;}
            if(item.getItemId()==2){editCard(c);return true;}
            if(item.getItemId()==3){reportCard(c);return true;}
            if(item.getItemId()==4){restoreOfficialContent(c);return true;}
            return false;
        });popup.show();
    }

    void restoreOfficialContent(Card c){
        new AlertDialog.Builder(this).setTitle("恢复官方内容？").setMessage("将删除这张卡的本地修正；如果本专题修改过例子或完整答案，也会恢复为随安装包提供的官方版本。学习记录、收藏和最近10次记录不会受影响。")
            .setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{
                List<String> ids=new ArrayList<>();for(Card x:queue)ids.add(x.id);
                feedback.clearCardOverride(c.id);feedback.clearDetailOverride(c.topic);
                try{
                    repo=new CardRepository(this,feedback);
                    List<Card> rebuilt=new ArrayList<>();
                    for(String id:ids){Card x=repo.byId(id);if(x!=null)rebuilt.add(x);}
                    queue=rebuilt;
                    if(qIndex>=queue.size())qIndex=Math.max(0,queue.size()-1);
                    revealed=false;answerTab=0;
                    store.saveSession(queue,qIndex);
                    Toast.makeText(this,"已恢复官方内容",Toast.LENGTH_SHORT).show();
                    showStudy();
                }catch(Exception e){
                    Toast.makeText(this,"恢复官方内容失败："+e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }).show();
    }
    View historyMini(Card c){StudyStore.State s=store.state(c.id);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);TextView label=tv("最近10次",11,MUTED,false);margin(label,0,10,0,4);wrap.addView(label);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);for(StudyStore.Rec r:s.history){TextView x=tv(r.ok?"熟":"模",10,r.ok?TEAL_DARK:ORANGE,true);x.setGravity(Gravity.CENTER);x.setBackground(shape(r.ok?TEAL_SOFT:ORANGE_SOFT,11));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(31),dp(25));p.setMargins(dp(2),0,dp(2),0);row.addView(x,p);}wrap.addView(row);return wrap;}
    void rate(Card c,boolean ok){engine.rate(c,ok);qIndex++;revealed=false;answerTab=0;store.saveSession(queue,qIndex);showStudy();}

    static class SearchHit {
        Card card; int score; String reason;
        SearchHit(Card card,int score,String reason){this.card=card;this.score=score;this.reason=reason;}
    }

    void showLibrary(){
        currentScreen="library";categoryOpenedFromStudy=false;
        shell("卡片库","强大的搜索与分类，快速定位内容。","卡片库");

        final String[] mode={"全部"};final Button[] filterButtons=new Button[4];String[] labels={"全部","已学习","未学习","★ 收藏"};

        LinearLayout searchBox=box(SURFACE,12,14);searchBox.setBackground(strokeShape(SURFACE,16,LINE));
        LinearLayout searchRow=new LinearLayout(this);searchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView searchIcon=tv("⌕",22,TEAL_DARK,true);searchIcon.setGravity(Gravity.CENTER);searchRow.addView(searchIcon,new LinearLayout.LayoutParams(dp(34),dp(46)));
        EditText search=new EditText(this);search.setHint("搜索卡片、知识点或题目");search.setSingleLine(true);search.setTextSize(14);search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(dp(4),0,dp(8),0);searchRow.addView(search,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView clear=tv("清除",12,MUTED,true);clear.setGravity(Gravity.CENTER);clear.setPadding(dp(10),0,dp(4),0);clear.setVisibility(View.GONE);clear.setOnClickListener(v->search.setText(""));searchRow.addView(clear,new LinearLayout.LayoutParams(dp(52),dp(46)));
        searchBox.addView(searchRow);body.addView(searchBox);

        TextView searchTip=tv("支持模糊搜索、多关键词（空格分隔）、同义词联想和相关度排序",11,MUTED,false);
        margin(searchTip,4,5,0,8);body.addView(searchTip);

        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<labels.length;i++){final int idx=i;Button b=btn(labels[i],i==0?TEAL_SOFT:Color.rgb(247,249,248),i==0?TEAL_DARK:MUTED);b.setTextSize(13);filterButtons[i]=b;filters.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));if(i<labels.length-1)filters.addView(gap(5));}
        margin(filters,0,0,0,8);body.addView(filters);

        TextView rule=tv("🎓  搜索可覆盖题目、知识点、答案要点与完整答案；筛选条件可与搜索同时使用。",12,TEAL_DARK,false);rule.setBackground(shape(TEAL_SOFT,14));rule.setPadding(dp(12),dp(9),dp(12),dp(9));margin(rule,0,0,0,8);body.addView(rule);

        LinearLayout quality=box(SURFACE,14,16);quality.setBackground(strokeShape(SURFACE,16,LINE));LinearLayout qr=new LinearLayout(this);qr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout qtxt=new LinearLayout(this);qtxt.setOrientation(LinearLayout.VERTICAL);qtxt.addView(tv("内容校对与反馈",16,INK,true));qtxt.addView(tv("待处理标记 "+feedback.pendingCount()+" 条 · 可本地修正，也可导出给我统一校对",11,MUTED,false));qr.addView(qtxt,new LinearLayout.LayoutParams(0,-2,1));Button exp=btn("导出反馈",TEAL_SOFT,TEAL_DARK);exp.setOnClickListener(v->exportFeedback());qr.addView(exp,new LinearLayout.LayoutParams(dp(105),dp(44)));quality.addView(qr);margin(quality,0,0,0,9);body.addView(quality);

        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);
        final Runnable[] fill=new Runnable[1];fill[0]=()->{
            list.removeAllViews();String key=search.getText().toString().trim();clear.setVisibility(key.isEmpty()?View.GONE:View.VISIBLE);int total=0;

            if(!key.isEmpty()){
                List<SearchHit> matches=new ArrayList<>();
                for(Card c:repo.cards){
                    boolean pass="全部".equals(mode[0])||("已学习".equals(mode[0])&&engine.learned(c))||("未学习".equals(mode[0])&&!engine.learned(c))||("收藏".equals(mode[0])&&store.isFavorite(c.id));
                    if(!pass)continue;
                    SearchHit hit=searchHit(c,key);
                    if(hit!=null){matches.add(hit);total++;}
                }
                Collections.sort(matches,(a,b)->{
                    int d=Integer.compare(b.score,a.score);
                    if(d!=0)return d;
                    return a.card.question.compareTo(b.card.question);
                });
                TextView resultTitle=tv(total==0?"没有找到相关卡片":"找到 "+total+" 张相关卡片 · 已按相关度排序",13,total==0?MUTED:TEAL_DARK,true);margin(resultTitle,2,4,0,8);list.addView(resultTitle);
                for(SearchHit h:matches)list.addView(searchCardRow(h,key));
                return;
            }

            for(Map.Entry<String,List<Card>> e:categories().entrySet()){
                List<Card> filtered=new ArrayList<>();
                for(Card c:e.getValue()){
                    boolean pass="全部".equals(mode[0])||("已学习".equals(mode[0])&&engine.learned(c))||("未学习".equals(mode[0])&&!engine.learned(c))||("收藏".equals(mode[0])&&store.isFavorite(c.id));
                    if(pass){filtered.add(c);total++;}
                }
                if(filtered.isEmpty())continue;list.addView(topicRow(e.getKey(),filtered));
            }
            if(total==0){TextView empty=tv("没有符合条件的卡片",14,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(28),0,dp(28));list.addView(empty);}
        };
        for(int i=0;i<filterButtons.length;i++){final int idx=i;filterButtons[i].setOnClickListener(v->{mode[0]=idx==0?"全部":idx==1?"已学习":idx==2?"未学习":"收藏";for(int j=0;j<filterButtons.length;j++){filterButtons[j].setBackground(shape(j==idx?TEAL_SOFT:Color.rgb(247,249,248),15));filterButtons[j].setTextColor(j==idx?TEAL_DARK:MUTED);}fill[0].run();});}
        fill[0].run();search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){fill[0].run();}public void afterTextChanged(android.text.Editable e){}});
    }

    List<String> queryTokens(String raw){
        String x=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);
        x=x.replaceAll("[，,。；;、/|]+"," ");
        String[] parts=x.split("\\s+");
        List<String> out=new ArrayList<>();
        for(String p:parts)if(!p.isEmpty()&&!out.contains(p))out.add(p);
        return out;
    }

    List<String> tokenVariants(String token){
        LinkedHashSet<String> s=new LinkedHashSet<>();s.add(token);
        String[][] pairs={
            {"赋能","增权"},{"危机干预","危机介入"},{"临终关怀","安宁疗护"},
            {"rebt","理性情绪治疗"},{"理性情绪疗法","理性情绪治疗"},
            {"社会支持网络","社会支持"},{"韧性","抗逆力"}
        };
        for(String[] p:pairs){
            if(token.contains(p[0])||p[0].contains(token)){s.add(p[1]);}
            if(token.contains(p[1])||p[1].contains(token)){s.add(p[0]);}
        }
        return new ArrayList<>(s);
    }

    int containsAny(String text,List<String> variants){
        if(text==null)return 0;String t=text.toLowerCase(Locale.ROOT);int longest=0;
        for(String v:variants)if(!v.isEmpty()&&t.contains(v))longest=Math.max(longest,v.length());
        return longest;
    }

    SearchHit searchHit(Card c,String raw){
        List<String> tokens=queryTokens(raw);if(tokens.isEmpty())return null;
        JSONObject d=repo.detail(c.topic);
        int total=0;String bestReason="";int bestField=0;

        for(String token:tokens){
            List<String> vars=tokenVariants(token);
            int tokenScore=0;String reason="";

            int m=containsAny(c.question,vars);if(m>0){tokenScore=Math.max(tokenScore,120+m);reason="题目";}
            m=containsAny(c.topic,vars);if(m>0&&90+m>tokenScore){tokenScore=90+m;reason="知识点";}
            m=containsAny(c.kind,vars);if(m>0&&45+m>tokenScore){tokenScore=45+m;reason="卡片类型";}
            for(String b:c.bullets){m=containsAny(b,vars);if(m>0&&70+m>tokenScore){tokenScore=70+m;reason="答案要点";}}
            m=containsAny(c.tip,vars);if(m>0&&38+m>tokenScore){tokenScore=38+m;reason="记忆提示";}
            m=containsAny(c.origin,vars);if(m>0&&24+m>tokenScore){tokenScore=24+m;reason="出处";}
            if(d!=null){
                String[] fields={"example","name","simple","essay"};
                for(String f:fields){m=containsAny(d.optString(f,""),vars);if(m>0&&52+m>tokenScore){tokenScore=52+m;reason="扩展答案";}}
            }

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
        currentScreen="stats";categoryOpenedFromStudy=false;
        shell("学习统计","学习数据一目了然，见证成长。","统计");
        TextView trend=tv("学习趋势 · 近7天",19,INK,true);margin(trend,2,18,0,7);body.addView(trend);LinearLayout chart=box(SURFACE,16,18);chart.setBackground(strokeShape(SURFACE,18,LINE));chart.addView(makeWeekChart());body.addView(chart);
        int mast=masteryCount("掌握"),fam=masteryCount("熟悉"),fuz=masteryCount("模糊"),un=repo.cards.size()-mast-fam-fuz;LinearLayout dist=box(SURFACE,16,18);margin(dist,0,12,0,0);dist.setBackground(strokeShape(SURFACE,18,LINE));dist.addView(tv("知识点掌握分布",18,INK,true));dist.addView(tv("掌握 "+mast+"    熟悉 "+fam+"    模糊 "+fuz+"    未学习 "+un,13,MUTED,false));body.addView(dist);
        TextView weakTitle=tv("薄弱卡片 TOP 10",19,INK,true);margin(weakTitle,2,18,0,8);body.addView(weakTitle);List<Card> weak=new ArrayList<>();for(Card x:repo.cards)if(engine.learned(x))weak.add(x);weak.sort((a,b)->Double.compare(engine.difficulty(b),engine.difficulty(a)));for(int i=0;i<Math.min(10,weak.size());i++){Card x=weak.get(i);LinearLayout wr=box(SURFACE,12,14);wr.setBackground(strokeShape(SURFACE,14,LINE));LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView rank=tv(String.valueOf(i+1),12,i<3?Color.WHITE:MUTED,true);rank.setGravity(Gravity.CENTER);rank.setBackground(shape(i==0?RED:i==1?ORANGE:i==2?Color.rgb(235,174,51):Color.rgb(238,242,240),14));line.addView(rank,new LinearLayout.LayoutParams(dp(30),dp(30)));TextView qq=tv(x.question,14,INK,true);qq.setPadding(dp(10),0,dp(10),0);line.addView(qq,new LinearLayout.LayoutParams(0,-2,1));TextView pct=tv(Math.round(engine.difficulty(x)*100)+"%",12,i<3?ORANGE:MUTED,true);line.addView(pct);wr.addView(line);wr.setOnClickListener(v->{queue=engine.reviewOnlyQueue(Collections.singletonList(x),Math.min(3,store.dailyGoal()));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});margin(wr,0,0,0,6);body.addView(wr);}TextView note=tv("🎓  智能复习：仅从已学习内容中抽取；多次不清楚的卡片会更频繁出现。",12,TEAL_DARK,false);note.setBackground(shape(TEAL_SOFT,14));note.setPadding(dp(12),dp(10),dp(12),dp(10));margin(note,0,12,0,0);body.addView(note);
    }
    View bigMetric(String label,String num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);TextView b=tv(num,25,INK,true);b.setGravity(Gravity.CENTER);l.addView(b);TextView c=tv(unit,10,MUTED,false);c.setGravity(Gravity.CENTER);l.addView(c);return l;}
    View makeWeekChart(){LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setGravity(Gravity.BOTTOM);Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,-6);int max=1;int[] vals=new int[7];String[] labs=new String[7];SimpleDateFormat f=new SimpleDateFormat("M/d",Locale.getDefault());for(int i=0;i<7;i++){Date d=cal.getTime();vals[i]=store.dayDone(d);labs[i]=f.format(d);max=Math.max(max,vals[i]);cal.add(Calendar.DAY_OF_YEAR,1);}for(int i=0;i<7;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView val=tv(String.valueOf(vals[i]),10,MUTED,true);val.setGravity(Gravity.CENTER);col.addView(val);View bar=new View(this);bar.setBackground(shape(i==6?TEAL:Color.rgb(151,222,205),8));int h=dp(18+(int)(90.0*vals[i]/max));col.addView(bar,new LinearLayout.LayoutParams(dp(28),h));TextView lab=tv(labs[i],9,MUTED,false);lab.setGravity(Gravity.CENTER);col.addView(lab);outer.addView(col,new LinearLayout.LayoutParams(0,dp(145),1));}return outer;}

    void showSettings(){
        currentScreen="settings";categoryOpenedFromStudy=false;
        shell("我的","设置、备份与内容反馈。","我的");LinearLayout s=box(SURFACE,18,20);s.setBackground(strokeShape(SURFACE,20,LINE));s.addView(tv("每日学习设置",19,INK,true));TextView desc=tv("统一设置每天希望完成的卡片数量。复习与新学习不再分别计数。",13,MUTED,false);margin(desc,0,6,0,10);s.addView(desc);EditText goal=new EditText(this);goal.setInputType(2);goal.setText(String.valueOf(store.dailyGoal()));goal.setHint("例如 30");goal.setBackground(strokeShape(Color.rgb(249,251,250),13,LINE));goal.setPadding(dp(12),0,dp(12),0);s.addView(goal,new LinearLayout.LayoutParams(-1,dp(50)));Button save=btn("保存每日学习量",TEAL,Color.WHITE);margin(save,0,10,0,0);save.setOnClickListener(v->{int g=parse(goal,30);store.setDailyGoal(g);Toast.makeText(this,"已保存为每天 "+store.dailyGoal()+" 张",Toast.LENGTH_SHORT).show();showSettings();});s.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));body.addView(s);
        LinearLayout info=box(SURFACE,18,20);info.setBackground(strokeShape(SURFACE,20,LINE));margin(info,0,12,0,0);info.addView(tv("智能复习规则",18,INK,true));info.addView(tv("• 复习卡只能来自已学习内容\n• 已学习卡可重复复习\n• 每张卡保留最近10次熟悉/不清楚记录\n• 最近多次不清楚的卡片优先级更高\n• 薄弱卡在同一学习队列内可重复出现2—3次\n• 所有学习数据只保存在本机",14,MUTED,false));body.addView(info);
        LinearLayout data=box(SURFACE,18,20);data.setBackground(strokeShape(SURFACE,20,LINE));margin(data,0,12,0,0);data.addView(tv("数据备份与恢复",18,INK,true));data.addView(tv("建议每次安装新版本前导出一次完整备份。备份包含学习进度、最近10次记录、每日学习量、未完成会话、收藏、本地修正和内容反馈。",13,MUTED,false));LinearLayout dr=new LinearLayout(this);dr.setOrientation(LinearLayout.HORIZONTAL);Button backup=btn("导出全部数据",TEAL,Color.WHITE),restore=btn("导入并恢复",TEAL_SOFT,TEAL_DARK);backup.setOnClickListener(v->exportBackup());restore.setOnClickListener(v->confirmImportBackup());dr.addView(backup,new LinearLayout.LayoutParams(0,dp(50),1));dr.addView(gap(8));dr.addView(restore,new LinearLayout.LayoutParams(0,dp(50),1));margin(dr,0,10,0,0);data.addView(dr);TextView guard=tv("升级保障：固定 applicationId + 固定 Release 签名 + 数据库只做迁移，不在升级时清空用户数据。",12,MUTED,false);margin(guard,0,10,0,0);data.addView(guard);body.addView(data);
        LinearLayout fb=box(SURFACE,18,20);fb.setBackground(strokeShape(SURFACE,20,LINE));margin(fb,0,12,0,0);fb.addView(tv("内容反馈数据",18,INK,true));fb.addView(tv("已记录 "+feedback.pendingCount()+" 条待校对内容。应用中的本地修正和错误标记不会自动发送给 ChatGPT；请导出 JSON 后在聊天中上传，我就能逐卡核对并回写题库。",13,MUTED,false));Button export=btn("导出内容反馈 JSON",TEAL,Color.WHITE);margin(export,0,10,0,0);export.setOnClickListener(v->exportFeedback());fb.addView(export,new LinearLayout.LayoutParams(-1,dp(52)));body.addView(fb);
        Button reset=btn("清空学习进度",RED_SOFT,Color.rgb(150,70,70));margin(reset,0,16,0,0);reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("将删除所有卡片学习记录、最近10次记录和每日设置。收藏、内容反馈与本地修正不会被此按钮删除。建议先导出完整备份。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{store.clearAll();queue.clear();showSettings();}).show());body.addView(reset,new LinearLayout.LayoutParams(-1,dp(52)));
    }

    void showExample(Card c){JSONObject d=repo.detail(c.topic);String x=d==null?"":d.optString("example");new AlertDialog.Builder(this).setTitle(c.topic+" · 例子").setMessage(x.isEmpty()?"当前资料没有单列例子。":x).setPositiveButton("关闭",null).show();}
    void showFull(Card c){JSONObject d=repo.detail(c.topic);if(d==null)return;String[] labels={"名词解释","简答","论述/案例"},keys={"name","simple","essay"};new AlertDialog.Builder(this).setTitle(c.topic+" · 完整答案").setItems(labels,(dlg,which)->new AlertDialog.Builder(this).setTitle(labels[which]).setMessage(d.optString(keys[which],"暂无")).setPositiveButton("关闭",null).show()).setNegativeButton("关闭",null).show();}
    void showHistory(Card c){StudyStore.State s=store.state(c.id);StringBuilder b=new StringBuilder();int i=1;SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault());for(StudyStore.Rec r:s.history)b.append(i++).append(". ").append(r.ok?"熟悉":"不清楚").append("  ").append(f.format(new Date(r.ts))).append("\n");if(s.history.isEmpty())b.append("暂无记录");b.append("\n薄弱度：").append(Math.round(engine.difficulty(c)*100)).append("%");new AlertDialog.Builder(this).setTitle("最近10次 · "+c.topic).setMessage(b.toString()).setPositiveButton("关闭",null).show();}

    void showCategoryCards(String name,List<Card> cards){
        currentScreen=categoryOpenedFromStudy?"categoryFromStudy":"category";
        shell(name,"查看、学习、校对本专题卡片","卡片库");
        TextView note=tv("点击“学习/复习”进入卡片；点击“编辑”可在本机覆盖内容；发现错误也可以只标记，不必当场修改。",12,MUTED,false);note.setBackground(shape(TEAL_SOFT,14));note.setPadding(dp(12),dp(10),dp(12),dp(10));margin(note,0,0,0,10);body.addView(note);
        for(Card c:cards){
            LinearLayout item=box(SURFACE,14,16);item.setBackground(strokeShape(SURFACE,16,LINE));
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.addView(tv(c.question,15,INK,true));txt.addView(tv(c.kind+" · "+(engine.learned(c)?"已学习":"未学习")+(store.isFavorite(c.id)?" · ★ 已收藏":"")+(feedback.hasCardOverride(c.id)?" · 已本地修正":""),11,MUTED,false));line.addView(txt,new LinearLayout.LayoutParams(0,-2,1));TextView badge=tv(feedback.hasCardOverride(c.id)?"已修正":"",10,TEAL_DARK,true);line.addView(badge);item.addView(line);
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button learn=btn(engine.learned(c)?"学习/复习":"开始学习",TEAL_SOFT,TEAL_DARK),edit=btn("编辑",Color.rgb(247,249,248),MUTED),report=btn("标记有误",RED_SOFT,Color.rgb(164,70,70));learn.setOnClickListener(v->{categoryOpenedFromStudy=false;queue=engine.learned(c)?engine.reviewOnlyQueue(Collections.singletonList(c),Math.min(3,store.dailyGoal())):new ArrayList<>(Collections.singletonList(c));qIndex=0;revealed=false;store.saveSession(queue,0);showStudy();});edit.setOnClickListener(v->editCard(c));report.setOnClickListener(v->reportCard(c));actions.addView(learn,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(gap(6));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(gap(6));actions.addView(report,new LinearLayout.LayoutParams(0,dp(42),1));margin(actions,0,9,0,0);item.addView(actions);margin(item,0,0,0,8);body.addView(item);
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
        if("categoryFromStudy".equals(currentScreen)){
            categoryOpenedFromStudy=false;
            showStudy();
            return;
        }
        if("study".equals(currentScreen)){
            if(!queue.isEmpty()&&qIndex<queue.size())store.saveSession(queue,qIndex);
            showHome();
            return;
        }
        if("category".equals(currentScreen)){
            showLibrary();
            return;
        }
        if("library".equals(currentScreen)||"stats".equals(currentScreen)||"settings".equals(currentScreen)){
            showHome();
            return;
        }
        super.onBackPressed();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        if((requestCode==EXPORT_FEEDBACK_REQ||requestCode==EXPORT_BACKUP_REQ)&&pendingExport!=null){
            try{java.io.OutputStream out=getContentResolver().openOutputStream(data.getData());out.write(pendingExport.getBytes("UTF-8"));out.close();boolean full=requestCode==EXPORT_BACKUP_REQ;pendingExport=null;Toast.makeText(this,full?"完整备份已导出。请妥善保存，可用于版本升级、换机或误卸载后的恢复。":"反馈 JSON 已导出。把该文件上传到聊天即可继续校对。",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"导出失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
        }else if(requestCode==IMPORT_BACKUP_REQ){
            try{String raw=readText(data.getData());backupManager.importAll(raw,true);queue.clear();repo=new CardRepository(this,feedback);engine=new ReviewEngine(store);Toast.makeText(this,"备份恢复成功。学习进度、本地修正与反馈数据已载入。",Toast.LENGTH_LONG).show();showHome();}catch(Exception e){Toast.makeText(this,"恢复失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }

    int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return def;}}
}
