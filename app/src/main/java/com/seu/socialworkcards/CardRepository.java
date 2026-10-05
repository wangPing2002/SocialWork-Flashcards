package com.seu.socialworkcards;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

public class CardRepository {
    public final List<Card> cards = new ArrayList<>();
    public final JSONObject details;
    public final JSONObject examMeta;
    public final JSONObject examAnswers;
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
    public JSONObject detail(String key){return feedback.mergeDetail(key,details.optJSONObject(key));}
    public JSONObject examMeta(String key){return examMeta.optJSONObject(key);}
    public JSONObject examAnswer(String key){return key==null||key.isEmpty()?null:examAnswers.optJSONObject(key);}

    public boolean isExactExam(Card c){return c!=null&&!c.exactExamSources.isEmpty();}
    public boolean isRecallExam(Card c){return c!=null&&!c.recallExamSources.isEmpty();}
    public boolean isRealExam(Card c){return isExactExam(c)||isRecallExam(c);}
    public boolean matchesExamSubject(Card c,String subject){
        if(c==null||!isRealExam(c))return true;
        for(String x:c.exactExamSources)if(x.endsWith(" "+subject))return true;
        for(String x:c.recallExamSources)if(x.endsWith(" "+subject))return true;
        return false;
    }
    public String examSourceLabel(Card c){
        if(c==null)return "模拟题";
        List<String> src=!c.exactExamSources.isEmpty()?c.exactExamSources:c.recallExamSources;
        if(src.isEmpty())return "模拟题";
        String prefix=!c.exactExamSources.isEmpty()?"真题·":"真题主题·";
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
