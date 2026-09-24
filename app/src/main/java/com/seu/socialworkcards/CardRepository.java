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
        for (int i=0;i<a.length();i++){ Card c=Card.fromJson(a.getJSONObject(i)); feedback.applyCardOverride(c); cards.add(c);}
        details = new JSONObject(readAsset(ctx, "details.json"));
        examMeta = new JSONObject(readAsset(ctx, "exam_meta.json"));
        examAnswers = new JSONObject(readAsset(ctx, "exam_answers.json"));
    }
    static String readAsset(Context ctx, String name) throws Exception {
        InputStream in = ctx.getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n=in.read(buf))>0) out.write(buf,0,n);
        return out.toString("UTF-8");
    }
    public Card byId(String id){ for(Card c:cards) if(c.id.equals(id)) return c; return null; }
    public JSONObject detail(String topic){ return feedback.mergeDetail(topic,details.optJSONObject(topic)); }
    public JSONObject examMeta(String topic){ return examMeta.optJSONObject(topic); }
    public JSONObject examAnswer(String key){ return key==null||key.isEmpty()?null:examAnswers.optJSONObject(key); }
}
