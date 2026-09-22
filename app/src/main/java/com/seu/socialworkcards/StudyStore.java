package com.seu.socialworkcards;

import android.content.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StudyStore {
    public static class Rec { public boolean ok; public long ts; Rec(boolean o,long t){ok=o;ts=t;} }
    public static class State {
        public int stage=0; public long due=0; public String tag="未学习"; public final List<Rec> history=new ArrayList<>();
    }
    private final SharedPreferences sp;
    public StudyStore(Context c){ sp=c.getSharedPreferences("study_v1",Context.MODE_PRIVATE); }

    public State state(String id){
        State s=new State();
        try{
            JSONObject o=new JSONObject(sp.getString("card_"+id,"{}"));
            s.stage=o.optInt("stage",0); s.due=o.optLong("due",0); s.tag=o.optString("tag","未学习");
            JSONArray a=o.optJSONArray("history");
            if(a!=null){ int start=Math.max(0,a.length()-10); for(int i=start;i<a.length();i++){JSONObject r=a.getJSONObject(i);s.history.add(new Rec(r.optBoolean("ok"),r.optLong("ts")));}}
        }catch(Exception ignored){}
        return s;
    }
    public void save(String id, State s){
        try{
            JSONObject o=new JSONObject(); o.put("stage",s.stage);o.put("due",s.due);o.put("tag",s.tag);
            JSONArray a=new JSONArray(); int start=Math.max(0,s.history.size()-10);
            for(int i=start;i<s.history.size();i++){Rec r=s.history.get(i);JSONObject x=new JSONObject();x.put("ok",r.ok);x.put("ts",r.ts);a.put(x);} o.put("history",a);
            sp.edit().putString("card_"+id,o.toString()).apply();
        }catch(Exception ignored){}
    }
    public int reviewGoal(){return Math.max(5,Math.min(300,sp.getInt("reviewGoal",30)));}
    public int newGoal(){return Math.max(0,Math.min(200,sp.getInt("newGoal",15)));}
    public void setGoals(int r,int n){sp.edit().putInt("reviewGoal",r).putInt("newGoal",n).apply();}
    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());}
    public int todayReview(){return sp.getInt("day_r_"+today(),0);} public int todayNew(){return sp.getInt("day_n_"+today(),0);}
    public void incToday(boolean wasNew){String k=wasNew?"day_n_":"day_r_";sp.edit().putInt(k+today(),sp.getInt(k+today(),0)+1).apply();}
    public int totalReviews(){return sp.getInt("totalReviews",0);} public void incTotal(){sp.edit().putInt("totalReviews",totalReviews()+1).apply();}
    public void clearAll(){sp.edit().clear().apply();}
}
