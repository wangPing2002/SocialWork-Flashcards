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
    CardRepository repo; StudyStore store; ReviewEngine engine; MockExamEngine mockEngine; ContentFeedbackStore feedback; DataBackupManager backupManager;
    LinearLayout root,body,nav; List<Card> queue=new ArrayList<>(); int qIndex=0; boolean revealed=false; int answerTab=0;
    boolean mockMode=false; String mockSubject=""; MockExamEngine.Paper mockPaper=null;
    String pendingExport=null; static final int EXPORT_FEEDBACK_REQ=91, EXPORT_BACKUP_REQ=92, IMPORT_BACKUP_REQ=93;
    String currentScreen="home"; boolean categoryOpenedFromStudy=false; String activePrimaryTopic="";
    String activeListName=""; final ArrayList<String> activeListIds=new ArrayList<>();
    String studyReturnScreen="", studyReturnName="", studyReturnPrimary=""; final ArrayList<String> studyReturnIds=new ArrayList<>();
    final HashMap<String,Integer> examRelationCounts=new HashMap<>();
    JSONObject serviceFrameworkCache=null; String currentFrameworkNodeId=""; boolean currentFrameworkCoreOnly=false;
    String studyReturnFrameworkNodeId=""; boolean studyReturnFrameworkCoreOnly=false;
    JSONObject quickMemoryCache=null; boolean quickMemoryMode=false; String quickMemoryCardId="";
    static boolean crashRecorderInstalled=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);installCrashRecorder();
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        try{feedback=new ContentFeedbackStore(this);repo=new CardRepository(this,feedback);store=new StudyStore(this);engine=new ReviewEngine(store);mockEngine=new MockExamEngine(repo,store,engine);backupManager=new DataBackupManager(this,store,feedback);showHome();}
        catch(Exception e){TextView t=new TextView(this);t.setPadding(24,24,24,24);t.setText("启动失败："+e);setContentView(t);}
    }

    void installCrashRecorder(){
        if(crashRecorderInstalled)return;crashRecorderInstalled=true;final android.content.Context app=getApplicationContext();final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{try{java.io.StringWriter sw=new java.io.StringWriter();java.io.PrintWriter pw=new java.io.PrintWriter(sw);error.printStackTrace(pw);pw.flush();app.getSharedPreferences("crash_diag",MODE_PRIVATE).edit().putLong("time",System.currentTimeMillis()).putString("trace",sw.toString()).commit();}catch(Exception ignored){}if(previous!=null)previous.uncaughtException(thread,error);});
    }
    String lastCrashTrace(){return getSharedPreferences("crash_diag",MODE_PRIVATE).getString("trace","");}
    void showLastCrash(){String trace=lastCrashTrace();if(trace.isEmpty()){Toast.makeText(this,"暂无已记录的异常退出",Toast.LENGTH_SHORT).show();return;}ScrollView sv=new ScrollView(this);TextView t=tv(trace,11,INK,false);t.setTextIsSelectable(true);t.setPadding(dp(14),dp(10),dp(14),dp(10));sv.addView(t);new AlertDialog.Builder(this).setTitle("上次异常退出信息").setView(sv).setNegativeButton("关闭",null).setNeutralButton("清除",(d,w)->getSharedPreferences("crash_diag",MODE_PRIVATE).edit().clear().apply()).show();}

    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    GradientDrawable strokeShape(int color,int radius,int strokeColor){GradientDrawable g=shape(color,radius);g.setStroke(dp(1),strokeColor);return g;}
    TextView tv(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);v.setLineSpacing(0,1.15f);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    Button btn(String s,int bgc,int tc){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(tc);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(bgc,15));b.setPadding(dp(12),0,dp(12),0);return b;}
    LinearLayout box(int color,int pad,int radius){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(pad),dp(pad),dp(pad),dp(pad));l.setBackground(shape(color,radius));return l;}
    void margin(View v,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));v.setLayoutParams(p);}
    Space gap(int w){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return s;}
    ImageView iconView(int resId,int size,int color){ImageView v=new ImageView(this);v.setImageResource(resId);v.setColorFilter(color);v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setPadding(dp(2),dp(2),dp(2),dp(2));v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return v;}

ImageView tapIcon(int resId,int size,int color,View.OnClickListener click){ImageView v=iconView(resId,size,color);v.setClickable(true);v.setFocusable(true);v.setOnClickListener(click);return v;}
View rateAction(int iconRes,String title,String sub,int bg,int color,View.OnClickListener click){
    LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.HORIZONTAL);wrap.setGravity(Gravity.CENTER);wrap.setBackground(shape(bg,16));wrap.setPadding(dp(9),dp(6),dp(9),dp(6));wrap.setOnClickListener(click);
    wrap.addView(iconView(iconRes,18,color),new LinearLayout.LayoutParams(dp(22),dp(22)));
    LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(6),0,0,0);TextView a=tv(title,13,color,true);TextView b=tv(sub,10,color,false);txt.addView(a);txt.addView(b);wrap.addView(txt);return wrap;
}

View sectionHeader(String title,String action,View.OnClickListener click){
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=tv(title,18,INK,true);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
    if(action!=null&&!action.isEmpty()){TextView a=tv(action,12,MUTED,false);a.setPadding(dp(8),dp(4),0,dp(4));if(click!=null)a.setOnClickListener(click);row.addView(a);}return row;
}
TextView chip(String label,boolean active){TextView c=tv(label,12,active?TEAL_DARK:MUTED,active);c.setGravity(Gravity.CENTER);c.setBackground(active?strokeShape(TEAL_SOFT,14,Color.rgb(190,229,220)):shape(Color.rgb(246,248,247),14));c.setPadding(dp(12),dp(7),dp(12),dp(7));return c;}
View masteryRow(String label,int value,int total,int color){
    LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);line.addView(tv(label,13,INK,true),new LinearLayout.LayoutParams(0,-2,1));line.addView(tv(String.valueOf(value),12,MUTED,true));wrap.addView(line);
    ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,total));p.setProgress(value);p.getProgressDrawable().setTint(color);margin(p,0,6,0,0);wrap.addView(p,new LinearLayout.LayoutParams(-1,dp(5)));return wrap;
}
int categoryIconRes(String n){
    if(n==null)return R.drawable.ic_cat_theory;
    if(n.equals("个案工作"))return R.drawable.ic_cat_case;
    if(n.equals("小组工作"))return R.drawable.ic_cat_group;
    if(n.equals("社区工作"))return R.drawable.ic_cat_community;
    if(n.contains("行政")||n.contains("督导")||n.contains("研究")||n.contains("政策"))return R.drawable.ic_cat_management;
    if(n.contains("实务")||n.contains("人群")||n.contains("场域"))return R.drawable.ic_cat_practice;
    return R.drawable.ic_cat_theory;
}

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

    int pageIconRes(String active){if("卡片库".equals(active))return R.drawable.ic_nav_cards;if("统计".equals(active))return R.drawable.ic_nav_stats;if("我的".equals(active))return R.drawable.ic_nav_profile;return R.drawable.ic_nav_home;}

    void shell(String title,String subtitle,String active){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);setContentView(root);applySafeInsets(root);

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        boolean home="学习".equals(active);
        header.setPadding(dp(18),home?dp(8):dp(5),dp(18),home?dp(6):dp(3));
        if(home){
            ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_brand_hands);mark.setScaleType(ImageView.ScaleType.CENTER_CROP);header.addView(mark,new LinearLayout.LayoutParams(dp(46),dp(46)));
        }else{
            LinearLayout mark=new LinearLayout(this);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(TEAL_SOFT,14));mark.addView(iconView(pageIconRes(active),20,TEAL_DARK),new LinearLayout.LayoutParams(dp(22),dp(22)));header.addView(mark,new LinearLayout.LayoutParams(dp(38),dp(38)));
        }
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(home?dp(12):dp(10),0,0,0);titles.addView(tv(title,home?26:23,INK,true));
        if(subtitle!=null&&!subtitle.isEmpty()){TextView sub=tv(subtitle,home?13:12,MUTED,false);margin(sub,0,2,0,0);titles.addView(sub);}header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));root.addView(header);

        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setClipToPadding(false);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(4),dp(16),dp(92));sv.addView(body);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setPadding(dp(10),dp(5),dp(10),dp(6));nav.setBackgroundColor(Color.WHITE);
        addNav(R.drawable.ic_nav_home,"学习","学习".equals(active),v->showHome());addNav(R.drawable.ic_nav_cards,"卡片库","卡片库".equals(active),v->showLibrary());addNav(R.drawable.ic_nav_stats,"统计","统计".equals(active),v->showStats());addNav(R.drawable.ic_nav_profile,"我的","我的".equals(active),v->showSettings());
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));
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
    LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(16),dp(8),dp(16),dp(6));
    ImageView back=tapIcon(R.drawable.ic_arrow_back,22,INK,v->onBackPressed());h.addView(back,new LinearLayout.LayoutParams(dp(42),dp(42)));
    String studyTitle=mockMode?(mockSubject+" 模拟中"):(hasStudyReturn()&&!studyReturnName.isEmpty()?studyReturnName:"学习中");TextView title=tv(studyTitle,19,INK,true);title.setGravity(Gravity.CENTER);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);h.addView(title,new LinearLayout.LayoutParams(0,-2,1));
    Space spacer=new Space(this);h.addView(spacer,new LinearLayout.LayoutParams(dp(42),dp(42)));root.addView(h);
    body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),0,dp(16),dp(14));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
}
int learnedCount(){int n=0;for(Card c:repo.cards)if(engine.learned(c))n++;return n;}
    int masteryCount(String tag){int n=0;for(Card c:repo.cards)if(store.state(c.id).tag.equals(tag))n++;return n;}
    int remainingToday(){return Math.max(0,store.dailyGoal()-store.todayDone());}


void showHome(){
    currentScreen="home";categoryOpenedFromStudy=false;activePrimaryTopic="";
    shell("社会工作闪卡","把知识连起来，把理解留下来。","学习");

    LinearLayout task=box(SURFACE,16,24);task.setBackground(strokeShape(SURFACE,24,LINE));
    LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);
    titleRow.addView(tv("今日任务",19,INK,true),new LinearLayout.LayoutParams(0,-2,1));
    String streak=store.streakDays()>0?"连续 "+store.streakDays()+" 天":"今天开始";
    TextView day=tv(streak,11,TEAL_DARK,true);day.setBackground(shape(TEAL_SOFT,12));day.setPadding(dp(9),dp(5),dp(9),dp(5));titleRow.addView(day);task.addView(titleRow);

    LinearLayout numberRow=new LinearLayout(this);numberRow.setGravity(Gravity.BOTTOM);
    TextView done=tv(String.valueOf(store.todayDone()),42,INK,true);numberRow.addView(done);
    TextView total=tv(" / "+store.dailyGoal(),17,MUTED,true);total.setPadding(0,0,0,dp(6));numberRow.addView(total);
    TextView remain=tv(remainingToday()==0?"今日目标已完成":"还差 "+remainingToday()+" 张",12,remainingToday()==0?TEAL_DARK:MUTED,remainingToday()==0);remain.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);numberRow.addView(remain,new LinearLayout.LayoutParams(0,-1,1));margin(numberRow,0,6,0,0);task.addView(numberRow);

    ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(Math.max(1,store.dailyGoal()));p.setProgress(Math.min(store.todayDone(),store.dailyGoal()));p.getProgressDrawable().setTint(TEAL);margin(p,0,8,0,10);task.addView(p,new LinearLayout.LayoutParams(-1,dp(6)));

    LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);
    stats.addView(miniStat("已学习",learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));
    stats.addView(miniStat("待学习",repo.cards.size()-learnedCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));
    stats.addView(miniStat("收藏",favoriteCount(),"张"),new LinearLayout.LayoutParams(0,-2,1));task.addView(stats);

    Button start=btn("开始今日学习",TEAL,Color.WHITE);start.setTextSize(16);margin(start,0,12,0,0);start.setOnClickListener(v->startNewSession());task.addView(start,new LinearLayout.LayoutParams(-1,dp(52)));
    if(store.hasActiveSession()){
        TextView cont=tv("继续上一次  →",13,TEAL_DARK,true);cont.setGravity(Gravity.CENTER);cont.setPadding(dp(8),dp(10),dp(8),dp(6));cont.setOnClickListener(v->continueSession());task.addView(cont);
    }
    body.addView(task);

    View priority=sectionHeader("优先刷题","真题 → 关联知识点",null);margin(priority,2,18,2,8);body.addView(priority);
    List<Card> homeExamCards=realExamCards(),homeRelatedCards=examRelatedKnowledgeCards();
    View trueBank=priorityBankRow("真题题库","优先建立东南大学命题感",homeExamCards.size(),ORANGE,v->showBankCards("真题题库",realExamCards(),"历年真题与回忆主题","examBank"));body.addView(trueBank);margin(trueBank,0,0,0,7);
    View knowledgeBank=priorityBankRow("真题关联知识点","刷完真题后补齐直接相关考点",homeRelatedCards.size(),TEAL_DARK,v->showBankCards("真题关联知识点",examRelatedKnowledgeCards(),"由真题的关联问题自动汇总","relatedBank"));body.addView(knowledgeBank);margin(knowledgeBank,0,0,0,4);

    LinearLayout section=new LinearLayout(this);section.setGravity(Gravity.CENTER_VERTICAL);TextView st=tv("真题模拟",20,INK,true);section.addView(st,new LinearLayout.LayoutParams(0,-2,1));TextView more=tv("题库仍在卡片库  ›",12,MUTED,false);more.setOnClickListener(v->showLibrary());section.addView(more);margin(section,2,18,2,8);body.addView(section);
    TextView simNote=tv("参考2015—2026东南大学真题结构，结合内容范围、考频、收藏与熟练度智能随机组卷。",11,MUTED,false);margin(simNote,2,0,2,10);body.addView(simNote);
    body.addView(mockExamEntry("331","社会工作原理","5 名词解释 · 5 简答 · 3 论述","150 分"));
    body.addView(mockExamEntry("437","社会工作实务","3 名词解释 · 2 简答 · 1 材料/案例 · 1 论述","150 分"));
}
View miniStat(String label,int num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);LinearLayout value=new LinearLayout(this);value.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView n=tv(num+"",22,INK,true);value.addView(n);TextView u=tv(unit,10,MUTED,false);u.setPadding(dp(3),0,0,dp(3));value.addView(u);l.addView(value);return l;}

View mockExamEntry(String code,String name,String distribution,String score){
    LinearLayout card=box(SURFACE,14,19);card.setBackground(strokeShape(SURFACE,19,LINE));
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout codeBox=new LinearLayout(this);codeBox.setGravity(Gravity.CENTER);codeBox.setBackground(shape("331".equals(code)?TEAL_SOFT:BLUE_SOFT,15));TextView codeText=tv(code,15,"331".equals(code)?TEAL_DARK:Color.rgb(36,145,218),true);codeText.setGravity(Gravity.CENTER);codeBox.addView(codeText,new LinearLayout.LayoutParams(dp(48),dp(48)));top.addView(codeBox,new LinearLayout.LayoutParams(dp(54),dp(54)));
    LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(10),0,dp(8),0);txt.addView(tv(name,17,INK,true));TextView dist=tv(distribution,11,MUTED,false);margin(dist,0,3,0,0);txt.addView(dist);top.addView(txt,new LinearLayout.LayoutParams(0,-2,1));
    TextView total=tv(score,11,TEAL_DARK,true);total.setBackground(shape(TEAL_SOFT,12));total.setPadding(dp(9),dp(5),dp(9),dp(5));top.addView(total);card.addView(top);
    LinearLayout factors=new LinearLayout(this);factors.setOrientation(LinearLayout.HORIZONTAL);String[] fs={"考频","收藏","熟练度","内容范围"};for(int i=0;i<fs.length;i++){TextView f=tv(fs[i],10,MUTED,true);f.setGravity(Gravity.CENTER);f.setBackground(shape(Color.rgb(247,249,248),10));f.setPadding(dp(7),dp(5),dp(7),dp(5));factors.addView(f,new LinearLayout.LayoutParams(0,-2,1));if(i<fs.length-1)factors.addView(gap(5));}margin(factors,0,10,0,10);card.addView(factors);
    Button make=btn("随机生成模拟卷",TEAL,Color.WHITE);make.setTextSize(13);make.setOnClickListener(v->generateMockExam(code));card.addView(make,new LinearLayout.LayoutParams(-1,dp(44)));margin(card,0,0,0,9);return card;
}

void generateMockExam(String subject){
    clearStudyReturn();mockPaper=mockEngine.generate(subject);mockSubject=subject;mockMode=true;showMockExamPreview(mockPaper);
}

void showMockExamPreview(MockExamEngine.Paper paper){
    currentScreen="mockPreview";mockPaper=paper;mockSubject=paper.subject;mockMode=true;
    shell("真题模拟",paper.subject+" · "+paper.name+" · "+paper.totalScore()+" 分","学习");
    TextView back=tv("‹  返回首页",12,TEAL_DARK,true);back.setPadding(dp(2),dp(2),dp(2),dp(8));back.setOnClickListener(v->showHome());body.addView(back);
    LinearLayout head=box(SURFACE,15,20);head.setBackground(strokeShape(SURFACE,20,LINE));
    LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);TextView title=tv(paper.subject+"  "+paper.name,19,INK,true);hr.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView total=tv(paper.totalScore()+" 分",12,TEAL_DARK,true);total.setBackground(shape(TEAL_SOFT,12));total.setPadding(dp(10),dp(5),dp(10),dp(5));hr.addView(total);head.addView(hr);
    TextView note=tv(paper.formatNote,12,MUTED,false);margin(note,0,6,0,0);head.addView(note);
    TextView rule=tv("智能组卷权重：系统/手动考频 + 收藏 + 当前熟练度 + 薄弱度 + 真题科目匹配 + 内容类别；不同类别会做平衡。",11,MUTED,false);rule.setBackground(shape(Color.rgb(247,249,248),11));rule.setPadding(dp(10),dp(8),dp(10),dp(8));margin(rule,0,10,0,0);head.addView(rule);body.addView(head);

    int sectionIndex=0;
    for(MockExamEngine.Section sec:paper.sections){
        View h=sectionHeader(sec.title,sec.cards.size()+" 题 · "+sec.subtotal()+" 分",null);margin(h,2,18,2,8);body.addView(h);
        int i=1;for(Card c:sec.cards)body.addView(mockQuestionRow(i++,c,sec));sectionIndex++;
    }
    LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);margin(actions,0,10,0,0);
    Button refresh=btn("重新组卷",Color.rgb(239,245,243),TEAL_DARK);refresh.setOnClickListener(v->generateMockExam(paper.subject));actions.addView(refresh,new LinearLayout.LayoutParams(0,dp(50),1));actions.addView(gap(9));
    Button start=btn("开始模拟",TEAL,Color.WHITE);start.setOnClickListener(v->startMockPaper(paper));actions.addView(start,new LinearLayout.LayoutParams(0,dp(50),1));body.addView(actions);
}

View mockQuestionRow(int no,Card c,MockExamEngine.Section sec){
    LinearLayout item=box(SURFACE,12,16);item.setBackground(strokeShape(SURFACE,16,LINE));
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.TOP);TextView n=tv(String.valueOf(no),11,Color.WHITE,true);n.setGravity(Gravity.CENTER);n.setBackground(shape(TEAL,10));top.addView(n,new LinearLayout.LayoutParams(dp(28),dp(28)));
    LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(10),0,0,0);TextView q=tv(c.question,14,INK,true);q.setMaxLines(4);content.addView(q);
    LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);TextView src=tv(repo.examSourceLabel(c),10,repo.isRealExam(c)?ORANGE:MUTED,true);meta.addView(src);TextView freq=tv(" · 考频"+mockEngine.effectiveFrequency(c)+" · 熟练度"+mockEngine.mastery(c),10,MUTED,false);meta.addView(freq);margin(meta,0,5,0,0);content.addView(meta);top.addView(content,new LinearLayout.LayoutParams(0,-2,1));item.addView(top);margin(item,0,0,0,7);return item;
}

void startMockPaper(MockExamEngine.Paper paper){
    queue=paper.flattened();qIndex=0;revealed=false;answerTab=0;mockMode=true;mockSubject=paper.subject;store.saveSession(queue,0);store.saveSessionMeta("mock",mockSubject);showStudy();
}

View frequencyMasteryPanel(Card c){
    LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.HORIZONTAL);wrap.setPadding(0,dp(2),0,0);
    String system=repo.systemFrequency(c),over=store.frequencyOverride(c.id),freq=over==null||over.isEmpty()?system:over,mastery=mockEngine.mastery(c);
    LinearLayout f=metricControl("考频",freq,(over==null||over.isEmpty()?"系统判定":"已手动调整")+" · 点击修改",freqColor(freq),v->editFrequency(c));
    LinearLayout m=metricControl("熟练度",mastery,engine.learned(c)?"依据最近10次表现":"尚未学习",masteryColor(mastery),null);
    wrap.addView(f,new LinearLayout.LayoutParams(0,dp(50),1));wrap.addView(gap(7));wrap.addView(m,new LinearLayout.LayoutParams(0,dp(50),1));return wrap;
}

LinearLayout metricControl(String label,String level,String sub,int color,View.OnClickListener click){
    LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.HORIZONTAL);b.setGravity(Gravity.CENTER_VERTICAL);b.setBackground(strokeShape(Color.rgb(248,250,249),13,LINE));b.setPadding(dp(9),dp(5),dp(9),dp(5));if(click!=null){b.setClickable(true);b.setOnClickListener(click);}
    TextView lv=tv(level,18,color,true);lv.setGravity(Gravity.CENTER);b.addView(lv,new LinearLayout.LayoutParams(dp(30),-2));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(6),0,0,0);tx.addView(tv(label,11,INK,true));TextView st=tv(sub,9,MUTED,false);st.setMaxLines(1);tx.addView(st);b.addView(tx,new LinearLayout.LayoutParams(0,-2,1));if(click!=null)b.addView(tv("›",18,MUTED,false));return b;
}

int freqColor(String level){return "高".equals(level)?RED:"中".equals(level)?Color.rgb(195,139,34):TEAL_DARK;}
int masteryColor(String level){return "高".equals(level)?TEAL_DARK:"中".equals(level)?Color.rgb(195,139,34):ORANGE;}

void editFrequency(Card c){
    String sys=repo.systemFrequency(c);
    String over=store.frequencyOverride(c.id);
    String current=(over==null||over.isEmpty())?sys:over;

    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setPadding(dp(22),dp(6),dp(22),0);

    TextView note=tv("手动考频会参与后续真题模拟的随机组卷权重；熟练度仍由学习记录自动计算。",12,MUTED,false);
    note.setPadding(0,0,0,dp(8));
    panel.addView(note);

    RadioGroup group=new RadioGroup(this);
    group.setOrientation(RadioGroup.VERTICAL);
    String[] values={"高","中","低",""};
    String[] labels={"高频","中频","低频","跟随系统（当前："+sys+"）"};
    int checkedId=View.NO_ID;
    for(int i=0;i<labels.length;i++){
        RadioButton rb=new RadioButton(this);
        int id=View.generateViewId();
        rb.setId(id);
        rb.setText(labels[i]);
        rb.setTextSize(14);
        rb.setTextColor(INK);
        rb.setTag(values[i]);
        rb.setPadding(0,dp(5),0,dp(5));
        group.addView(rb,new RadioGroup.LayoutParams(-1,dp(44)));
        boolean selected=(i==3&&(over==null||over.isEmpty()))||(i<3&&values[i].equals(current)&&over!=null&&!over.isEmpty());
        if(selected)checkedId=id;
    }
    if(checkedId!=View.NO_ID)group.check(checkedId);
    panel.addView(group);

    AlertDialog dialog=new AlertDialog.Builder(this)
        .setTitle("调整本题考频")
        .setView(panel)
        .setNegativeButton("取消",null)
        .setPositiveButton("保存",null)
        .create();

    dialog.setOnShowListener(x->{
        Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        save.setTextColor(TEAL_DARK);
        save.setOnClickListener(v->{
            int id=group.getCheckedRadioButtonId();
            if(id==View.NO_ID){
                Toast.makeText(this,"请选择考频",Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selected=group.findViewById(id);
            String value=String.valueOf(selected.getTag());
            store.setFrequencyOverride(c.id,value);
            dialog.dismiss();
            String effective=value.isEmpty()?sys:value;
            Toast.makeText(this,value.isEmpty()?"已恢复系统考频："+effective:"考频已调整为："+effective,Toast.LENGTH_SHORT).show();
            showStudy();
        });
    });
    dialog.show();
}

void startNewSession(){
        clearStudyReturn();mockMode=false;mockSubject="";mockPaper=null;
        int count=remainingToday();if(count<=0)count=store.dailyGoal();
        queue=engine.learningQueue(repo.cards,count);qIndex=0;revealed=false;store.saveSession(queue,0);store.saveSessionMeta("study","");showStudy();
    }
    void continueSession(){
        if(!store.hasActiveSession()){Toast.makeText(this,"当前没有中断的学习进度，将开始新的学习。",Toast.LENGTH_SHORT).show();startNewSession();return;}
        queue=new ArrayList<>();for(String id:store.sessionIds()){Card c=repo.byId(id);if(c!=null)queue.add(c);}if(queue.isEmpty()){store.clearSession();Toast.makeText(this,"旧题库会话已失效，将按新2497题母版开始学习。",Toast.LENGTH_SHORT).show();startNewSession();return;}qIndex=Math.min(store.sessionIndex(),queue.size());revealed=false;
        mockMode="mock".equals(store.sessionMode());mockSubject=store.sessionSubject();showStudy();
    }


void showStudy(){
    currentScreen="study";
    studyShell();
    if(!revealed)answerTab=0;
    if(queue.isEmpty()||qIndex>=queue.size()){
        store.clearSession();
        LinearLayout done=box(SURFACE,24,22);done.setBackground(strokeShape(SURFACE,22,LINE));
        LinearLayout.LayoutParams dpDone=new LinearLayout.LayoutParams(-1,0,1);dpDone.setMargins(0,0,0,dp(8));done.setLayoutParams(dpDone);done.setGravity(Gravity.CENTER);
        TextView title=tv(mockMode?"本次模拟完成":"本轮学习完成",22,INK,true);title.setGravity(Gravity.CENTER);done.addView(title);
        TextView d=tv(mockMode?(mockSubject+" 模拟题已全部完成。考频与熟练度会继续参与下一次智能组卷。"):("今天已完成 "+store.todayDone()+" 张。薄弱卡会依据最近10次记录提高后续出现概率。"),14,MUTED,false);d.setGravity(Gravity.CENTER);margin(d,0,8,0,16);done.addView(d);
        if(hasStudyReturn()&&!mockMode){
            Button backToList=btn("返回上一板块",TEAL,Color.WHITE);backToList.setOnClickListener(v->returnToStudyOrigin());done.addView(backToList,new LinearLayout.LayoutParams(-1,dp(52)));
            Button again=btn("继续加练",TEAL_SOFT,TEAL_DARK);margin(again,0,8,0,0);again.setOnClickListener(v->startNewSession());done.addView(again,new LinearLayout.LayoutParams(-1,dp(46)));
        }else{
            Button again=btn(mockMode?"再组一套":"继续加练",TEAL,Color.WHITE);again.setOnClickListener(v->{if(mockMode)generateMockExam(mockSubject);else startNewSession();});done.addView(again,new LinearLayout.LayoutParams(-1,dp(52)));
        }
        body.addView(done);return;
    }
    Card c=queue.get(qIndex);

    LinearLayout progress=new LinearLayout(this);progress.setOrientation(LinearLayout.VERTICAL);progress.setPadding(dp(2),0,dp(2),dp(2));
    LinearLayout pr=new LinearLayout(this);pr.setGravity(Gravity.CENTER_VERTICAL);TextView pt=tv(mockMode?(mockSubject+" 模拟进度"):"本轮进度",12,MUTED,true);pr.addView(pt,new LinearLayout.LayoutParams(0,-2,1));TextView pos=tv((qIndex+1)+" / "+queue.size(),12,INK,true);pr.addView(pos);progress.addView(pr);
    ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,queue.size()));pb.setProgress(Math.min(queue.size(),qIndex+1));pb.getProgressDrawable().setTint(TEAL);margin(pb,0,4,0,revealed?4:7);progress.addView(pb,new LinearLayout.LayoutParams(-1,dp(4)));body.addView(progress);

    LinearLayout card=box(SURFACE,revealed?13:16,24);card.setBackground(strokeShape(SURFACE,24,LINE));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,0,1);cp.setMargins(0,0,0,revealed?dp(9):0);card.setLayoutParams(cp);
    LinearLayout metaRow=new LinearLayout(this);metaRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView kind=tv(repo.questionType(c),10,TEAL_DARK,true);kind.setBackground(shape(TEAL_SOFT,11));kind.setPadding(dp(8),dp(3),dp(8),dp(3));metaRow.addView(kind);
    TextView sourceBadge=tv(repo.examSourceLabel(c),10,repo.isRealExam(c)?ORANGE:MUTED,true);sourceBadge.setBackground(shape(repo.isRealExam(c)?ORANGE_SOFT:Color.rgb(246,248,247),11));sourceBadge.setPadding(dp(8),dp(3),dp(8),dp(3));LinearLayout.LayoutParams sbp=new LinearLayout.LayoutParams(-2,-2);sbp.setMargins(dp(6),0,0,0);metaRow.addView(sourceBadge,sbp);
    TextView topic=tv(c.id+" · "+c.secondaryTopic,11,MUTED,false);topic.setPadding(dp(8),0,0,0);topic.setMaxLines(1);topic.setEllipsize(android.text.TextUtils.TruncateAt.END);metaRow.addView(topic,new LinearLayout.LayoutParams(0,-2,1));
    if(store.isFavorite(c.id)){TextView star=tv("★",17,Color.rgb(222,164,46),true);star.setPadding(0,0,dp(6),0);metaRow.addView(star);}
    TextView status=tv(engine.learned(c)?"已学习":"新学习",10,engine.learned(c)?TEAL_DARK:ORANGE,true);status.setBackground(shape(engine.learned(c)?TEAL_SOFT:ORANGE_SOFT,12));status.setPadding(dp(8),dp(3),dp(8),dp(3));metaRow.addView(status);
    ImageView more=tapIcon(R.drawable.ic_more_vertical,20,MUTED,v->showCardMenu(c,v));more.setPadding(dp(8),dp(8),dp(8),dp(8));metaRow.addView(more,new LinearLayout.LayoutParams(dp(38),dp(38)));card.addView(metaRow);

    if(!revealed){
        ScrollView questionScroll=new ScrollView(this);questionScroll.setFillViewport(true);questionScroll.setVerticalScrollBarEnabled(false);
        LinearLayout qWrap=new LinearLayout(this);qWrap.setOrientation(LinearLayout.VERTICAL);qWrap.setGravity(Gravity.CENTER_HORIZONTAL);qWrap.setPadding(dp(8),dp(50),dp(8),dp(20));
        TextView recall=tv("先回忆关键词，再组织完整答案",11,TEAL_DARK,true);recall.setBackground(shape(TEAL_SOFT,11));recall.setPadding(dp(9),dp(5),dp(9),dp(5));qWrap.addView(recall,new LinearLayout.LayoutParams(-2,-2));
        TextView q=tv(c.question,23,INK,true);q.setGravity(Gravity.CENTER);q.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);margin(q,dp(2),dp(24),dp(2),dp(12));qWrap.addView(q);
        questionScroll.addView(qWrap,new ScrollView.LayoutParams(-1,-2));card.addView(questionScroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout reveal=new LinearLayout(this);reveal.setGravity(Gravity.CENTER);reveal.setBackground(shape(TEAL_SOFT,16));reveal.setPadding(dp(12),dp(12),dp(12),dp(12));
        reveal.addView(iconView(R.drawable.ic_eye,19,TEAL_DARK),new LinearLayout.LayoutParams(dp(24),dp(24)));TextView rt=tv("查看答案",14,TEAL_DARK,true);rt.setPadding(dp(6),0,0,0);reveal.addView(rt);
        View.OnClickListener revealAction=v->{revealed=true;answerTab=0;showStudy();};reveal.setOnClickListener(revealAction);questionScroll.setOnClickListener(revealAction);qWrap.setOnClickListener(revealAction);q.setOnClickListener(revealAction);margin(reveal,0,8,0,0);card.addView(reveal,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView hint=tv("答案揭晓后再评价熟悉程度",11,Color.rgb(150,161,157),false);hint.setGravity(Gravity.CENTER);margin(hint,0,7,0,0);card.addView(hint);body.addView(card);return;
    }

    LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(0,dp(10),0,dp(6));
    tabs.addView(answerTab("核心答案",0,c),new LinearLayout.LayoutParams(0,dp(38),1));
    tabs.addView(answerTab("考频与真题",1,c),new LinearLayout.LayoutParams(0,dp(38),1));
    tabs.addView(answerTab("相关知识点",2,c),new LinearLayout.LayoutParams(0,dp(38),1));card.addView(tabs);

    ScrollView mid=new ScrollView(this);mid.setFillViewport(true);mid.setVerticalScrollBarEnabled(true);mid.setScrollbarFadingEnabled(true);
    LinearLayout inner=new LinearLayout(this);inner.setOrientation(LinearLayout.VERTICAL);inner.setPadding(dp(5),dp(5),dp(5),dp(8));
    if(answerTab==0)renderCoreAnswer(inner,c);else if(answerTab==1)renderExamInfo(inner,c);else renderRelatedKnowledge(inner,c);
    mid.addView(inner,new ScrollView.LayoutParams(-1,-2));card.addView(mid,new LinearLayout.LayoutParams(-1,0,1));

    View fm=frequencyMasteryPanel(c);margin(fm,0,dp(5),0,0);card.addView(fm);body.addView(card);

    LinearLayout rates=new LinearLayout(this);rates.setOrientation(LinearLayout.HORIZONTAL);
    rates.addView(rateAction(R.drawable.ic_repeat,"再看看","不清楚",ORANGE_SOFT,ORANGE,v->rate(c,false)),new LinearLayout.LayoutParams(0,dp(56),1));rates.addView(gap(9));
    rates.addView(rateAction(R.drawable.ic_check,"记住了","熟悉",TEAL_SOFT,TEAL_DARK,v->rate(c,true)),new LinearLayout.LayoutParams(0,dp(56),1));body.addView(rates);
}

View answerTab(String label,int tab,Card c){
    boolean active=answerTab==tab;LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setGravity(Gravity.CENTER);wrap.setOnClickListener(x->{answerTab=tab;showStudy();});
    TextView v=tv(label,12,active?TEAL_DARK:MUTED,active);v.setGravity(Gravity.CENTER);wrap.addView(v,new LinearLayout.LayoutParams(-1,0,1));
    View line=new View(this);line.setBackground(shape(active?TEAL:Color.TRANSPARENT,2));wrap.addView(line,new LinearLayout.LayoutParams(dp(34),dp(3)));return wrap;
}


void renderCoreAnswer(LinearLayout inner,Card c){
    if(c==null)return;
    if(!c.id.equals(quickMemoryCardId)){quickMemoryCardId=c.id;quickMemoryMode=false;}
    String answer=repo.fullAnswer(c);
    if(c.answerStatus!=null&&!c.answerStatus.isEmpty()&&!c.answerStatus.startsWith("✅")){
        TextView warn=tv("⚠ "+c.answerStatus,11,ORANGE,true);warn.setBackground(shape(ORANGE_SOFT,11));warn.setPadding(dp(10),dp(8),dp(10),dp(8));margin(warn,0,0,0,10);inner.addView(warn);
    }

    LinearLayout answerHead=new LinearLayout(this);answerHead.setGravity(Gravity.CENTER_VERTICAL);
    TextView answerTitle=tv(quickMemoryMode?"快速记忆":"完整解答",12,INK,true);answerHead.addView(answerTitle,new LinearLayout.LayoutParams(0,-2,1));
    TextView toggle=tv(quickMemoryMode?"↻  完整解答":"↻  快速记忆",11,TEAL_DARK,true);toggle.setGravity(Gravity.CENTER);toggle.setBackground(shape(TEAL_SOFT,12));toggle.setPadding(dp(10),dp(6),dp(10),dp(6));toggle.setOnClickListener(v->{quickMemoryMode=!quickMemoryMode;showStudy();});answerHead.addView(toggle);
    margin(answerHead,0,0,0,8);inner.addView(answerHead);

    if(quickMemoryMode){
        JSONObject memory=quickMemoryFor(c);
        String example=memory.optString("example","").trim();
        String mnemonic=memory.optString("mnemonic","").trim();

        LinearLayout exampleBox=box(Color.rgb(239,248,245),12,14);
        TextView exampleTitle=tv("理解例子",12,TEAL_DARK,true);exampleBox.addView(exampleTitle);
        TextView exampleBody=tv(example.isEmpty()?"暂无理解例子。":example,15,INK,false);exampleBody.setLineSpacing(dp(3),1.20f);exampleBody.setTextIsSelectable(true);margin(exampleBody,0,7,0,0);exampleBox.addView(exampleBody);inner.addView(exampleBox);

        LinearLayout mnemonicBox=box(Color.rgb(255,247,239),12,14);
        TextView mnemonicTitle=tv("记忆口诀",12,ORANGE,true);mnemonicBox.addView(mnemonicTitle);
        TextView mnemonicBody=tv(mnemonic.isEmpty()?"暂无记忆口诀。":mnemonic,15,INK,false);mnemonicBody.setLineSpacing(dp(3),1.20f);mnemonicBody.setTextIsSelectable(true);margin(mnemonicBody,0,7,0,0);mnemonicBox.addView(mnemonicBody);margin(mnemonicBox,0,10,0,0);inner.addView(mnemonicBox);
    }else{
        TextView body=tv("",16,INK,false);body.setText(richAnswer(answer));body.setLineSpacing(dp(3),1.20f);body.setTextIsSelectable(true);inner.addView(body);
    }

    String srcText="核心教材依据  "+c.origin;
    if(c.sourceLevel!=null&&!c.sourceLevel.isEmpty())srcText+="\n答案来源层级  "+c.sourceLevel;
    TextView src=tv(srcText,10,MUTED,false);src.setBackground(shape(Color.rgb(248,250,249),11));src.setPadding(dp(10),dp(8),dp(10),dp(8));margin(src,0,12,0,7);inner.addView(src);
    TextView scrollHint=tv(quickMemoryMode?"快速记忆用于理解与回忆 · 正式作答仍以完整解答为准":"上下滑动查看完整解答 · #C62828 红色为核心得分点",10,Color.rgb(151,163,160),false);scrollHint.setGravity(Gravity.CENTER);inner.addView(scrollHint);
}

CharSequence richAnswer(String raw){
    if(raw==null||raw.trim().isEmpty())return "暂无完整解答。";
    String x=raw.replace("&","&amp;");
    // Restore supported answer markup after escaping; the question bank uses #C62828 for core scoring points.
    x=x.replace("&lt;", "<").replace("&gt;", ">");
    x=x.replaceAll("(?i)<span\\s+style=[\"']color\\s*:\\s*#(?:C62828|D32F2F);?[\"']\\s*>","<font color=\"#C62828\"><b>");
    x=x.replace("</span>","</b></font>");
    x=x.replaceAll("\\*\\*([^*]+)\\*\\*","<b>$1</b>");
    x=x.replace("\n","<br>");
    if(Build.VERSION.SDK_INT>=24)return android.text.Html.fromHtml(x,android.text.Html.FROM_HTML_MODE_LEGACY);
    return android.text.Html.fromHtml(x);
}

    void renderExamInfo(LinearLayout inner,Card c){
        String sys=repo.systemFrequency(c),eff=mockEngine.effectiveFrequency(c);
        LinearLayout heat=box(sys.equals("高")?Color.rgb(235,247,255):sys.equals("中")?PURPLE_SOFT:GOLD_SOFT,12,15);
        LinearLayout hr=new LinearLayout(this);hr.setGravity(Gravity.CENTER_VERTICAL);TextView ht=tv("▥  系统考频",14,INK,true);hr.addView(ht,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=tv(sys,11,sys.equals("高")?Color.rgb(42,115,171):sys.equals("中")?Color.rgb(105,78,175):Color.rgb(161,118,24),true);badge.setBackground(shape(Color.WHITE,12));badge.setPadding(dp(9),dp(4),dp(9),dp(4));hr.addView(badge);heat.addView(hr);
        String raw=c.predictedFrequencyRaw==null||c.predictedFrequencyRaw.isEmpty()?sys:c.predictedFrequencyRaw;
        TextView note=tv("母版预测考频："+raw+" · 当前有效考频："+eff+"\n系统考频用于复习优先级和随机组卷权重，不表示考试概率。",12,INK,false);margin(note,0,7,0,0);heat.addView(note);inner.addView(heat);

        TextView title=tv("真题身份",14,INK,true);margin(title,0,14,0,7);inner.addView(title);
        LinearLayout truth=box(Color.rgb(248,250,249),12,15);truth.setBackground(strokeShape(Color.rgb(248,250,249),15,LINE));
        if(repo.isRealExam(c)){
            TextView tag=tv(repo.isExactExam(c)?"✓ 历年真题":"◫ 回忆版真题主题",12,repo.isExactExam(c)?TEAL_DARK:ORANGE,true);truth.addView(tag);
            String label=c.truthLabel==null?"":c.truthLabel.replace("✅ ","").replace("🟨 ","");
            if(!label.isEmpty()){TextView l=tv(label,13,INK,true);margin(l,0,7,0,0);truth.addView(l);}
            if(c.truthQuestion!=null&&!c.truthQuestion.isEmpty()){TextView q=tv("原题/主题："+c.truthQuestion,13,INK,false);margin(q,0,7,0,0);truth.addView(q);}
            if(c.truthEvidence!=null&&!c.truthEvidence.isEmpty()){TextView e=tv("证据："+c.truthEvidence,11,MUTED,false);margin(e,0,6,0,0);truth.addView(e);}
        }else{
            truth.addView(tv("本卡为真题化模拟题",13,INK,true));
            TextView rel=tv(c.truthRelation==null||c.truthRelation.isEmpty()?"按东南大学2015—2026相应题型风格设计。":c.truthRelation,12,MUTED,false);margin(rel,0,7,0,0);truth.addView(rel);
        }
        inner.addView(truth);
        TextView src=tv("题目来源："+c.sourceField,11,MUTED,false);src.setBackground(shape(Color.rgb(246,249,248),12));src.setPadding(dp(10),dp(9),dp(10),dp(9));margin(src,0,9,0,0);inner.addView(src);
    }

    void showExamAnswer(JSONObject record,JSONObject ans){
        ScrollView sv=new ScrollView(this);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(17),dp(10),dp(17),dp(12));
        TextView verified=tv("✓  已核验参考答案",12,TEAL_DARK,true);verified.setBackground(shape(TEAL_SOFT,14));verified.setPadding(dp(10),dp(6),dp(10),dp(6));wrap.addView(verified);
        TextView q=tv(record.optString("question"),16,INK,true);q.setBackground(shape(Color.rgb(247,249,248),13));q.setPadding(dp(12),dp(10),dp(12),dp(10));margin(q,0,10,0,12);wrap.addView(q);
        TextView at=tv("参考答案",14,TEAL_DARK,true);wrap.addView(at);
        TextView body=tv("",14,INK,false);body.setText(richAnswer(ans.optString("answer")));body.setLineSpacing(dp(2),1.17f);margin(body,0,7,0,12);wrap.addView(body);
        String source=ans.optString("source");if(!source.isEmpty()){TextView src=tv("核验依据："+source,11,MUTED,false);src.setBackground(shape(Color.rgb(248,250,249),12));src.setPadding(dp(10),dp(9),dp(10),dp(9));wrap.addView(src);}
        TextView note=tv("说明：这是依据教材、真题和已核验题库整理的参考答案，不宣称为东南大学官方标准答案。若来源或题干尚未达到核验门槛，App不会显示答案按钮。",11,ORANGE,false);note.setBackground(shape(ORANGE_SOFT,12));note.setPadding(dp(10),dp(9),dp(10),dp(9));margin(note,0,9,0,0);wrap.addView(note);
        sv.addView(wrap);String title=record.optString("yearLabel")+" · "+record.optString("subject")+" · "+record.optString("type");
        new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("关闭",null).show();
    }

    void renderRelatedKnowledge(LinearLayout inner,Card c){
        List<Card> related=repo.relatedCards(c);
        if(related.isEmpty()){inner.addView(tv("母版未给出可跳转的关联问题。",14,MUTED,false));return;}
        TextView h1=tv("⌘  母版关联问题",14,INK,true);inner.addView(h1);
        int[] cs={Color.rgb(239,110,93),Color.rgb(232,164,50),Color.rgb(117,96,201),Color.rgb(71,137,211),Color.rgb(57,166,126)};
        for(int i=0;i<related.size();i++){
            Card x=related.get(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(9),dp(8),dp(8),dp(8));row.setBackground(shape(Color.rgb(248,250,249),11));
            TextView icon=tv(String.valueOf(i+1),10,Color.WHITE,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(cs[i%cs.length],9));row.addView(icon,new LinearLayout.LayoutParams(dp(24),dp(24)));
            LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(9),0,dp(6),0);tx.addView(tv(x.id+" · "+repo.questionType(x),11,MUTED,true));TextView q=tv(x.question,13,INK,true);q.setMaxLines(2);tx.addView(q);row.addView(tx,new LinearLayout.LayoutParams(0,-2,1));row.addView(tv("›",21,MUTED,false));
            row.setOnClickListener(v->showRelatedCardPreview(x.id));margin(row,0,5,0,0);inner.addView(row);
        }
        TextView note=tv("关联关系直接读取母版“关联问题”字段。点击后在当前学习序列上方展开，不会离开本轮学习。",11,MUTED,false);note.setBackground(shape(Color.rgb(246,249,248),12));note.setPadding(dp(10),dp(9),dp(10),dp(9));margin(note,0,9,0,0);inner.addView(note);
    }

    void showRelatedCardPreview(String id){
        Card x=repo.byId(id);if(x==null){Toast.makeText(this,"关联卡片不存在",Toast.LENGTH_SHORT).show();return;}
        final Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(BG);applySafeInsets(page);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(dp(14),dp(8),dp(14),dp(6));
        ImageView back=tapIcon(R.drawable.ic_arrow_back,22,INK,v->dialog.dismiss());head.addView(back,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(dp(8),0,dp(8),0);titles.addView(tv("相关知识点",17,INK,true));titles.addView(tv(x.id+" · "+repo.questionType(x)+" · "+x.secondaryTopic,11,MUTED,false));head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView close=tv("返回原序列",12,TEAL_DARK,true);close.setPadding(dp(8),dp(8),dp(4),dp(8));close.setOnClickListener(v->dialog.dismiss());head.addView(close);page.addView(head);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(10),dp(18),dp(22));
        TextView q=tv(x.question,20,INK,true);q.setBackground(shape(Color.WHITE,16));q.setPadding(dp(14),dp(12),dp(14),dp(12));content.addView(q);
        TextView meta=tv(repo.examSourceLabel(x)+" · 考频"+mockEngine.effectiveFrequency(x)+" · "+x.primaryTopic+" / "+x.secondaryTopic,11,MUTED,false);margin(meta,0,8,0,12);content.addView(meta);
        TextView aTitle=tv("完整解答",13,TEAL_DARK,true);content.addView(aTitle);
        TextView answer=tv("",16,INK,false);answer.setText(richAnswer(repo.fullAnswer(x)));answer.setLineSpacing(dp(3),1.20f);answer.setTextIsSelectable(true);margin(answer,0,7,0,12);content.addView(answer);
        TextView source=tv("核心教材依据  "+x.origin,11,MUTED,false);source.setBackground(shape(Color.rgb(248,250,249),12));source.setPadding(dp(11),dp(9),dp(11),dp(9));content.addView(source);
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout rates=new LinearLayout(this);rates.setPadding(dp(16),dp(7),dp(16),dp(12));rates.setOrientation(LinearLayout.HORIZONTAL);
        View again=rateAction(R.drawable.ic_repeat,"再看看","记录为不清楚",ORANGE_SOFT,ORANGE,v->{engine.rate(x,false);Toast.makeText(this,"已记录：不清楚",Toast.LENGTH_SHORT).show();dialog.dismiss();});
        View know=rateAction(R.drawable.ic_check,"记住了","记录为熟悉",TEAL_SOFT,TEAL_DARK,v->{engine.rate(x,true);Toast.makeText(this,"已记录：熟悉",Toast.LENGTH_SHORT).show();dialog.dismiss();});
        rates.addView(again,new LinearLayout.LayoutParams(0,dp(56),1));rates.addView(gap(8));rates.addView(know,new LinearLayout.LayoutParams(0,dp(56),1));page.addView(rates);
        dialog.setContentView(page);dialog.show();
    }

    void showCardMenu(Card c,View anchor){
        PopupMenu popup=new PopupMenu(this,anchor);Menu menu=popup.getMenu();
        menu.add(0,1,0,store.isFavorite(c.id)?"★ 取消收藏":"☆ 添加收藏");
        menu.add(0,2,1,"✎ 本地修正");
        menu.add(0,3,2,"⚑ 内容反馈");
        boolean hasOverride=feedback.hasCardOverride(c.id)||feedback.hasDetailOverride(c.id);
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
        new AlertDialog.Builder(this).setTitle("恢复官方内容？").setMessage("将删除这张卡的本地修正；如果本卡修改过完整答案，也会恢复为随安装包提供的官方版本。学习记录、收藏和最近10次记录不会受影响。")
            .setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{
                List<String> ids=new ArrayList<>();for(Card x:queue)ids.add(x.id);
                feedback.clearCardOverride(c.id);feedback.clearDetailOverride(c.id);
                try{
                    repo=new CardRepository(this,feedback);mockEngine=new MockExamEngine(repo,store,engine);
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
    void rate(Card c,boolean ok){engine.rate(c,ok);qIndex++;revealed=false;answerTab=0;store.saveSession(queue,qIndex);store.saveSessionMeta(mockMode?"mock":"study",mockMode?mockSubject:"");showStudy();}

    static class SearchHit {
        Card card; int score; String reason;
        SearchHit(Card card,int score,String reason){this.card=card;this.score=score;this.reason=reason;}
    }


void showLibrary(){
    currentScreen="library";categoryOpenedFromStudy=false;activePrimaryTopic="";
    shell("卡片库",repo.cards.size()+" 张卡片 · "+primaryTopics().size()+" 个一级专题 · "+secondaryTopicCount()+" 个二级专题","卡片库");

    List<Card> examCards=realExamCards(),relatedCards=examRelatedKnowledgeCards();
    View priorityTitle=sectionHeader("优先学习","真题 → 关联知识点",null);margin(priorityTitle,2,0,0,8);body.addView(priorityTitle);
    View examEntry=priorityBankRow("真题题库","先刷历年真题与回忆主题，建立命题感",examCards.size(),ORANGE,v->showBankCards("真题题库",realExamCards(),"历年真题与回忆主题","examBank"));body.addView(examEntry);margin(examEntry,0,0,0,7);
    View relatedEntry=priorityBankRow("真题关联知识点","由真题关联问题汇总的小库，按关联强度优先",relatedCards.size(),TEAL_DARK,v->showBankCards("真题关联知识点",examRelatedKnowledgeCards(),"由真题的关联问题自动汇总","relatedBank"));body.addView(relatedEntry);margin(relatedEntry,0,0,0,14);
    View systemTitle=sectionHeader("教材系统题库","按教材框架与专题刷",null);margin(systemTitle,2,0,0,8);body.addView(systemTitle);
    View frameworkTrial=priorityBankRow("教材框架 · 社会工作服务","要素 → 目标 → 功能 · 2497 张卡建立多对多知识链接",frameworkUniqueLinkedCount(),Color.rgb(112,100,220),v->showServiceFramework());body.addView(frameworkTrial);margin(frameworkTrial,0,0,0,12);

    final String[] mode={"全部"};final TextView[] filterViews=new TextView[4];final int[] searchLimit={60};String[] labels={"全部","已学习","未学习","收藏"};

    LinearLayout searchBox=box(SURFACE,10,18);searchBox.setBackground(strokeShape(SURFACE,18,LINE));
    LinearLayout searchRow=new LinearLayout(this);searchRow.setGravity(Gravity.CENTER_VERTICAL);
    searchRow.addView(iconView(R.drawable.ic_search,20,MUTED),new LinearLayout.LayoutParams(dp(34),dp(44)));
    EditText search=new EditText(this);search.setHint("搜索题目、知识点或答案");search.setSingleLine(true);search.setTextSize(14);search.setTextColor(INK);search.setHintTextColor(Color.rgb(155,166,162));search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(dp(2),0,dp(8),0);searchRow.addView(search,new LinearLayout.LayoutParams(0,dp(48),1));
    TextView clear=tv("清除",11,MUTED,true);clear.setGravity(Gravity.CENTER);clear.setVisibility(View.GONE);clear.setOnClickListener(v->search.setText(""));searchRow.addView(clear,new LinearLayout.LayoutParams(dp(48),dp(40)));
    searchBox.addView(searchRow);body.addView(searchBox);

    TextView searchTip=tv("支持多关键词与常用同义词",10,MUTED,false);margin(searchTip,4,5,0,10);body.addView(searchTip);

    LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
    for(int i=0;i<labels.length;i++){final int idx=i;TextView c=chip(labels[i],i==0);filterViews[i]=c;filters.addView(c,new LinearLayout.LayoutParams(0,dp(38),1));if(i<labels.length-1)filters.addView(gap(6));}
    margin(filters,0,0,0,14);body.addView(filters);

    LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);
    final Runnable[] fill=new Runnable[1];fill[0]=()->{
        list.removeAllViews();String key=search.getText().toString().trim();clear.setVisibility(key.isEmpty()?View.GONE:View.VISIBLE);int total=0;
        if(!key.isEmpty()){
            List<SearchHit> matches=new ArrayList<>();
            for(Card c:repo.cards){
                boolean pass="全部".equals(mode[0])||("已学习".equals(mode[0])&&engine.learned(c))||("未学习".equals(mode[0])&&!engine.learned(c))||("收藏".equals(mode[0])&&store.isFavorite(c.id));
                if(!pass)continue;SearchHit hit=searchHit(c,key);if(hit!=null){matches.add(hit);total++;}
            }
            Collections.sort(matches,(a,b)->{int d=Integer.compare(b.score,a.score);if(d!=0)return d;return a.card.question.compareTo(b.card.question);});
            TextView resultTitle=tv(total==0?"没有找到相关卡片":"找到 "+total+" 张相关卡片",13,total==0?MUTED:TEAL_DARK,true);margin(resultTitle,2,0,0,9);list.addView(resultTitle);
            int shown=Math.min(searchLimit[0],matches.size());for(int i=0;i<shown;i++)list.addView(searchCardRow(matches.get(i),key));
            if(shown<matches.size()){Button more=btn("继续加载（剩余 "+(matches.size()-shown)+" 张）",Color.rgb(246,249,248),TEAL_DARK);more.setTextSize(12);more.setOnClickListener(v->{searchLimit[0]=Math.min(matches.size(),searchLimit[0]+60);fill[0].run();});list.addView(more,new LinearLayout.LayoutParams(-1,dp(46)));}return;
        }

        LinkedHashMap<String,List<Card>> allTopics=primaryTopics();
        for(Map.Entry<String,List<String>> group:topicGroups().entrySet()){
            boolean headerAdded=false;
            for(String primary:group.getValue()){
                List<Card> source=allTopics.get(primary);if(source==null)continue;List<Card> filtered=new ArrayList<>();
                for(Card c:source){
                    boolean pass="全部".equals(mode[0])||("已学习".equals(mode[0])&&engine.learned(c))||("未学习".equals(mode[0])&&!engine.learned(c))||("收藏".equals(mode[0])&&store.isFavorite(c.id));
                    if(pass){filtered.add(c);total++;}
                }
                if(!filtered.isEmpty()){
                    if(!headerAdded){View section=sectionHeader(group.getKey(),null,null);margin(section,2,list.getChildCount()==0?0:10,0,9);list.addView(section);headerAdded=true;}
                    list.addView(topicRow(primary,filtered));
                }
            }
        }
        if(total==0){TextView empty=tv("没有符合条件的卡片",14,MUTED,false);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(32),0,dp(32));list.addView(empty);}
    };
    for(int i=0;i<filterViews.length;i++){final int idx=i;filterViews[i].setOnClickListener(v->{mode[0]=idx==0?"全部":idx==1?"已学习":idx==2?"未学习":"收藏";searchLimit[0]=60;for(int j=0;j<filterViews.length;j++){filterViews[j].setBackground(j==idx?strokeShape(TEAL_SOFT,14,Color.rgb(190,229,220)):shape(Color.rgb(246,248,247),14));filterViews[j].setTextColor(j==idx?TEAL_DARK:MUTED);filterViews[j].setTypeface(Typeface.DEFAULT,j==idx?Typeface.BOLD:Typeface.NORMAL);}fill[0].run();});}
    fill[0].run();search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){searchLimit[0]=60;fill[0].run();}public void afterTextChanged(android.text.Editable e){}});
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
        int total=0;String bestReason="";int bestField=0;

        for(String token:tokens){
            List<String> vars=tokenVariants(token);
            int tokenScore=0;String reason="";

            int m=containsAny(c.question,vars);if(m>0){tokenScore=Math.max(tokenScore,120+m);reason="题目";}
            m=containsAny(c.topic,vars);if(m>0&&90+m>tokenScore){tokenScore=90+m;reason="知识点";}
            m=containsAny(c.primaryTopic,vars);if(m>0&&86+m>tokenScore){tokenScore=86+m;reason="一级专题";}
            m=containsAny(c.secondaryTopic,vars);if(m>0&&82+m>tokenScore){tokenScore=82+m;reason="二级专题";}
            m=containsAny(c.kind,vars);if(m>0&&45+m>tokenScore){tokenScore=45+m;reason="卡片类型";}
            for(String b:c.bullets){m=containsAny(b,vars);if(m>0&&70+m>tokenScore){tokenScore=70+m;reason="答案要点";}}
            m=containsAny(c.tip,vars);if(m>0&&38+m>tokenScore){tokenScore=38+m;reason="记忆提示";}
            m=containsAny(c.origin,vars);if(m>0&&24+m>tokenScore){tokenScore=24+m;reason="出处";}
            m=containsAny(repo.fullAnswer(c),vars);if(m>0&&58+m>tokenScore){tokenScore=58+m;reason="完整答案";}

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
        TextView topic=tv("",11,MUTED,false);topic.setText(highlightMatches(c.id+" · "+c.primaryTopic+" · "+c.secondaryTopic+" · "+c.kind+" · "+state+(store.isFavorite(c.id)?" · ★ 收藏":""),key));margin(topic,0,4,0,0);text.addView(topic);
        TextView reason=tv("匹配："+hit.reason,10,TEAL_DARK,true);margin(reason,0,4,0,0);text.addView(reason);

        row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=tv("›",24,MUTED,false);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(44)));
        item.addView(row);
        item.setOnClickListener(v->{activePrimaryTopic="";showCategoryCards(c.id,new ArrayList<>(Collections.singletonList(c)));});
        margin(item,0,0,0,7);
        return item;
    }


    List<Card> cardsFromIds(List<String> ids){
        List<Card> out=new ArrayList<>();if(ids==null)return out;for(String id:ids){Card c=repo.byId(id);if(c!=null)out.add(c);}return out;
    }
    void setActiveList(String name,List<Card> cards){
        activeListName=name==null?"":name;activeListIds.clear();if(cards!=null)for(Card c:cards)activeListIds.add(c.id);
    }
    void clearStudyReturn(){studyReturnScreen="";studyReturnName="";studyReturnPrimary="";studyReturnIds.clear();studyReturnFrameworkNodeId="";studyReturnFrameworkCoreOnly=false;}
    void rememberStudyReturn(String screen,String name,String primary,List<Card> cards){
        studyReturnScreen=screen==null?"":screen;studyReturnName=name==null?"":name;studyReturnPrimary=primary==null?"":primary;studyReturnIds.clear();if(cards!=null)for(Card c:cards)studyReturnIds.add(c.id);
        // Every new study origin starts from a clean route-specific state.
        studyReturnFrameworkNodeId="";studyReturnFrameworkCoreOnly=false;
    }
    void rememberCurrentAsStudyReturn(){
        if("primary".equals(currentScreen)){
            List<Card> cards=primaryTopics().get(activePrimaryTopic);rememberStudyReturn("primary",activePrimaryTopic,activePrimaryTopic,cards);
        }else if("secondary".equals(currentScreen)||"category".equals(currentScreen)||"examBank".equals(currentScreen)||"relatedBank".equals(currentScreen)||"frameworkBank".equals(currentScreen)){
            rememberStudyReturn(currentScreen,activeListName,activePrimaryTopic,cardsFromIds(activeListIds));
            if("frameworkBank".equals(currentScreen)){studyReturnFrameworkNodeId=currentFrameworkNodeId;studyReturnFrameworkCoreOnly=currentFrameworkCoreOnly;}
        }else if("frameworkTree".equals(currentScreen)){
            rememberStudyReturn("frameworkTree","教材框架", "",null);studyReturnFrameworkNodeId=currentFrameworkNodeId;
        }else if("framework".equals(currentScreen)){
            rememberStudyReturn("framework","教材框架 · 社会工作服务","",null);
        }else if("stats".equals(currentScreen)){
            rememberStudyReturn("stats","学习统计","",null);
        }else if("library".equals(currentScreen)) rememberStudyReturn("library","","",null);
        else clearStudyReturn();
    }
    boolean hasStudyReturn(){return studyReturnScreen!=null&&!studyReturnScreen.isEmpty();}
    void returnToStudyOrigin(){
        String screen=studyReturnScreen,name=studyReturnName,primary=studyReturnPrimary;List<Card> cards=cardsFromIds(studyReturnIds);
        if("primary".equals(screen)){clearStudyReturn();List<Card> p=primaryTopics().get(primary);if(p!=null){showPrimaryTopic(primary,p);return;}}
        if("secondary".equals(screen)||"category".equals(screen)){clearStudyReturn();activePrimaryTopic=primary;showCategoryCards(name,cards);return;}
        if("examBank".equals(screen)){clearStudyReturn();showBankCards("真题题库",realExamCards(),"历年真题与回忆主题","examBank");return;}
        if("relatedBank".equals(screen)){clearStudyReturn();showBankCards("真题关联知识点",examRelatedKnowledgeCards(),"由真题的关联问题自动汇总","relatedBank");return;}
        if("frameworkBank".equals(screen)){String nodeId=studyReturnFrameworkNodeId;boolean coreOnly=studyReturnFrameworkCoreOnly;clearStudyReturn();showFrameworkCards(nodeId,coreOnly);return;}
        if("frameworkTree".equals(screen)){String nodeId=studyReturnFrameworkNodeId;clearStudyReturn();if(nodeId==null||nodeId.isEmpty())showServiceFramework();else showFrameworkTreeNode(nodeId);return;}
        if("framework".equals(screen)){clearStudyReturn();showServiceFramework();return;}
        if("stats".equals(screen)){clearStudyReturn();showStats();return;}
        if("library".equals(screen)){clearStudyReturn();showLibrary();return;}
        clearStudyReturn();showHome();
    }
    void startStudySequence(List<Card> cards,Card start){
        if(cards==null||cards.isEmpty()){Toast.makeText(this,"这个板块暂时没有卡片",Toast.LENGTH_SHORT).show();return;}
        rememberCurrentAsStudyReturn();mockMode=false;mockSubject="";mockPaper=null;
        int startIndex=0;if(start!=null){int i=cards.indexOf(start);if(i>=0)startIndex=i;}
        queue=new ArrayList<>();for(int i=startIndex;i<cards.size();i++)queue.add(cards.get(i));
        qIndex=0;revealed=false;answerTab=0;store.saveSession(queue,0);store.saveSessionMeta("study","");showStudy();
    }
    List<Card> cardsByIdRange(int from,int to){List<Card> out=new ArrayList<>();for(int i=from;i<=to;i++){Card c=repo.byId(String.format(Locale.ROOT,"C%04d",i));if(c!=null)out.add(c);}return out;}

    String readAssetText(String name){try{java.io.InputStream in=getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();return out.toString("UTF-8");}catch(Exception e){return "";}}
    JSONObject quickMemory(){
        if(quickMemoryCache!=null)return quickMemoryCache;
        try{quickMemoryCache=new JSONObject(readAssetText("quick_memory_v2_8_8.json"));}catch(Exception e){quickMemoryCache=new JSONObject();}
        return quickMemoryCache;
    }
    JSONObject quickMemoryFor(Card c){
        if(c==null)return new JSONObject();
        JSONObject x=quickMemory().optJSONObject(c.id);return x==null?new JSONObject():x;
    }
    JSONObject serviceFramework(){
        if(serviceFrameworkCache!=null)return serviceFrameworkCache;
        try{serviceFrameworkCache=new JSONObject(readAssetText("service_framework_v2_8_5.json"));}catch(Exception e){serviceFrameworkCache=new JSONObject();}
        return serviceFrameworkCache;
    }
    int frameworkUniqueLinkedCount(){return serviceFramework().optInt("uniqueLinkedCards",repo.cards.size());}
    JSONObject frameworkNodeById(String id){return findFrameworkNode(serviceFramework().optJSONArray("branches"),id);}
    JSONObject findFrameworkNode(JSONArray a,String id){if(a==null||id==null)return null;for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n==null)continue;if(id.equals(n.optString("id")))return n;JSONObject x=findFrameworkNode(n.optJSONArray("children"),id);if(x!=null)return x;}return null;}
    String frameworkParentId(String childId){return findFrameworkParent(serviceFramework().optJSONArray("branches"),childId,"");}
    String findFrameworkParent(JSONArray a,String childId,String parent){if(a==null)return "";for(int i=0;i<a.length();i++){JSONObject n=a.optJSONObject(i);if(n==null)continue;if(childId.equals(n.optString("id")))return parent;String x=findFrameworkParent(n.optJSONArray("children"),childId,n.optString("id"));if(!x.isEmpty())return x;}return "";}
    ArrayList<String> jsonStringList(JSONArray a){ArrayList<String> out=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){String x=a.optString(i);if(!x.isEmpty())out.add(x);}return out;}
    List<Card> frameworkCards(JSONObject node,boolean coreOnly){
        LinkedHashSet<String> ids=new LinkedHashSet<>();if(node==null)return new ArrayList<>();ids.addAll(jsonStringList(node.optJSONArray("coreIds")));if(!coreOnly)ids.addAll(jsonStringList(node.optJSONArray("relatedIds")));
        return cardsFromIds(new ArrayList<>(ids));
    }
    View frameworkNodeRow(JSONObject node,int depth){
        int core=node.optInt("coreCount",0),rel=node.optInt("relatedCount",0),total=node.optInt("totalCount",core+rel);boolean hasChildren=node.optJSONArray("children")!=null&&node.optJSONArray("children").length()>0;
        LinearLayout row=box(SURFACE,12,15);row.setBackground(strokeShape(SURFACE,15,LINE));row.setPadding(dp(12+depth*8),dp(10),dp(10),dp(10));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);TextView title=tv(node.optString("title"),14,INK,true);title.setMaxLines(2);tx.addView(title);TextView meta=tv("核心 "+core+" · 拓展 "+rel+" · 共 "+total+" 张",10,MUTED,false);margin(meta,0,3,0,0);tx.addView(meta);line.addView(tx,new LinearLayout.LayoutParams(0,-2,1));line.addView(tv("›",24,MUTED,false),new LinearLayout.LayoutParams(dp(28),dp(44)));row.addView(line);
        row.setOnClickListener(v->{if(hasChildren)showFrameworkTreeNode(node.optString("id"));else showFrameworkCards(node.optString("id"),false);});margin(row,0,0,0,7);return row;
    }
    void addFrameworkChildren(LinearLayout target,JSONArray children){if(children==null)return;for(int i=0;i<children.length();i++){JSONObject n=children.optJSONObject(i);if(n!=null)target.addView(frameworkNodeRow(n,0));}}
    void showServiceFramework(){
        currentScreen="framework";currentFrameworkNodeId="";currentFrameworkCoreOnly=false;shell("教材框架 · 社会工作服务","要素 → 目标 → 功能 · "+frameworkUniqueLinkedCount()+" / "+repo.cards.size()+" 张已建立链接","卡片库");
        LinearLayout note=box(Color.rgb(248,250,249),12,14);note.addView(tv("这不是23张卡的章节试点，而是覆盖完整题库的知识关系网。框架节点以教材与思维导图为准；外部资料只用于扩展“某张卡与哪个节点相关”，不改写题目和答案。",12,MUTED,false));margin(note,0,0,0,12);body.addView(note);
        JSONObject fw=serviceFramework();JSONArray branches=fw.optJSONArray("branches");if(branches==null){body.addView(tv("框架数据加载失败",14,RED,true));return;}
        LinearLayout all=box(PURPLE_SOFT,14,16);LinearLayout ar=new LinearLayout(this);ar.setGravity(Gravity.CENTER_VERTICAL);LinearLayout at=new LinearLayout(this);at.setOrientation(LinearLayout.VERTICAL);at.addView(tv("全框架学习",16,INK,true));at.addView(tv("2497张卡至少关联一个框架节点；同一卡可同时出现在要素、目标和功能中。",11,MUTED,false));ar.addView(at,new LinearLayout.LayoutParams(0,-2,1));Button start=btn("开始",Color.rgb(112,100,220),Color.WHITE);start.setTextSize(12);start.setOnClickListener(v->startStudySequence(new ArrayList<>(repo.cards),null));ar.addView(start,new LinearLayout.LayoutParams(dp(86),dp(42)));all.addView(ar);margin(all,0,0,0,14);body.addView(all);
        for(int i=0;i<branches.length();i++){JSONObject n=branches.optJSONObject(i);if(n!=null)body.addView(frameworkNodeRow(n,0));}
    }
    void showFrameworkTreeNode(String nodeId){
        JSONObject node=frameworkNodeById(nodeId);if(node==null){showServiceFramework();return;}currentScreen="frameworkTree";currentFrameworkNodeId=nodeId;currentFrameworkCoreOnly=false;
        String parent=frameworkParentId(nodeId);String crumb=parent.isEmpty()?"教材框架":"上一级";shell(node.optString("title"),node.optInt("totalCount")+" 张关联卡 · 核心 "+node.optInt("coreCount")+" · 拓展 "+node.optInt("relatedCount"),crumb);
        String noteText=node.optString("note");if(!noteText.isEmpty()){TextView note=tv(noteText,11,MUTED,false);note.setBackground(shape(Color.rgb(248,250,249),12));note.setPadding(dp(10),dp(8),dp(10),dp(8));margin(note,0,0,0,10);body.addView(note);}
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button core=btn("核心考点 "+node.optInt("coreCount"),TEAL_SOFT,TEAL_DARK);core.setTextSize(11);core.setOnClickListener(v->showFrameworkCards(nodeId,true));Button all=btn("全部关联 "+node.optInt("totalCount"),Color.rgb(112,100,220),Color.WHITE);all.setTextSize(11);all.setOnClickListener(v->showFrameworkCards(nodeId,false));actions.addView(core,new LinearLayout.LayoutParams(0,dp(44),1));actions.addView(gap(8));actions.addView(all,new LinearLayout.LayoutParams(0,dp(44),1));margin(actions,0,0,0,12);body.addView(actions);
        JSONArray children=node.optJSONArray("children");if(children!=null&&children.length()>0){body.addView(sectionHeader("下一级知识节点","",null));margin(body.getChildAt(body.getChildCount()-1),0,5,0,8);addFrameworkChildren(body,children);}else{TextView hint=tv("这是末级知识节点。可直接进入“核心考点”或“全部关联”刷题。",11,MUTED,false);body.addView(hint);}
    }
    void showFrameworkCards(String nodeId,boolean coreOnly){
        JSONObject node=frameworkNodeById(nodeId);if(node==null){showServiceFramework();return;}currentFrameworkNodeId=nodeId;currentFrameworkCoreOnly=coreOnly;List<Card> cards=frameworkCards(node,coreOnly);showBankCards(node.optString("title"),cards,coreOnly?"框架核心考点":"框架核心 + 应用拓展","frameworkBank");
    }

    List<Card> realExamCards(){List<Card> out=new ArrayList<>();for(Card c:repo.cards)if(repo.isRealExam(c))out.add(c);return out;}
    void ensureExamRelationCounts(){if(!examRelationCounts.isEmpty())return;for(Card c:repo.cards)if(repo.isRealExam(c))for(String id:c.relatedIds)examRelationCounts.put(id,examRelationCounts.getOrDefault(id,0)+1);}
    int relatedExamCount(Card target){if(target==null)return 0;ensureExamRelationCounts();return examRelationCounts.getOrDefault(target.id,0);}
    List<Card> examRelatedKnowledgeCards(){
        ensureExamRelationCounts();LinkedHashSet<String> ids=new LinkedHashSet<>();for(Card c:repo.cards)if(repo.isRealExam(c))for(String id:c.relatedIds){Card x=repo.byId(id);if(x!=null&&!repo.isRealExam(x))ids.add(id);}List<Card> out=cardsFromIds(new ArrayList<>(ids));
        out.sort((a,b)->{int d=Integer.compare(examRelationCounts.getOrDefault(b.id,0),examRelationCounts.getOrDefault(a.id,0));if(d!=0)return d;String fa=repo.systemFrequency(a),fb=repo.systemFrequency(b);int ra="高".equals(fa)?2:"中".equals(fa)?1:0,rb="高".equals(fb)?2:"中".equals(fb)?1:0;d=Integer.compare(rb,ra);return d!=0?d:a.id.compareTo(b.id);});return out;
    }

    int favoriteCount(){int n=0;for(Card c:repo.cards)if(store.isFavorite(c.id))n++;return n;}

    View priorityBankRow(String title,String desc,int count,int accent,View.OnClickListener click){
        LinearLayout row=box(SURFACE,13,18);row.setBackground(strokeShape(SURFACE,18,LINE));row.setOnClickListener(click);
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=tv(String.valueOf(count),12,accent,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(Color.rgb(246,249,248),13));line.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(46)));
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(11),0,dp(6),0);tx.addView(tv(title,16,INK,true));TextView d=tv(desc,11,MUTED,false);d.setMaxLines(2);tx.addView(d);line.addView(tx,new LinearLayout.LayoutParams(0,-2,1));line.addView(tv("›",24,MUTED,false),new LinearLayout.LayoutParams(dp(28),dp(44)));row.addView(line);return row;
    }

    View bankCardRow(Card c,List<Card> cards,boolean relatedBank){
        LinearLayout item=box(SURFACE,12,16);item.setBackground(strokeShape(SURFACE,16,LINE));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);
        TextView q=tv(c.question,14,INK,true);q.setMaxLines(3);tx.addView(q);
        String meta=c.id+" · "+repo.questionType(c)+" · "+repo.examSourceLabel(c)+" · "+c.secondaryTopic;
        if(relatedBank){int n=relatedExamCount(c);meta=c.id+" · 关联真题 "+n+" 道 · 考频"+mockEngine.effectiveFrequency(c)+" · "+c.secondaryTopic;}
        TextView m=tv(meta,10,MUTED,false);margin(m,0,4,0,0);tx.addView(m);line.addView(tx,new LinearLayout.LayoutParams(0,-2,1));
        TextView state=tv(engine.learned(c)?store.state(c.id).tag:"未学习",10,engine.learned(c)?TEAL_DARK:ORANGE,true);state.setBackground(shape(engine.learned(c)?TEAL_SOFT:ORANGE_SOFT,11));state.setPadding(dp(7),dp(3),dp(7),dp(3));line.addView(state);item.addView(line);
        item.setOnClickListener(v->startStudySequence(cards,c));margin(item,0,0,0,7);return item;
    }

    void showBankCards(String title,List<Card> cards,String subtitle,String screen){
        currentScreen=screen;categoryOpenedFromStudy=false;activePrimaryTopic="";setActiveList(title,cards);shell(title,cards.size()+" 张 · "+subtitle,"卡片库");
        int learned=0;for(Card c:cards)if(engine.learned(c))learned++;
        LinearLayout summary=box(Color.rgb(240,248,245),14,18);LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout st=new LinearLayout(this);st.setOrientation(LinearLayout.VERTICAL);st.addView(tv("独立学习队列",14,TEAL_DARK,true));st.addView(tv("已学习 "+learned+" / "+cards.size(),12,MUTED,false));sr.addView(st,new LinearLayout.LayoutParams(0,-2,1));Button begin=btn(learned==0?"从头开始":"继续刷题",TEAL,Color.WHITE);begin.setTextSize(12);begin.setOnClickListener(v->openCategory(title,cards));sr.addView(begin,new LinearLayout.LayoutParams(dp(104),dp(42)));summary.addView(sr);body.addView(summary);
        TextView note=tv("这里与2497张教材系统题库使用同一份卡片内容，但入口、学习序列和返回路径独立，避免维护两份答案。",11,MUTED,false);note.setBackground(shape(Color.rgb(248,250,249),12));note.setPadding(dp(10),dp(8),dp(10),dp(8));margin(note,0,8,0,12);body.addView(note);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);final int[] limit={60};final Runnable[] render=new Runnable[1];render[0]=()->{list.removeAllViews();int n=Math.min(limit[0],cards.size());boolean related="relatedBank".equals(screen);for(int i=0;i<n;i++)list.addView(bankCardRow(cards.get(i),cards,related));if(n<cards.size()){Button more=btn("继续加载（剩余 "+(cards.size()-n)+" 张）",Color.rgb(246,249,248),TEAL_DARK);more.setTextSize(12);more.setOnClickListener(v->{limit[0]=Math.min(cards.size(),limit[0]+60);render[0].run();});list.addView(more,new LinearLayout.LayoutParams(-1,dp(46)));}};render[0].run();
    }

View topicRow(String name,List<Card> cards){
    LinearLayout row=box(SURFACE,13,18);row.setBackground(strokeShape(SURFACE,18,LINE));int learned=0,mast=0,fam=0,fuz=0;for(Card c:cards){String tag=store.state(c.id).tag;if(!tag.equals("未学习"))learned++;if(tag.equals("掌握"))mast++;else if(tag.equals("熟悉"))fam++;else if(tag.equals("模糊"))fuz++;}
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout iconBox=new LinearLayout(this);iconBox.setGravity(Gravity.CENTER);iconBox.setBackground(shape(categorySoft(name),14));iconBox.addView(iconView(categoryIconRes(name),22,categoryColor(name)),new LinearLayout.LayoutParams(dp(24),dp(24)));top.addView(iconBox,new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.setPadding(dp(11),0,0,0);TextView title=tv(name,16,INK,true);title.setMaxLines(2);txt.addView(title);txt.addView(tv("已学习 "+learned+" / "+cards.size(),11,MUTED,false));top.addView(txt,new LinearLayout.LayoutParams(0,-2,1));
    String st=fuz>Math.max(2,learned/4)?"模糊较多":learned==0?"待学习":learned==cards.size()?"已学习":"继续学习";TextView status=tv(st,10,st.equals("模糊较多")?ORANGE:TEAL_DARK,true);status.setBackground(shape(st.equals("模糊较多")?ORANGE_SOFT:TEAL_SOFT,12));status.setPadding(dp(8),dp(4),dp(8),dp(4));top.addView(status);row.addView(top);
    ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setMax(Math.max(1,cards.size()));pb.setProgress(learned);pb.getProgressDrawable().setTint(TEAL);margin(pb,dp(59),7,0,4);row.addView(pb,new LinearLayout.LayoutParams(-1,dp(5)));
    String summary=learned==0?"尚未开始 · 共 "+cards.size()+" 张":"掌握 "+mast+"   熟悉 "+fam+"   模糊 "+fuz;
    TextView small=tv(summary,10,MUTED,false);small.setPadding(dp(59),0,0,0);row.addView(small);row.setOnClickListener(v->showPrimaryTopic(name,cards));margin(row,0,0,0,8);return row;
}
    void openCategory(String name,List<Card> cards){
        if(cards==null||cards.isEmpty()){Toast.makeText(this,"这个专题暂时没有卡片",Toast.LENGTH_SHORT).show();return;}
        rememberCurrentAsStudyReturn();mockMode=false;mockSubject="";mockPaper=null;
        int firstUnseen=-1;for(int i=0;i<cards.size();i++){if(engine.unseen(cards.get(i))){firstUnseen=i;break;}}
        int start=firstUnseen>=0?firstUnseen:0;
        queue=new ArrayList<>();for(int i=start;i<cards.size();i++)queue.add(cards.get(i));
        qIndex=0;revealed=false;answerTab=0;store.saveSession(queue,0);store.saveSessionMeta("study","");showStudy();
    }

    LinkedHashMap<String,List<String>> topicGroups(){
        LinkedHashMap<String,List<String>> g=new LinkedHashMap<>();
        g.put("核心知识",Arrays.asList("社会工作基础与发展","社会工作价值与伦理","人类行为与社会环境","社会工作理论"));
        g.put("专业方法",Arrays.asList("通用社会工作实务","个案工作","小组工作","社区工作"));
        g.put("专业体系",Arrays.asList("社会工作行政、组织与督导","社会工作研究、教育与实习","社会政策、福利与保障"));
        g.put("实务领域",Arrays.asList("重点人群社会工作","重点场域社会工作"));
        g.put("综合训练",Arrays.asList("综合应用"));return g;
    }
    LinkedHashMap<String,List<Card>> primaryTopics(){
        LinkedHashMap<String,List<Card>> m=new LinkedHashMap<>();for(List<String> group:topicGroups().values())for(String x:group)m.put(x,new ArrayList<>());
        for(Card c:repo.cards){String p=(c.primaryTopic==null||c.primaryTopic.trim().isEmpty())?"综合应用":c.primaryTopic;if(!m.containsKey(p))m.put(p,new ArrayList<>());m.get(p).add(c);}return m;
    }
    LinkedHashMap<String,List<Card>> secondaryTopics(List<Card> cards){LinkedHashMap<String,List<Card>> m=new LinkedHashMap<>();for(Card c:cards){String x=(c.secondaryTopic==null||c.secondaryTopic.trim().isEmpty())?"其他":c.secondaryTopic;if(!m.containsKey(x))m.put(x,new ArrayList<>());m.get(x).add(c);}return m;}
    int secondaryTopicCount(){LinkedHashSet<String> s=new LinkedHashSet<>();for(Card c:repo.cards)if(c.secondaryTopic!=null&&!c.secondaryTopic.trim().isEmpty())s.add(c.primaryTopic+"\u0001"+c.secondaryTopic);return s.size();}
    int categoryColor(String n){
        if(n==null)return TEAL_DARK;if(n.equals("个案工作"))return Color.rgb(226,139,58);if(n.equals("小组工作"))return Color.rgb(223,95,105);if(n.equals("社区工作"))return TEAL;
        if(n.contains("行政")||n.contains("研究")||n.contains("政策"))return Color.rgb(112,100,220);if(n.contains("人群")||n.contains("场域")||n.contains("实务"))return Color.rgb(36,145,218);return TEAL_DARK;
    }
    int categorySoft(String n){
        if(n==null)return TEAL_SOFT;if(n.equals("个案工作"))return Color.rgb(255,242,226);if(n.equals("小组工作"))return Color.rgb(255,236,239);if(n.equals("社区工作"))return TEAL_SOFT;
        if(n.contains("行政")||n.contains("研究")||n.contains("政策"))return PURPLE_SOFT;if(n.contains("人群")||n.contains("场域")||n.contains("实务"))return BLUE_SOFT;return TEAL_SOFT;
    }


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
        for(int i=0;i<Math.min(10,weak.size());i++){Card x=weak.get(i);LinearLayout wr=box(SURFACE,12,16);wr.setBackground(strokeShape(SURFACE,16,LINE));LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);TextView rank=tv(String.valueOf(i+1),11,i<3?Color.WHITE:MUTED,true);rank.setGravity(Gravity.CENTER);rank.setBackground(shape(i==0?RED:i==1?ORANGE:i==2?Color.rgb(224,164,54):Color.rgb(238,242,240),12));line.addView(rank,new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setPadding(dp(10),0,dp(8),0);TextView qq=tv(x.question,13,INK,true);qq.setMaxLines(2);tx.addView(qq);tx.addView(tv(x.topic,10,MUTED,false));line.addView(tx,new LinearLayout.LayoutParams(0,-2,1));TextView pct=tv(Math.round(engine.difficulty(x)*100)+"%",11,i<3?ORANGE:MUTED,true);line.addView(pct);wr.addView(line);wr.setOnClickListener(v->{List<Card> review=engine.reviewOnlyQueue(Collections.singletonList(x),Math.min(3,store.dailyGoal()));if(review.isEmpty())review=new ArrayList<>(Collections.singletonList(x));startStudySequence(review,null);});margin(wr,0,0,0,7);body.addView(wr);}
    }
}

    View bigMetric(String label,String num,String unit){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);TextView a=tv(label,11,MUTED,false);a.setGravity(Gravity.CENTER);l.addView(a);TextView b=tv(num,25,INK,true);b.setGravity(Gravity.CENTER);l.addView(b);TextView c=tv(unit,10,MUTED,false);c.setGravity(Gravity.CENTER);l.addView(c);return l;}
    View makeWeekChart(){
        Calendar cal=Calendar.getInstance();cal.add(Calendar.DAY_OF_YEAR,-6);int max=1,total=0;int[] vals=new int[7];String[] labs=new String[7];SimpleDateFormat f=new SimpleDateFormat("M/d",Locale.getDefault());
        for(int i=0;i<7;i++){Date d=cal.getTime();vals[i]=store.dayDone(d);labs[i]=f.format(d);max=Math.max(max,vals[i]);total+=vals[i];cal.add(Calendar.DAY_OF_YEAR,1);}
        if(total==0){
            LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(18),0,dp(18));
            TextView title=tv("还没有学习记录",14,INK,true);title.setGravity(Gravity.CENTER);empty.addView(title);
            TextView note=tv("完成第一组卡片后，这里会显示近 7 天学习节奏。",11,MUTED,false);note.setGravity(Gravity.CENTER);margin(note,0,5,0,0);empty.addView(note);return empty;
        }
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setGravity(Gravity.BOTTOM);
        for(int i=0;i<7;i++){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);TextView val=tv(String.valueOf(vals[i]),10,MUTED,true);val.setGravity(Gravity.CENTER);col.addView(val);View bar=new View(this);bar.setBackground(shape(i==6?TEAL:Color.rgb(151,222,205),8));int h=dp(10+(int)(62.0*vals[i]/max));col.addView(bar,new LinearLayout.LayoutParams(dp(24),h));TextView lab=tv(labs[i],9,MUTED,false);lab.setGravity(Gravity.CENTER);col.addView(lab);outer.addView(col,new LinearLayout.LayoutParams(0,dp(112),1));}return outer;
    }


void showSettings(){
    currentScreen="settings";categoryOpenedFromStudy=false;
    shell("我的","学习偏好、数据与内容管理。","我的");

    View learnTitle=sectionHeader("学习设置",null,null);margin(learnTitle,2,4,0,8);body.addView(learnTitle);
    LinearLayout goalCard=box(SURFACE,15,18);goalCard.setBackground(strokeShape(SURFACE,18,LINE));
    LinearLayout goalTop=new LinearLayout(this);goalTop.setGravity(Gravity.CENTER_VERTICAL);LinearLayout gt=new LinearLayout(this);gt.setOrientation(LinearLayout.VERTICAL);gt.addView(tv("每日学习量",16,INK,true));gt.addView(tv("每天计划完成的卡片数量",11,MUTED,false));goalTop.addView(gt,new LinearLayout.LayoutParams(0,-2,1));
    EditText goal=new EditText(this);goal.setInputType(2);goal.setText(String.valueOf(store.dailyGoal()));goal.setGravity(Gravity.CENTER);goal.setTextSize(17);goal.setTextColor(INK);goal.setBackground(strokeShape(Color.rgb(249,251,250),13,LINE));goalTop.addView(goal,new LinearLayout.LayoutParams(dp(68),dp(44)));goalTop.addView(gap(8));
    Button save=btn("保存",TEAL,Color.WHITE);save.setTextSize(12);save.setOnClickListener(v->{int g=parse(goal,30);store.setDailyGoal(g);Toast.makeText(this,"已保存为每天 "+store.dailyGoal()+" 张",Toast.LENGTH_SHORT).show();showSettings();});goalTop.addView(save,new LinearLayout.LayoutParams(dp(74),dp(44)));goalCard.addView(goalTop);body.addView(goalCard);

    View dataTitle=sectionHeader("数据与迁移",null,null);margin(dataTitle,2,18,0,8);body.addView(dataTitle);
    LinearLayout data=box(SURFACE,15,18);data.setBackground(strokeShape(SURFACE,18,LINE));data.addView(tv("完整备份",16,INK,true));TextView backupDesc=tv("包含学习进度、最近10次记录、收藏、手动考频、本地修正和内容反馈。安装正式新版前建议先导出。",12,MUTED,false);margin(backupDesc,0,5,0,10);data.addView(backupDesc);LinearLayout dr=new LinearLayout(this);dr.setOrientation(LinearLayout.HORIZONTAL);Button backup=btn("导出备份",TEAL,Color.WHITE),restore=btn("恢复备份",TEAL_SOFT,TEAL_DARK);backup.setOnClickListener(v->exportBackup());restore.setOnClickListener(v->confirmImportBackup());dr.addView(backup,new LinearLayout.LayoutParams(0,dp(46),1));dr.addView(gap(8));dr.addView(restore,new LinearLayout.LayoutParams(0,dp(46),1));data.addView(dr);body.addView(data);

    View contentTitle=sectionHeader("内容管理",null,null);margin(contentTitle,2,18,0,8);body.addView(contentTitle);
    LinearLayout fb=box(SURFACE,15,18);fb.setBackground(strokeShape(SURFACE,18,LINE));LinearLayout fr=new LinearLayout(this);fr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout ft=new LinearLayout(this);ft.setOrientation(LinearLayout.VERTICAL);ft.addView(tv("内容反馈",16,INK,true));ft.addView(tv("待校对 "+feedback.pendingCount()+" 条 · 可导出 JSON 统一修正",11,MUTED,false));fr.addView(ft,new LinearLayout.LayoutParams(0,-2,1));Button export=btn("导出",TEAL_SOFT,TEAL_DARK);export.setOnClickListener(v->exportFeedback());fr.addView(export,new LinearLayout.LayoutParams(dp(76),dp(42)));fb.addView(fr);body.addView(fb);

    LinearLayout diag=box(SURFACE,15,18);diag.setBackground(strokeShape(SURFACE,18,LINE));margin(diag,0,9,0,0);LinearLayout dg=new LinearLayout(this);dg.setGravity(Gravity.CENTER_VERTICAL);LinearLayout dgt=new LinearLayout(this);dgt.setOrientation(LinearLayout.VERTICAL);dgt.addView(tv("稳定性诊断",16,INK,true));dgt.addView(tv(lastCrashTrace().isEmpty()?"暂无已记录的异常退出":"检测到上次异常退出，可查看堆栈信息",11,lastCrashTrace().isEmpty()?MUTED:ORANGE,false));dg.addView(dgt,new LinearLayout.LayoutParams(0,-2,1));Button viewCrash=btn("查看",Color.rgb(247,249,248),TEAL_DARK);viewCrash.setTextSize(12);viewCrash.setOnClickListener(v->showLastCrash());dg.addView(viewCrash,new LinearLayout.LayoutParams(dp(76),dp(42)));diag.addView(dg);body.addView(diag);

    LinearLayout algo=box(Color.rgb(242,248,245),14,18);margin(algo,0,12,0,0);algo.addView(tv("复习机制",15,TEAL_DARK,true));TextView rules=tv("已学习卡可重复出现；最近10次中多次“不清楚”的卡片会获得更高优先级；所有学习数据默认只保存在本机。",12,MUTED,false);margin(rules,0,5,0,0);algo.addView(rules);body.addView(algo);

    View dangerTitle=sectionHeader("其他",null,null);margin(dangerTitle,2,18,0,8);body.addView(dangerTitle);
    Button reset=btn("清空学习进度",RED_SOFT,Color.rgb(160,75,76));reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("将删除所有卡片学习记录、最近10次记录和每日设置。收藏、内容反馈与本地修正不会被此按钮删除。建议先导出完整备份。").setNegativeButton("取消",null).setPositiveButton("清空",(dialog,which)->{store.clearAll();queue.clear();showSettings();}).show());body.addView(reset,new LinearLayout.LayoutParams(-1,dp(48)));
}

    void showExample(Card c){new AlertDialog.Builder(this).setTitle(c.id+" · 来源").setMessage(c.sourceField).setPositiveButton("关闭",null).show();}
    void showFull(Card c){ScrollView sv=new ScrollView(this);TextView body=tv("",14,INK,false);body.setText(richAnswer(repo.fullAnswer(c)));body.setPadding(dp(16),dp(12),dp(16),dp(12));sv.addView(body);new AlertDialog.Builder(this).setTitle(c.id+" · 完整答案").setView(sv).setPositiveButton("关闭",null).show();}
    void showHistory(Card c){StudyStore.State s=store.state(c.id);StringBuilder b=new StringBuilder();int i=1;SimpleDateFormat f=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault());for(StudyStore.Rec r:s.history)b.append(i++).append(". ").append(r.ok?"熟悉":"不清楚").append("  ").append(f.format(new Date(r.ts))).append("\n");if(s.history.isEmpty())b.append("暂无记录");b.append("\n薄弱度：").append(Math.round(engine.difficulty(c)*100)).append("%");new AlertDialog.Builder(this).setTitle("最近10次 · "+c.topic).setMessage(b.toString()).setPositiveButton("关闭",null).show();}


void showPrimaryTopic(String name,List<Card> cards){
    currentScreen="primary";categoryOpenedFromStudy=false;activePrimaryTopic=name;LinkedHashMap<String,List<Card>> subs=secondaryTopics(cards);
    shell(name,cards.size()+" 张卡片 · "+subs.size()+" 个二级专题","卡片库");
    int learned=0;for(Card c:cards)if(engine.learned(c))learned++;
    LinearLayout summary=box(Color.rgb(240,248,245),14,18);LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout st=new LinearLayout(this);st.setOrientation(LinearLayout.VERTICAL);st.addView(tv("一级专题进度",14,TEAL_DARK,true));st.addView(tv("已学习 "+learned+" / "+cards.size(),12,MUTED,false));sr.addView(st,new LinearLayout.LayoutParams(0,-2,1));Button begin=btn(learned==0?"开始专题":"继续专题",TEAL,Color.WHITE);begin.setTextSize(12);begin.setOnClickListener(v->openCategory(name,cards));sr.addView(begin,new LinearLayout.LayoutParams(dp(104),dp(42)));summary.addView(sr);margin(summary,0,0,0,14);body.addView(summary);
    View header=sectionHeader("二级专题","共 "+subs.size()+" 个",null);margin(header,2,0,0,9);body.addView(header);
    for(Map.Entry<String,List<Card>> e:subs.entrySet())body.addView(secondaryTopicRow(e.getKey(),e.getValue()));
}

View secondaryTopicRow(String name,List<Card> cards){
    LinearLayout row=box(SURFACE,12,16);row.setBackground(strokeShape(SURFACE,16,LINE));int learned=0;for(Card c:cards)if(engine.learned(c))learned++;
    LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);txt.addView(tv(name,15,INK,true));txt.addView(tv(cards.size()+" 张 · 已学习 "+learned,11,MUTED,false));line.addView(txt,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=tv("›",22,MUTED,false);arrow.setGravity(Gravity.CENTER);line.addView(arrow,new LinearLayout.LayoutParams(dp(28),dp(42)));row.addView(line);row.setOnClickListener(v->showCategoryCards(name,cards));margin(row,0,0,0,7);return row;
}

void showCategoryCards(String name,List<Card> cards){
    currentScreen=categoryOpenedFromStudy?"categoryFromStudy":(!activePrimaryTopic.isEmpty()?"secondary":"category");setActiveList(name,cards);
    String sub=activePrimaryTopic.isEmpty()?cards.size()+" 张卡片 · 查看与学习":activePrimaryTopic+" · "+cards.size()+" 张卡片";
    shell(name,sub,"卡片库");
    int learned=0;for(Card c:cards)if(engine.learned(c))learned++;
    LinearLayout summary=box(Color.rgb(240,248,245),14,18);LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);LinearLayout st=new LinearLayout(this);st.setOrientation(LinearLayout.VERTICAL);st.addView(tv("专题进度",14,TEAL_DARK,true));st.addView(tv("已学习 "+learned+" / "+cards.size(),12,MUTED,false));sr.addView(st,new LinearLayout.LayoutParams(0,-2,1));Button begin=btn(learned==0?"开始专题":"继续专题",TEAL,Color.WHITE);begin.setTextSize(12);begin.setOnClickListener(v->openCategory(name,cards));sr.addView(begin,new LinearLayout.LayoutParams(dp(104),dp(42)));summary.addView(sr);margin(summary,0,0,0,12);body.addView(summary);

    for(Card c:cards){
        LinearLayout item=box(SURFACE,13,16);item.setBackground(strokeShape(SURFACE,16,LINE));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);LinearLayout txt=new LinearLayout(this);txt.setOrientation(LinearLayout.VERTICAL);TextView q=tv(c.question,14,INK,true);q.setMaxLines(3);txt.addView(q);txt.addView(tv(c.id+" · "+c.kind+" · "+(engine.learned(c)?store.state(c.id).tag:"未学习")+(store.isFavorite(c.id)?" · 已收藏":"")+(feedback.hasCardOverride(c.id)?" · 已修正":""),10,MUTED,false));line.addView(txt,new LinearLayout.LayoutParams(0,-2,1));item.addView(line);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button learn=btn(engine.learned(c)?"复习":"学习",TEAL_SOFT,TEAL_DARK),edit=btn("编辑",Color.rgb(247,249,248),MUTED),report=btn("反馈",Color.rgb(247,249,248),MUTED);learn.setTextSize(12);edit.setTextSize(12);report.setTextSize(12);learn.setOnClickListener(v->{categoryOpenedFromStudy=false;startStudySequence(cards,c);});edit.setOnClickListener(v->editCard(c));report.setOnClickListener(v->reportCard(c));actions.addView(learn,new LinearLayout.LayoutParams(0,dp(38),1));actions.addView(gap(5));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(38),1));actions.addView(gap(5));actions.addView(report,new LinearLayout.LayoutParams(0,dp(38),1));margin(actions,0,9,0,0);item.addView(actions);margin(item,0,0,0,7);body.addView(item);
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
        Button ext=btn("编辑完整答案",TEAL_SOFT,TEAL_DARK);margin(ext,0,12,0,0);ext.setOnClickListener(v->editDetail(c));form.addView(ext,new LinearLayout.LayoutParams(-1,dp(46)));sv.addView(form);
        new AlertDialog.Builder(this).setTitle("本地修正 · "+c.id).setView(sv).setNegativeButton("取消",null).setNeutralButton("标记有误",(d,w)->reportCard(c)).setPositiveButton("保存",(d,w)->{
            List<String> bullets=new ArrayList<>();for(String x:a.getText().toString().split("\n"))if(!x.trim().isEmpty())bullets.add(x.trim());
            feedback.saveCardOverride(c,q.getText().toString().trim(),bullets,tip.getText().toString().trim(),origin.getText().toString().trim());
            Toast.makeText(this,"已本地修正。建议同时导出反馈，以便后续统一修正源题库。",Toast.LENGTH_LONG).show();
        }).show();
    }

    void editDetail(Card c){
        EditText e=new EditText(this);e.setText(repo.fullAnswer(c));e.setMinLines(14);e.setGravity(Gravity.TOP);e.setPadding(dp(14),dp(10),dp(14),dp(10));
        new AlertDialog.Builder(this).setTitle("本地修正完整答案 · "+c.id).setView(e).setNegativeButton("取消",null).setPositiveButton("保存",(dd,ww)->{feedback.saveDetailOverride(c.id,"essay",e.getText().toString());Toast.makeText(this,"完整答案已本地修正",Toast.LENGTH_SHORT).show();}).show();
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
        if("mockPreview".equals(currentScreen)){showHome();return;}
        if("study".equals(currentScreen)){
            if(!queue.isEmpty()&&qIndex<queue.size()){store.saveSession(queue,qIndex);store.saveSessionMeta(mockMode?"mock":"study",mockMode?mockSubject:"");}
            if(hasStudyReturn()&&!mockMode){returnToStudyOrigin();return;}showHome();
            return;
        }
        if("secondary".equals(currentScreen)){
            List<Card> cards=primaryTopics().get(activePrimaryTopic);if(cards!=null){showPrimaryTopic(activePrimaryTopic,cards);return;}showLibrary();return;
        }
        if("frameworkBank".equals(currentScreen)){showFrameworkTreeNode(currentFrameworkNodeId);return;}
        if("frameworkTree".equals(currentScreen)){String parent=frameworkParentId(currentFrameworkNodeId);if(parent.isEmpty())showServiceFramework();else showFrameworkTreeNode(parent);return;}
        if("framework".equals(currentScreen)){showLibrary();return;}
        if("primary".equals(currentScreen)||"category".equals(currentScreen)||"examBank".equals(currentScreen)||"relatedBank".equals(currentScreen)){
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
            try{String raw=readText(data.getData());backupManager.importAll(raw,true);queue.clear();repo=new CardRepository(this,feedback);engine=new ReviewEngine(store);mockEngine=new MockExamEngine(repo,store,engine);Toast.makeText(this,"备份恢复成功。学习进度、本地修正与反馈数据已载入。",Toast.LENGTH_LONG).show();showHome();}catch(Exception e){Toast.makeText(this,"恢复失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }

    int parse(EditText e,int def){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return def;}}
}
