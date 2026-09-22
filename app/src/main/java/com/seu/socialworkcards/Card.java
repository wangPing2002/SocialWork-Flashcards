package com.seu.socialworkcards;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Card {
    public String id, topic, kind, question, tip, origin;
    public boolean hasExample, hasLong;
    public final List<String> bullets = new ArrayList<>();

    public static Card fromJson(JSONObject o) throws Exception {
        Card c = new Card();
        c.id = o.optString("id"); c.topic = o.optString("topic"); c.kind = o.optString("kind");
        c.question = o.optString("question"); c.tip = o.optString("tip"); c.origin = o.optString("origin");
        c.hasExample = o.optBoolean("hasExample"); c.hasLong = o.optBoolean("hasLong");
        JSONArray a = o.optJSONArray("bullets");
        if (a != null) for (int i=0;i<a.length();i++) c.bullets.add(a.optString(i));
        return c;
    }
}
