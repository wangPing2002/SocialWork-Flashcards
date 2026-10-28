package com.seu.socialworkcards;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

public class CardRepository {
    public final List<Card> cards = new ArrayList<>();
    // 437M 专属模块绝不混入主库2497，作为可选训练题源独立存放。
    public final List<Card> v24Cards = new ArrayList<>();
    // R10来源为汇编题文，非已核签原卷。与C主库和437M专项完全隔离。
    public final List<Card> r10ExamCards = new ArrayList<>();
    public final JSONObject r10Records = new JSONObject();
    private final Set<String> r10MockAllowed = new HashSet<>();

    public final JSONObject v24Records = new JSONObject();
    private final JSONObject zCases;
    public final JSONObject details;
    public final JSONObject examMeta;
    public final JSONObject examAnswers;
    public final JSONObject topicTaxonomy;
    public final JSONObject questionOverlay;
    private final ContentFeedbackStore feedback;
    private final Map<String,Card> byIdMap = new HashMap<>();
    private final Map<String,String> answerOverrideCache = new HashMap<>();

    public CardRepository(Context ctx, ContentFeedbackStore feedback) throws Exception {
        this.feedback=feedback;
        JSONArray a = new JSONArray(readAsset(ctx, "cards.json"));
        for (int i=0;i<a.length();i++){
            Card c=Card.fromJson(a.getJSONObject(i));
            feedback.applyCardOverride(c);
            cards.add(c);
            byIdMap.put(c.id,c);
        }
        // 主库仅增加可追溯的训练分值建议映射，不改2497张原题、答案与ID。
        JSONObject trainingRoot=new JSONObject(readAsset(ctx,"training_score_suggestions_v321.json"));
        JSONObject trainingScores=trainingRoot.getJSONObject("scores");
        if(cards.size()!=2497||trainingScores.length()!=2497)throw new IllegalStateException("2497题训练分值映射不完整");
        for(Card c:cards){
            JSONObject item=trainingScores.optJSONObject(c.id);
            if(item==null)throw new IllegalStateException("训练分值缺失: "+c.id);
            int training=item.optInt("trainingScore",0), effective=item.optInt("answerEffectiveChars",0);
            if(effective!=c.answerEffectiveChars||training<=0||effective<25*training||effective>30*training)
                throw new IllegalStateException("训练分值与答案有效字数不一致: "+c.id);
            c.trainingScore=training;
            c.trainingScoreSuggested=true;
        }
        JSONObject v24=new JSONObject(readAsset(ctx,"v24_437_378_cards.json"));
        JSONArray v24Items=v24.getJSONArray("cards");
        if(v24Items.length()!=378)throw new IllegalStateException("437专项不完整: "+v24Items.length());
        for(int i=0;i<v24Items.length();i++){
            JSONObject raw=v24Items.getJSONObject(i);
            Card c=new Card();
            c.id=raw.getString("id");
            if(!c.id.matches("437M\\d{3}") || byIdMap.containsKey(c.id))throw new IllegalStateException("437专项卡ID冲突: "+c.id);
            c.sourceModule="V24_437";c.examSubjectScope="437";
            c.topic=raw.optString("term");c.category="437实务生僻名词专项";
            c.primaryTopic="437实务生僻名词专项";c.secondaryTopic="生僻名词解释";
            c.kind="名词解释";c.questionType="名词解释";
            // 仅统一前台显示术语，原始V24的front/source仍原样留存在v24Records中。
            // 378条front全部为“名词解释：术语”；类型已由questionType独立展示，不重复。
            String rawFront=raw.optString("front",c.topic).trim();
            c.question=rawFront.replaceFirst("^名词解释\\s*[：:]\\s*","");
            if(c.question.trim().isEmpty())c.question=c.topic;
            c.fullAnswer=raw.optString("answer");c.hasLong=true;
            c.trainingScore=raw.optInt("trainingScore",0);c.answerEffectiveChars=raw.optInt("answerEffectiveChars",0);
            c.origin="V24·437实务专项独立训练候选（非331题源、非官方原卷）";
            JSONObject src=raw.optJSONObject("source");
            if(src!=null){c.sourceField=src.toString();}
            c.sourceLevel="";
            c.answerStatus="";
            c.tip="三词提示 · 五维答案 · 训练分值独立于原题分值";
            c.predictedFrequency="中";
            c.bullets.add(c.fullAnswer);
            v24Cards.add(c);v24Records.put(c.id,raw);byIdMap.put(c.id,c);
        }
        if(v24Cards.size()!=378)throw new IllegalStateException("437专项必须378题");
        JSONObject r10=new JSONObject(readAsset(ctx,"z00_r10_exam_cards.json"));
        JSONArray r10Items=r10.getJSONArray("cards");
        if(r10Items.length()!=56)throw new IllegalStateException("R10题文登记数量不符: "+r10Items.length());
        for(int i=0;i<r10Items.length();i++){
            JSONObject raw=r10Items.getJSONObject(i);
            Card c=Card.fromJson(raw);
            if(!c.id.matches("R10-\\d{3}")||byIdMap.containsKey(c.id))throw new IllegalStateException("R10题号冲突: "+c.id);
            if(!"Z00_R10".equals(c.sourceModule)||!("331".equals(c.examSubjectScope)||"437".equals(c.examSubjectScope)))throw new IllegalStateException("R10题源科目非法: "+c.id);
            c.trainingScoreSuggested="AI_PROPOSED_NOT_SIGNED".equals(raw.optString("trainingScoreGate",""));
            r10ExamCards.add(c);r10Records.put(c.id,raw);byIdMap.put(c.id,c);
            if(raw.optBoolean("mockEligible",false))r10MockAllowed.add(c.id);
        }
        if(r10ExamCards.size()!=56||r10MockAllowed.size()!=54)throw new IllegalStateException("R10学习/模拟题源数量异常");

        zCases=readOptionalObject(ctx,"z_case_staging_v31.json").optJSONObject("cards");
        topicTaxonomy=readOptionalObject(ctx,"topic_taxonomy_v30.json");
        questionOverlay=readOptionalObject(ctx,"question_overlay_v30.json");
        JSONObject cardTopics=topicTaxonomy.optJSONObject("cardTopics");
        if(cardTopics!=null)for(Card c:cards){JSONObject t=cardTopics.optJSONObject(c.id);if(t!=null){
            String primary=t.optString("primaryTopic","");String secondary=t.optString("secondaryTopic","");
            if(!primary.isEmpty())c.primaryTopic=primary;
            if(!secondary.isEmpty())c.secondaryTopic=secondary;
        }}
        details=readOptionalObject(ctx,"details.json");
        examMeta=readOptionalObject(ctx,"exam_meta.json");
        examAnswers=readOptionalObject(ctx,"exam_answers.json");
        JSONObject local=feedback.detailOverrides();
        Iterator<String> keys=local.keys();
        while(keys.hasNext()){
            String id=keys.next();
            JSONObject x=local.optJSONObject(id);
            if(x!=null){String essay=x.optString("essay","").trim();if(!essay.isEmpty())answerOverrideCache.put(id,essay);}
        }
    }
    static JSONObject readOptionalObject(Context ctx,String name){try{return new JSONObject(readAsset(ctx,name));}catch(Exception e){return new JSONObject();}}
    static String readAsset(Context ctx, String name) throws Exception {
        try(InputStream in=ctx.getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString("UTF-8");
        }
    }
    public Card byId(String id){return id==null?null:byIdMap.get(id);}
    public boolean isV24(Card c){return c!=null&&"V24_437".equals(c.sourceModule);}
    public boolean isR10(Card c){return c!=null&&"Z00_R10".equals(c.sourceModule);}
    public boolean isR10Case(Card c){return isR10(c)&&r10Records.optJSONObject(c.id)!=null&&r10Records.optJSONObject(c.id).optBoolean("isCaseGroup",false);}
    public JSONObject r10Record(Card c){return isR10(c)?r10Records.optJSONObject(c.id):null;}

    public JSONObject v24Record(Card c){return isV24(c)?v24Records.optJSONObject(c.id):null;}
    public JSONArray stagedCases(Card c){return c==null||isV24(c)||zCases==null?null:zCases.optJSONArray(c.id);}
    public boolean eligibleForExam(Card c,String subject){
        if(c==null||!("331".equals(subject)||"437".equals(subject)))return false;
        if(isV24(c))return "437".equals(subject); // 永远不进入331题池
        if(isR10(c))return subject.equals(c.examSubjectScope)&&r10MockAllowed.contains(c.id); // 真题补录绝不跨科，摘要案例不进入模拟卷
        return c.examSubjectScope==null||c.examSubjectScope.isEmpty()||Arrays.asList(c.examSubjectScope.split(",")).contains(subject);
    }

    public JSONObject detail(String key){return feedback.mergeDetail(key,details.optJSONObject(key));}
    public JSONObject examMeta(String key){return examMeta.optJSONObject(key);}
    public JSONObject examAnswer(String key){return key==null||key.isEmpty()?null:examAnswers.optJSONObject(key);}

    // 原题仍保存在原始记录中，仅去除 R10 展示序号。
    public String cleanR10DisplayNumber(String raw){
        if(raw==null)return "";
        return raw.replaceFirst("^\\s*(?:[0-9]{1,2}[.．、)）]|[(（][0-9]{1,2}[)）]|[一二三四五六七八九十]{1,3}[、.．])\\s*", "").trim();
    }
    public String displayQuestion(Card c){
        if(c==null)return "";
        if(isR10(c))return cleanR10DisplayNumber(c.question);
        JSONObject o=questionOverlay.optJSONObject(c.id);
        if(o==null||!o.optString("baselineQuestion","").equals(c.question))return c.question;
        if(!c.exactExamSources.isEmpty()||!c.recallExamSources.isEmpty())return c.question;
        String training=o.optString("trainingQuestion","").trim();
        return training.isEmpty()?c.question:training;
    }
    public boolean hasTrainingRewrite(Card c){return c!=null&&!isR10(c)&&!displayQuestion(c).equals(c.question);}
    public boolean isExactExam(Card c){return c!=null&&!c.exactExamSources.isEmpty();}
    public boolean isRecallExam(Card c){return c!=null&&!c.recallExamSources.isEmpty();}
    public boolean isRealExam(Card c){return isR10(c)||isExactExam(c)||isRecallExam(c);}
    public boolean matchesExamSubject(Card c,String subject){
        if(!eligibleForExam(c,subject))return false;
        if(isR10(c))return true; // R10科目/完整题文门禁已在eligibleForExam核查
        if(!isRealExam(c))return true;
        for(String x:c.exactExamSources)if(x.endsWith(" "+subject))return true;
        for(String x:c.recallExamSources)if(x.endsWith(" "+subject))return true;
        return false;
    }
    public String examSourceLabel(Card c){
        if(c==null)return "模拟题";
        if(isV24(c))return "437专项训练·非原卷";
        if(isR10(c)){JSONObject r=r10Record(c);return (r==null?"":r.optInt("year")+"·"+r.optString("subject")+" ")+"汇编题文·原卷待核";}
        List<String> src=!c.exactExamSources.isEmpty()?c.exactExamSources:c.recallExamSources;
        if(src.isEmpty())return "模拟题";
        String prefix=!c.exactExamSources.isEmpty()?"真题关联·原卷待核·":"回忆考点·原题待核·";
        return prefix+src.get(0)+(src.size()>1?" +"+(src.size()-1):"");
    }
    public String questionType(Card c){return c==null?"题目":normalizeType(c.questionType==null||c.questionType.isEmpty()?c.kind:c.questionType);}
    public String fullAnswer(Card c){
        if(c==null)return "";
        String edited=answerOverrideCache.get(c.id);
        if(edited!=null&&!edited.trim().isEmpty())return edited;
        if(c.fullAnswer!=null&&!c.fullAnswer.trim().isEmpty())return c.fullAnswer;
        StringBuilder b=new StringBuilder();for(String x:c.bullets){if(b.length()>0)b.append("\n\n");b.append(x);}return b.toString();
    }
    public void setFullAnswerOverride(String id,String essay){if(id==null)return;if(essay==null||essay.trim().isEmpty())answerOverrideCache.remove(id);else answerOverrideCache.put(id,essay);}
    public void clearFullAnswerOverride(String id){if(id!=null)answerOverrideCache.remove(id);}
    public String systemFrequency(Card c){
        String x=c==null?"":c.predictedFrequency;
        if(x.contains("高"))return "高";if(x.contains("低"))return "低";return "中";
    }
    public List<Card> relatedCards(Card c){List<Card> out=new ArrayList<>();if(c==null)return out;for(String id:c.relatedIds){Card x=byId(id);if(x!=null)out.add(x);}return out;}
    static String normalizeType(String t){
        if(t==null)return "题目";if(t.contains("名词"))return "名词解释";if(t.contains("简答"))return "简答题";
        if(t.contains("案例"))return "案例分析题";if(t.contains("材料"))return "材料题";if(t.contains("论述"))return "论述题";return t;
    }
}
