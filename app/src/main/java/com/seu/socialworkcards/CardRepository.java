package com.seu.socialworkcards;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

public class CardRepository {
    public final List<Card> cards = new ArrayList<>();
    public final JSONObject details;

    public CardRepository(Context ctx) throws Exception {
        JSONArray a = new JSONArray(readAsset(ctx, "cards.json"));
        for (int i=0;i<a.length();i++) cards.add(Card.fromJson(a.getJSONObject(i)));
        details = new JSONObject(readAsset(ctx, "details.json"));
    }
    static String readAsset(Context ctx, String name) throws Exception {
        InputStream in = ctx.getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n=in.read(buf))>0) out.write(buf,0,n);
        return out.toString("UTF-8");
    }
    public Card byId(String id){ for(Card c:cards) if(c.id.equals(id)) return c; return null; }
    public JSONObject detail(String topic){ return details.optJSONObject(topic); }
}
