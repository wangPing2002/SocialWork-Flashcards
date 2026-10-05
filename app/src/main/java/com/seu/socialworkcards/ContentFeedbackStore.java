package com.seu.socialworkcards;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

/**
 * SQLite-backed local correction/feedback database.
 * Bundled cards remain immutable assets; user corrections live here as overrides.
 * v2.9.0 keeps override snapshots in memory so the 2497-card deck does not issue
 * thousands of tiny SQLite queries during startup/search/rendering.
 */
public class ContentFeedbackStore extends SQLiteOpenHelper {
    private static final String DB="content_feedback.db";
    private static final int VERSION=2;
    private final Map<String,JSONObject> cardOverrideCache=new HashMap<>();
    private final Map<String,JSONObject> detailOverrideCache=new HashMap<>();
    private boolean overrideCachesLoaded=false;

    public ContentFeedbackStore(Context c){ super(c,DB,null,VERSION); }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE card_overrides(card_id TEXT PRIMARY KEY, question TEXT, bullets_json TEXT, tip TEXT, origin TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE detail_overrides(topic TEXT PRIMARY KEY, example TEXT, name TEXT, simple TEXT, essay TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE reports(report_id TEXT PRIMARY KEY, card_id TEXT, topic TEXT, kind TEXT, question TEXT, answer_json TEXT, origin TEXT, type TEXT, note TEXT, created_at INTEGER, status TEXT)");
        db.execSQL("CREATE INDEX idx_reports_status ON reports(status)");
        db.execSQL("CREATE INDEX idx_reports_card ON reports(card_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        if(oldV<2){
            db.execSQL("CREATE TABLE IF NOT EXISTS migration_log(version INTEGER PRIMARY KEY, migrated_at INTEGER)");
            ContentValues v=new ContentValues();v.put("version",2);v.put("migrated_at",System.currentTimeMillis());
            db.insertWithOnConflict("migration_log",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private synchronized void ensureOverrideCaches(){
        if(overrideCachesLoaded)return;
        cardOverrideCache.clear();detailOverrideCache.clear();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM card_overrides",null)){
            while(c.moveToNext()){
                try{
                    JSONObject o=new JSONObject();o.put("question",c.getString(c.getColumnIndexOrThrow("question")));o.put("bullets",new JSONArray(c.getString(c.getColumnIndexOrThrow("bullets_json"))));o.put("tip",c.getString(c.getColumnIndexOrThrow("tip")));o.put("origin",c.getString(c.getColumnIndexOrThrow("origin")));o.put("updatedAt",c.getLong(c.getColumnIndexOrThrow("updated_at")));
                    cardOverrideCache.put(c.getString(c.getColumnIndexOrThrow("card_id")),o);
                }catch(Exception ignored){}
            }
        }
        try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM detail_overrides",null)){
            while(c.moveToNext()){
                try{
                    JSONObject o=new JSONObject();for(String k:new String[]{"example","name","simple","essay"}){int i=c.getColumnIndex(k);if(i>=0&&!c.isNull(i))o.put(k,c.getString(i));}o.put("updatedAt",c.getLong(c.getColumnIndexOrThrow("updated_at")));
                    detailOverrideCache.put(c.getString(c.getColumnIndexOrThrow("topic")),o);
                }catch(Exception ignored){}
            }
        }
        overrideCachesLoaded=true;
    }
    private synchronized void invalidateOverrideCaches(){overrideCachesLoaded=false;cardOverrideCache.clear();detailOverrideCache.clear();}
    private JSONObject copy(JSONObject x){try{return new JSONObject(x==null?"{}":x.toString());}catch(Exception e){return new JSONObject();}}

    public void applyCardOverride(Card c){
        if(c==null)return;ensureOverrideCaches();JSONObject x=cardOverrideCache.get(c.id);if(x==null)return;
        try{
            c.question=x.optString("question",c.question);c.tip=x.optString("tip",c.tip);c.origin=x.optString("origin",c.origin);
            JSONArray a=x.optJSONArray("bullets");if(a!=null){c.bullets.clear();for(int i=0;i<a.length();i++)c.bullets.add(a.optString(i));}
        }catch(Exception ignored){}
    }

    public void saveCardOverride(Card c,String question,List<String> bullets,String tip,String origin){
        try{
            JSONArray a=new JSONArray();List<String> cleaned=new ArrayList<>();for(String x:bullets)if(x!=null&&!x.trim().isEmpty()){a.put(x.trim());cleaned.add(x.trim());}
            long now=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("card_id",c.id);v.put("question",question);v.put("bullets_json",a.toString());v.put("tip",tip);v.put("origin",origin);v.put("updated_at",now);
            getWritableDatabase().insertWithOnConflict("card_overrides",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            c.question=question;c.tip=tip;c.origin=origin;c.bullets.clear();c.bullets.addAll(cleaned);
            ensureOverrideCaches();JSONObject o=new JSONObject();o.put("question",question);o.put("bullets",a);o.put("tip",tip);o.put("origin",origin);o.put("updatedAt",now);cardOverrideCache.put(c.id,o);
        }catch(Exception ignored){}
    }
    public boolean hasCardOverride(String id){ensureOverrideCaches();return cardOverrideCache.containsKey(id);}
    public void clearCardOverride(String id){getWritableDatabase().delete("card_overrides","card_id=?",new String[]{id});ensureOverrideCaches();cardOverrideCache.remove(id);}
    public boolean hasDetailOverride(String topic){ensureOverrideCaches();return detailOverrideCache.containsKey(topic);}
    public void clearDetailOverride(String topic){getWritableDatabase().delete("detail_overrides","topic=?",new String[]{topic});ensureOverrideCaches();detailOverrideCache.remove(topic);}

    public JSONObject mergeDetail(String topic,JSONObject base){
        JSONObject out=copy(base);ensureOverrideCaches();JSONObject local=detailOverrideCache.get(topic);
        if(local!=null)for(String key:new String[]{"example","name","simple","essay"})if(local.has(key))try{out.put(key,local.optString(key));}catch(Exception ignored){}
        return out;
    }
    public void saveDetailOverride(String topic,String key,String value){
        if(!Arrays.asList("example","name","simple","essay").contains(key))return;
        SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("topic",topic);v.put(key,value);v.put("updated_at",now);
        long n=db.update("detail_overrides",v,"topic=?",new String[]{topic});if(n==0)db.insert("detail_overrides",null,v);
        ensureOverrideCaches();JSONObject o=detailOverrideCache.get(topic);if(o==null){o=new JSONObject();detailOverrideCache.put(topic,o);}try{o.put(key,value);o.put("updatedAt",now);}catch(Exception ignored){}
    }

    public void addReport(Card c,String type,String note){
        try{
            JSONArray a=new JSONArray();for(String x:c.bullets)a.put(x);ContentValues v=new ContentValues();long now=System.currentTimeMillis();
            v.put("report_id",c.id+"-"+now);v.put("card_id",c.id);v.put("topic",c.topic);v.put("kind",c.kind);v.put("question",c.question);v.put("answer_json",a.toString());v.put("origin",c.origin);v.put("type",type);v.put("note",note);v.put("created_at",now);v.put("status","pending");
            getWritableDatabase().insert("reports",null,v);
        }catch(Exception ignored){}
    }
    public int pendingCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM reports WHERE status='pending'",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public JSONArray reports(){
        JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM reports ORDER BY created_at DESC",null)){while(c.moveToNext()){JSONObject o=new JSONObject();try{o.put("reportId",c.getString(c.getColumnIndexOrThrow("report_id")));o.put("cardId",c.getString(c.getColumnIndexOrThrow("card_id")));o.put("topic",c.getString(c.getColumnIndexOrThrow("topic")));o.put("kind",c.getString(c.getColumnIndexOrThrow("kind")));o.put("question",c.getString(c.getColumnIndexOrThrow("question")));o.put("answerBullets",new JSONArray(c.getString(c.getColumnIndexOrThrow("answer_json"))));o.put("origin",c.getString(c.getColumnIndexOrThrow("origin")));o.put("type",c.getString(c.getColumnIndexOrThrow("type")));o.put("note",c.getString(c.getColumnIndexOrThrow("note")));o.put("createdAt",c.getLong(c.getColumnIndexOrThrow("created_at")));o.put("status",c.getString(c.getColumnIndexOrThrow("status")));a.put(o);}catch(Exception ignored){}}}return a;
    }
    public JSONObject cardOverrides(){ensureOverrideCaches();JSONObject all=new JSONObject();for(Map.Entry<String,JSONObject> e:cardOverrideCache.entrySet())try{all.put(e.getKey(),copy(e.getValue()));}catch(Exception ignored){}return all;}
    public JSONObject detailOverrides(){ensureOverrideCaches();JSONObject all=new JSONObject();for(Map.Entry<String,JSONObject> e:detailOverrideCache.entrySet())try{all.put(e.getKey(),copy(e.getValue()));}catch(Exception ignored){}return all;}
    public JSONObject exportAll(){
        try{JSONObject o=new JSONObject();o.put("schemaVersion",2);o.put("reports",reports());o.put("cardOverrides",cardOverrides());o.put("detailOverrides",detailOverrides());return o;}catch(Exception e){return new JSONObject();}
    }

    public void importAll(JSONObject root, boolean replace){
        if(root==null)return;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            if(replace){db.delete("reports",null,null);db.delete("card_overrides",null,null);db.delete("detail_overrides",null,null);}
            JSONObject co=root.optJSONObject("cardOverrides");
            if(co!=null){Iterator<String> it=co.keys();while(it.hasNext()){String cardId=it.next();JSONObject x=co.optJSONObject(cardId);if(x==null)continue;ContentValues v=new ContentValues();v.put("card_id",cardId);v.put("question",x.optString("question"));JSONArray a=x.optJSONArray("bullets");v.put("bullets_json",a==null?"[]":a.toString());v.put("tip",x.optString("tip"));v.put("origin",x.optString("origin"));v.put("updated_at",x.optLong("updatedAt",System.currentTimeMillis()));db.insertWithOnConflict("card_overrides",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
            JSONObject dd=root.optJSONObject("detailOverrides");
            if(dd!=null){Iterator<String> it=dd.keys();while(it.hasNext()){String topic=it.next();JSONObject x=dd.optJSONObject(topic);if(x==null)continue;ContentValues v=new ContentValues();v.put("topic",topic);for(String k:new String[]{"example","name","simple","essay"})if(x.has(k))v.put(k,x.optString(k));v.put("updated_at",x.optLong("updatedAt",System.currentTimeMillis()));db.insertWithOnConflict("detail_overrides",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
            JSONArray rr=root.optJSONArray("reports");
            if(rr!=null){for(int i=0;i<rr.length();i++){JSONObject x=rr.optJSONObject(i);if(x==null)continue;ContentValues v=new ContentValues();v.put("report_id",x.optString("reportId",UUID.randomUUID().toString()));v.put("card_id",x.optString("cardId"));v.put("topic",x.optString("topic"));v.put("kind",x.optString("kind"));v.put("question",x.optString("question"));JSONArray a=x.optJSONArray("answerBullets");v.put("answer_json",a==null?"[]":a.toString());v.put("origin",x.optString("origin"));v.put("type",x.optString("type"));v.put("note",x.optString("note"));v.put("created_at",x.optLong("createdAt",System.currentTimeMillis()));v.put("status",x.optString("status","pending"));db.insertWithOnConflict("reports",null,v,SQLiteDatabase.CONFLICT_REPLACE);}}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();invalidateOverrideCaches();}
    }

    public String exportBundle(){
        try{JSONObject o=new JSONObject();o.put("schemaVersion",1);o.put("app","社会工作闪卡");o.put("appVersion","2.9.0");o.put("exportedAt",System.currentTimeMillis());o.put("usage","将此文件上传到 ChatGPT，可用于定位被标记的错误卡片和本地修订内容。应用本地数据库不会自动被 ChatGPT 读取。");o.put("reports",reports());o.put("cardOverrides",cardOverrides());o.put("detailOverrides",detailOverrides());return o.toString(2);}catch(Exception e){return "{}";}
    }
    public void clearFeedback(){SQLiteDatabase db=getWritableDatabase();db.delete("reports",null,null);db.delete("card_overrides",null,null);db.delete("detail_overrides",null,null);invalidateOverrideCaches();}
}
