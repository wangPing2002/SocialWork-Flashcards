package com.seu.socialworkcards;

import java.util.*;

public class MockExamEngine {
    public static class Section {
        public final String title,targetType;public final int scoreEach;public final List<Card> cards=new ArrayList<>();
        Section(String title,String targetType,int scoreEach){this.title=title;this.targetType=targetType;this.scoreEach=scoreEach;}
        public int subtotal(){return scoreEach*cards.size();}
    }
    public static class Paper {
        public final String subject,name,formatNote;public final List<Section> sections=new ArrayList<>();
        Paper(String subject,String name,String formatNote){this.subject=subject;this.name=name;this.formatNote=formatNote;}
        public List<Card> flattened(){List<Card> out=new ArrayList<>();for(Section s:sections)out.addAll(s.cards);return out;}
        public int totalScore(){int n=0;for(Section s:sections)n+=s.subtotal();return n;}
    }

    private final CardRepository repo;private final StudyStore store;private final ReviewEngine review;private final Random rnd=new Random();
    public MockExamEngine(CardRepository repo,StudyStore store,ReviewEngine review){this.repo=repo;this.store=store;this.review=review;}

    public Paper generate(String subject){
        Paper p;
        if("437".equals(subject)){
            p=new Paper("437","社会工作实务","按2026卷面结构：3道名词解释、2道简答、1道材料/案例分析、1道论述，共150分。组卷综合题型、考频、真题身份、收藏、熟练度和内容范围。 ");
            p.sections.add(new Section("一、名词解释","名词解释",10));p.sections.add(new Section("二、简答题","简答题",20));
            p.sections.add(new Section("三、材料/案例分析题","案例题",40));p.sections.add(new Section("四、论述题","论述题",40));
            fill(p.sections.get(0),3,subject,new HashSet<String>());Set<String> used=ids(p);
            fill(p.sections.get(1),2,subject,used);used=ids(p);fill(p.sections.get(2),1,subject,used);used=ids(p);fill(p.sections.get(3),1,subject,used);
        }else{
            p=new Paper("331","社会工作原理","按近年331的150分结构：5道名词解释、5道简答、3道论述。组卷综合题型、考频、真题身份、收藏、熟练度和内容范围。 ");
            p.sections.add(new Section("一、名词解释","名词解释",6));p.sections.add(new Section("二、简答题","简答题",12));p.sections.add(new Section("三、论述题","论述题",20));
            fill(p.sections.get(0),5,"331",new HashSet<String>());Set<String> used=ids(p);fill(p.sections.get(1),5,"331",used);used=ids(p);fill(p.sections.get(2),3,"331",used);
        }
        return p;
    }
    private Set<String> ids(Paper p){Set<String>s=new HashSet<>();for(Section sec:p.sections)for(Card c:sec.cards){s.add(c.id);if(c.category!=null)s.add("cat:"+c.category);}return s;}
    private void fill(Section sec,int count,String subject,Set<String> used){
        List<Card> pool=new ArrayList<>();for(Card c:repo.cards)if(typeCompatible(c,sec.targetType))pool.add(c);
        while(sec.cards.size()<count&&!pool.isEmpty()){Card pick=weightedPick(pool,subject,used);if(pick==null)break;sec.cards.add(pick);pool.remove(pick);used.add(pick.id);if(pick.category!=null)used.add("cat:"+pick.category);}
        if(sec.cards.size()<count){List<Card> fallback=new ArrayList<>(repo.cards);while(sec.cards.size()<count&&!fallback.isEmpty()){Card pick=weightedPick(fallback,subject,used);if(pick==null)break;sec.cards.add(pick);fallback.remove(pick);used.add(pick.id);if(pick.category!=null)used.add("cat:"+pick.category);}}
    }
    private boolean typeCompatible(Card c,String target){
        String t=repo.questionType(c);if("名词解释".equals(target))return t.contains("名词");if("简答题".equals(target))return t.contains("简答");
        if("论述题".equals(target))return t.contains("论述");if("案例题".equals(target))return t.contains("案例")||t.contains("材料");return true;
    }
    private Card weightedPick(List<Card> pool,String subject,Set<String> used){
        List<Card> candidates=new ArrayList<>();List<Double> weights=new ArrayList<>();double total=0;
        for(Card c:pool){
            if(used.contains(c.id))continue;if(repo.isRealExam(c)&&!repo.matchesExamSubject(c,subject))continue;
            double w=1.0;if(c.category!=null&&used.contains("cat:"+c.category))w*=.78;String freq=effectiveFrequency(c);w*=freq.equals("高")?4.0:freq.equals("中")?2.3:1.15;
            if(store.isFavorite(c.id))w*=1.45;String mastery=mastery(c);w*=mastery.equals("低")?1.65:mastery.equals("中")?1.20:.82;w*=1.0+review.difficulty(c)*1.35;
            if(repo.isExactExam(c))w*=2.8;else if(repo.isRecallExam(c))w*=2.0;else w*=1.10;
            if("437".equals(subject)){if(isPractice(c))w*=1.65;else if("社会工作原理".equals(c.category))w*=.72;}
            else {if("社会工作原理".equals(c.category))w*=1.48;else if(isMethod(c))w*=1.15;else if("社会工作实务".equals(c.category))w*=.76;}
            w*=.86+rnd.nextDouble()*.28;candidates.add(c);weights.add(w);total+=w;
        }
        if(candidates.isEmpty())return null;double r=rnd.nextDouble()*total;for(int i=0;i<candidates.size();i++){r-=weights.get(i);if(r<=0)return candidates.get(i);}return candidates.get(candidates.size()-1);
    }
    public String effectiveFrequency(Card c){String o=store.frequencyOverride(c.id);return o==null||o.isEmpty()?repo.systemFrequency(c):o;}
    public String mastery(Card c){StudyStore.State s=store.state(c.id);if(s.history.isEmpty())return "低";double d=review.difficulty(c);if("模糊".equals(s.tag)||d>=.58)return "低";if("掌握".equals(s.tag)||(s.history.size()>=4&&d<=.22))return "高";return "中";}
    private boolean isPractice(Card c){return c!=null&&("社会工作实务".equals(c.category)||isMethod(c)||"督导与管理".equals(c.category));}
    private boolean isMethod(Card c){return c!=null&&("个案工作".equals(c.category)||"小组工作".equals(c.category)||"社区工作".equals(c.category));}
}
