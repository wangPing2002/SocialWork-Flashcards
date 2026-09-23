package com.seu.socialworkcards;

import android.content.Context;
import org.json.JSONObject;

/**
 * Full local backup/restore for learning progress and content feedback.
 * The bundled card assets are not duplicated in the backup; only user state is stored.
 */
public class DataBackupManager {
    private final StudyStore study;
    private final ContentFeedbackStore feedback;

    public DataBackupManager(Context context, StudyStore study, ContentFeedbackStore feedback){
        this.study=study;
        this.feedback=feedback;
    }

    public String exportAll(){
        try{
            JSONObject root=new JSONObject();
            root.put("backupSchemaVersion",1);
            root.put("app","社会工作闪卡");
            root.put("appVersion","2.2.0");
            root.put("applicationId","com.seu.socialworkcards");
            root.put("exportedAt",System.currentTimeMillis());
            root.put("study",study.exportJson());
            root.put("contentFeedback",feedback.exportAll());
            root.put("note","包含学习进度、最近10次记录、每日学习量、未完成会话、本地修正和内容反馈。可用于换机或误卸载后的恢复。");
            return root.toString(2);
        }catch(Exception e){
            return "{}";
        }
    }

    public void importAll(String raw, boolean replace) throws Exception {
        JSONObject root=new JSONObject(raw);
        int schema=root.optInt("backupSchemaVersion",0);
        if(schema<1) throw new IllegalArgumentException("无法识别的备份格式");
        JSONObject studyJson=root.optJSONObject("study");
        JSONObject feedbackJson=root.optJSONObject("contentFeedback");
        if(studyJson==null && feedbackJson==null) throw new IllegalArgumentException("备份中没有可恢复的数据");
        if(studyJson!=null) study.importJson(studyJson,replace);
        if(feedbackJson!=null) feedback.importAll(feedbackJson,replace);
    }
}
