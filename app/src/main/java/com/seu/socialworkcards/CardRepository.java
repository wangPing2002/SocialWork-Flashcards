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

    public CardRepository(Context ctx, ContentFeedbackStore feedback) throws Exception {
        this.feedback=feedback;
        JSONArray a = new JSONArray(readAsset(ctx, "cards.json"));
        for (int i=0;i<a.length();i++){Card c=Card.fromJson(a.getJSONObject(i));feedback.applyCardOverride(c);cards.add(c);}
        details=readOptionalObject(ctx,"details.json"); examMeta=readOptionalObject(ctx,"exam_meta.json"); examAnswers=readOptionalObject(ctx,"exam_answers.json");
    }
    static JSONObject readOptionalObject(Context ctx,String name){try{return new JSONObject(readAsset(ctx,name));}catch(Exception e){return new JSONObject();}}
    static String readAsset(Context ctx, String name) throws Exception {
        InputStream in=ctx.getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))>0)out.write(buf,0,n);return out.toString("UTF-8");
    }
    public Card byId(String id){for(Card c:cards)if(c.id.equals(id))return c;return null;}
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
        JSONObject local=feedback.mergeDetail(c.id,null);String edited=local.optString("essay","");
        if(!edited.trim().isEmpty())return edited;
        if(c.fullAnswer!=null&&!c.fullAnswer.trim().isEmpty())return c.fullAnswer;
        StringBuilder b=new StringBuilder();for(String x:c.bullets){if(b.length()>0)b.append("\n\n");b.append(x);}return b.toString();
    }
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
