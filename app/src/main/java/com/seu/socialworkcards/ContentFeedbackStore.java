package com.seu.socialworkcards;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

/**
 * SQLite-backed local correction/feedback database.
 * Bundled cards remain immutable assets; user corrections live here as overrides.
 */
public class ContentFeedbackStore extends SQLiteOpenHelper {
    private static final String DB="content_feedback.db";
    private static final int VERSION=1;

    public ContentFeedbackStore(Context c){ super(c,DB,null,VERSION); }
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE card_overrides(card_id TEXT PRIMARY KEY, question TEXT, bullets_json TEXT, tip TEXT, origin TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE detail_overrides(topic TEXT PRIMARY KEY, example TEXT, name TEXT, simple TEXT, essay TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE reports(report_id TEXT PRIMARY KEY, card_id TEXT, topic TEXT, kind TEXT, question TEXT, answer_json TEXT, origin TEXT, type TEXT, note TEXT, created_at INTEGER, status TEXT)");
        db.execSQL("CREATE INDEX idx_reports_status ON reports(status)");
        db.execSQL("CREATE INDEX idx_reports_card ON reports(card_id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){}

    public void applyCardOverride(Card c){
        try(Cursor cur=getReadableDatabase().query("card_overrides",null,"card_id=?",new String[]{c.id},null,null,null)){
            if(!cur.moveToFirst())return;
            c.question=cur.getString(cur.getColumnIndexOrThrow("question"));
            c.tip=cur.getString(cur.getColumnIndexOrThrow("tip"));
            c.origin=cur.getString(cur.getColumnIndexOrThrow("origin"));
            JSONArray a=new JSONArray(cur.getString(cur.getColumnIndexOrThrow("bullets_json")));c.bullets.clear();for(int i=0;i<a.length();i++)c.bullets.add(a.optString(i));
        }catch(Exception ignored){}
    }

    public void saveCardOverride(Card c,String question,List<String> bullets,String tip,String origin){
        try{
            JSONArray a=new JSONArray();List<String> cleaned=new ArrayList<>();for(String x:bullets)if(x!=null&&!x.trim().isEmpty()){a.put(x.trim());cleaned.add(x.trim());}
            ContentValues v=new ContentValues();v.put("card_id",c.id);v.put("question",question);v.put("bullets_json",a.toString());v.put("tip",tip);v.put("origin",origin);v.put("updated_at",System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("card_overrides",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            c.question=question;c.tip=tip;c.origin=origin;c.bullets.clear();c.bullets.addAll(cleaned);
        }catch(Exception ignored){}
    }
    public boolean hasCardOverride(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM card_overrides WHERE card_id=? LIMIT 1",new String[]{id})){return c.moveToFirst();}}
    public void clearCardOverride(String id){getWritableDatabase().delete("card_overrides","card_id=?",new String[]{id});}

    public JSONObject mergeDetail(String topic,JSONObject base){
        JSONObject out;try{out=new JSONObject(base==null?"{}":base.toString());}catch(Exception e){out=new JSONObject();}
        try(Cursor c=getReadableDatabase().query("detail_overrides",null,"topic=?",new String[]{topic},null,null,null)){
            if(c.moveToFirst())for(String key:new String[]{"example","name","simple","essay"}){int i=c.getColumnIndex(key);if(i>=0&&!c.isNull(i))out.put(key,c.getString(i));}
        }catch(Exception ignored){}
        return out;
    }
    public void saveDetailOverride(String topic,String key,String value){
        if(!Arrays.asList("example","name","simple","essay").contains(key))return;
        SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("topic",topic);v.put(key,value);v.put("updated_at",System.currentTimeMillis());
        long n=db.update("detail_overrides",v,"topic=?",new String[]{topic});if(n==0)db.insert("detail_overrides",null,v);
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
    public JSONObject cardOverrides(){
        JSONObject all=new JSONObject();try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM card_overrides",null)){while(c.moveToNext()){try{JSONObject o=new JSONObject();o.put("question",c.getString(c.getColumnIndexOrThrow("question")));o.put("bullets",new JSONArray(c.getString(c.getColumnIndexOrThrow("bullets_json"))));o.put("tip",c.getString(c.getColumnIndexOrThrow("tip")));o.put("origin",c.getString(c.getColumnIndexOrThrow("origin")));o.put("updatedAt",c.getLong(c.getColumnIndexOrThrow("updated_at")));all.put(c.getString(c.getColumnIndexOrThrow("card_id")),o);}catch(Exception ignored){}}}return all;
    }
    public JSONObject detailOverrides(){
        JSONObject all=new JSONObject();try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM detail_overrides",null)){while(c.moveToNext()){try{JSONObject o=new JSONObject();for(String k:new String[]{"example","name","simple","essay"}){int i=c.getColumnIndex(k);if(i>=0&&!c.isNull(i))o.put(k,c.getString(i));}o.put("updatedAt",c.getLong(c.getColumnIndexOrThrow("updated_at")));all.put(c.getString(c.getColumnIndexOrThrow("topic")),o);}catch(Exception ignored){}}}return all;
    }
    public String exportBundle(){
        try{JSONObject o=new JSONObject();o.put("schemaVersion",1);o.put("app","社会工作闪卡");o.put("appVersion","2.1.0");o.put("exportedAt",System.currentTimeMillis());o.put("usage","将此文件上传到 ChatGPT，可用于定位被标记的错误卡片和本地修订内容。应用本地数据库不会自动被 ChatGPT 读取。");o.put("reports",reports());o.put("cardOverrides",cardOverrides());o.put("detailOverrides",detailOverrides());return o.toString(2);}catch(Exception e){return "{}";}
    }
    public void clearFeedback(){SQLiteDatabase db=getWritableDatabase();db.delete("reports",null,null);db.delete("card_overrides",null,null);db.delete("detail_overrides",null,null);}
}
