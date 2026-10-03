package com.seu.socialworkcards;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Card {
    public String id="", topic="", category="", primaryTopic="", secondaryTopic="", kind="", question="", tip="", origin="";
    public String questionType="", fullAnswer="", predictedFrequency="中", predictedFrequencyRaw="";
    public String sourceField="", truthLabel="", truthQuestion="", truthEvidence="", truthRelation="";
    public String answerStatus="", sourceLevel="";
    public boolean hasExample, hasLong;
    public final List<String> bullets = new ArrayList<>();
    public final List<String> relatedIds = new ArrayList<>();
    public final List<String> exactExamSources = new ArrayList<>();
    public final List<String> recallExamSources = new ArrayList<>();

    public static Card fromJson(JSONObject o) throws Exception {
        Card c = new Card();
        c.id=o.optString("id"); c.topic=o.optString("topic"); c.category=o.optString("category");
        c.primaryTopic=o.optString("primaryTopic",c.category); c.secondaryTopic=o.optString("secondaryTopic",c.topic);
        c.kind=o.optString("kind"); c.question=o.optString("question"); c.tip=o.optString("tip"); c.origin=o.optString("origin");
        c.questionType=o.optString("questionType"); c.fullAnswer=o.optString("fullAnswer");
        c.predictedFrequency=o.optString("predictedFrequency","中"); c.predictedFrequencyRaw=o.optString("predictedFrequencyRaw",c.predictedFrequency);
        c.sourceField=o.optString("sourceField"); c.truthLabel=o.optString("truthLabel"); c.truthQuestion=o.optString("truthQuestion");
        c.truthEvidence=o.optString("truthEvidence"); c.truthRelation=o.optString("truthRelation");
        c.answerStatus=o.optString("answerStatus"); c.sourceLevel=o.optString("sourceLevel");
        c.hasExample=o.optBoolean("hasExample"); c.hasLong=o.optBoolean("hasLong",true);
        readArray(o.optJSONArray("bullets"),c.bullets);
        readArray(o.optJSONArray("relatedIds"),c.relatedIds);
        readArray(o.optJSONArray("exactExamSources"),c.exactExamSources);
        readArray(o.optJSONArray("recallExamSources"),c.recallExamSources);
        return c;
    }
    static void readArray(JSONArray a,List<String> out){if(a!=null)for(int i=0;i<a.length();i++){String x=a.optString(i);if(x!=null&&!x.trim().isEmpty())out.add(x);}}
}
