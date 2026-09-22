package com.seu.socialworkcards;

import java.util.*;

public class ReviewEngine {
    public static final long[] INTERVAL_MIN={0,10,30,720,1440,2880,5760,10080,21600,43200,86400};
    private final StudyStore store; private final Random rnd=new Random();
    public ReviewEngine(StudyStore s){store=s;}
    public boolean unseen(Card c){return store.state(c.id).history.isEmpty();}
    public boolean due(Card c){return store.state(c.id).due<=System.currentTimeMillis();}
    public double difficulty(Card c){
        StudyStore.State s=store.state(c.id); List<StudyStore.Rec> h=s.history;
        if(h.isEmpty()) return .18;
        double bad=0,total=0; int streak=0;
        for(int i=0;i<h.size();i++){double w=i+1;total+=w;if(!h.get(i).ok)bad+=w;}
        for(int i=h.size()-1;i>=0;i--){if(h.get(i).ok)break;streak++;}
        return Math.min(1,(bad/Math.max(1,total))*.82+Math.min(3,streak)*.08);
    }
    double overdue(Card c){long d=store.state(c.id).due;if(d<=0)return .25;double hrs=Math.max(0,(System.currentTimeMillis()-d)/3600000.0);return Math.min(1,hrs/72.0);}
    double priority(Card c){return difficulty(c)*.72+overdue(c)*.28+rnd.nextDouble()*.03;}

    public List<Card> dailyQueue(List<Card> all,int reviewLimit,int newLimit){
        List<Card> learned=new ArrayList<>(), fresh=new ArrayList<>();
        for(Card c:all){if(unseen(c))fresh.add(c);else learned.add(c);}
        learned.sort((a,b)->Double.compare(priority(b),priority(a)));
        Collections.shuffle(fresh,rnd);
        List<Card> out=new ArrayList<>(); Map<String,Integer> use=new HashMap<>();
        int guard=0;
        while(out.size()<reviewLimit && !learned.isEmpty() && guard<reviewLimit*30){guard++; Card c=weighted(learned,out.isEmpty()?null:out.get(out.size()-1));double d=difficulty(c);int max=d>=.70?3:d>=.45?2:1;int n=use.getOrDefault(c.id,0);if(n>=max){learned.remove(c);continue;}out.add(c);use.put(c.id,n+1);}
        for(int i=0;i<Math.min(newLimit,fresh.size());i++)out.add(fresh.get(i));
        return out;
    }
    Card weighted(List<Card> pool,Card last){
        double sum=0;double[] w=new double[pool.size()];
        for(int i=0;i<pool.size();i++){Card c=pool.get(i);w[i]=1+difficulty(c)*5+overdue(c)*2;if(last!=null&&last.id.equals(c.id))w[i]*=.08;sum+=w[i];}
        double r=rnd.nextDouble()*sum;for(int i=0;i<pool.size();i++){r-=w[i];if(r<=0)return pool.get(i);}return pool.get(pool.size()-1);
    }
    public void rate(Card c, boolean ok){
        StudyStore.State s=store.state(c.id); boolean wasNew=s.history.isEmpty(); s.history.add(new StudyStore.Rec(ok,System.currentTimeMillis()));while(s.history.size()>10)s.history.remove(0);
        int recentBad=0;for(StudyStore.Rec x:s.history)if(!x.ok)recentBad++;
        if(ok){s.stage=Math.min(INTERVAL_MIN.length-1,s.stage+1);double weak=difficultyFromHistory(s.history);double factor=weak>=.65?.55:weak>=.4?.75:1;s.due=System.currentTimeMillis()+Math.max(10,Math.round(INTERVAL_MIN[s.stage]*factor))*60000L;s.tag=weak<.25&&s.history.size()>=4?"掌握":"熟悉";}
        else{s.stage=Math.max(0,s.stage-(recentBad>=5?3:2));int mins=recentBad>=6?5:recentBad>=3?7:10;s.due=System.currentTimeMillis()+mins*60000L;s.tag="模糊";}
        store.save(c.id,s);store.incToday(wasNew);store.incTotal();
    }
    double difficultyFromHistory(List<StudyStore.Rec> h){if(h.isEmpty())return .18;double bad=0,total=0;int streak=0;for(int i=0;i<h.size();i++){double w=i+1;total+=w;if(!h.get(i).ok)bad+=w;}for(int i=h.size()-1;i>=0;i--){if(h.get(i).ok)break;streak++;}return Math.min(1,(bad/Math.max(1,total))*.82+Math.min(3,streak)*.08);}
}
