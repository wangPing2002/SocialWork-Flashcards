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

    public int dailyGoal(){
        if(sp.contains("dailyGoal")) return Math.max(5,Math.min(300,sp.getInt("dailyGoal",30)));
        // Migrate gently from the old split setting: keep the former review amount as the unified default.
        return Math.max(5,Math.min(300,sp.getInt("reviewGoal",30)));
    }
    public void setDailyGoal(int n){sp.edit().putInt("dailyGoal",Math.max(5,Math.min(300,n))).apply();}

    String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());}
    String dayKey(Date d){return new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(d);}

    public int todayDone(){
        String k="day_done_"+today();
        if(sp.contains(k)) return sp.getInt(k,0);
        // Preserve progress from previous versions that tracked review/new separately.
        return sp.getInt("day_r_"+today(),0)+sp.getInt("day_n_"+today(),0);
    }
    public void incToday(){String k="day_done_"+today();sp.edit().putInt(k,todayDone()+1).apply();}
    public int dayDone(Date d){
        String key=dayKey(d); String k="day_done_"+key;
        if(sp.contains(k)) return sp.getInt(k,0);
        return sp.getInt("day_r_"+key,0)+sp.getInt("day_n_"+key,0);
    }
    public int totalReviews(){return sp.getInt("totalReviews",0);} public void incTotal(){sp.edit().putInt("totalReviews",totalReviews()+1).apply();}

    // Favorites are independent of spaced repetition and are included automatically in full backup.
    public boolean isFavorite(String cardId){return sp.getBoolean("fav_"+cardId,false);}
    public void setFavorite(String cardId,boolean favorite){SharedPreferences.Editor e=sp.edit();if(favorite)e.putBoolean("fav_"+cardId,true);else e.remove("fav_"+cardId);e.apply();}

    public int streakDays(){
        Calendar cal=Calendar.getInstance(); int streak=0;
        for(int i=0;i<365;i++){
            if(dayDone(cal.getTime())>0) streak++; else if(i==0){ /* today may not have started yet */ } else break;
            cal.add(Calendar.DAY_OF_YEAR,-1);
        }
        return streak;
    }

    public void saveSession(List<Card> cards,int index){
        try{
            JSONArray a=new JSONArray(); for(Card c:cards)a.put(c.id);
            sp.edit().putString("session_ids",a.toString()).putInt("session_index",Math.max(0,index)).apply();
        }catch(Exception ignored){}
    }
    public List<String> sessionIds(){
        List<String> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(sp.getString("session_ids","[]"));for(int i=0;i<a.length();i++)out.add(a.optString(i));}catch(Exception ignored){}
        return out;
    }
    public int sessionIndex(){return Math.max(0,sp.getInt("session_index",0));}
    public boolean hasActiveSession(){List<String> ids=sessionIds();return !ids.isEmpty()&&sessionIndex()<ids.size();}
    public void clearSession(){sp.edit().remove("session_ids").remove("session_index").apply();}

    public JSONObject exportJson(){
        JSONObject root=new JSONObject();
        try{
            root.put("schemaVersion",1);
            JSONObject data=new JSONObject();
            for(Map.Entry<String,?> e:sp.getAll().entrySet()){
                Object v=e.getValue();
                JSONObject item=new JSONObject();
                if(v instanceof String){item.put("type","string");item.put("value",v);}
                else if(v instanceof Integer){item.put("type","int");item.put("value",v);}
                else if(v instanceof Long){item.put("type","long");item.put("value",v);}
                else if(v instanceof Boolean){item.put("type","boolean");item.put("value",v);}
                else if(v instanceof Float){item.put("type","float");item.put("value",v);}
                else if(v instanceof Set){item.put("type","stringSet");JSONArray a=new JSONArray();for(Object x:(Set<?>)v)a.put(String.valueOf(x));item.put("value",a);}
                else continue;
                data.put(e.getKey(),item);
            }
            root.put("preferences",data);
        }catch(Exception ignored){}
        return root;
    }

    public void importJson(JSONObject root, boolean replace){
        JSONObject data=root==null?null:root.optJSONObject("preferences");
        if(data==null)return;
        SharedPreferences.Editor ed=sp.edit();
        if(replace)ed.clear();
        Iterator<String> keys=data.keys();
        while(keys.hasNext()){
            String k=keys.next();JSONObject item=data.optJSONObject(k);if(item==null)continue;
            String t=item.optString("type");
            try{
                if("string".equals(t))ed.putString(k,item.optString("value",null));
                else if("int".equals(t))ed.putInt(k,item.getInt("value"));
                else if("long".equals(t))ed.putLong(k,item.getLong("value"));
                else if("boolean".equals(t))ed.putBoolean(k,item.getBoolean("value"));
                else if("float".equals(t))ed.putFloat(k,(float)item.getDouble("value"));
                else if("stringSet".equals(t)){JSONArray a=item.optJSONArray("value");Set<String> set=new HashSet<>();if(a!=null)for(int i=0;i<a.length();i++)set.add(a.optString(i));ed.putStringSet(k,set);}
            }catch(Exception ignored){}
        }
        ed.apply();
    }

    public void clearAll(){
        // Clear learning/session/settings data but preserve user favorites.
        SharedPreferences.Editor ed=sp.edit();for(String k:sp.getAll().keySet())if(!k.startsWith("fav_"))ed.remove(k);ed.apply();
    }
}
