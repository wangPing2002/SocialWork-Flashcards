package com.seu.socialworkcards;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Independent training case material; neither verified exam scripts nor legacy Card answers are mutated. */
public final class CaseBank {
    private final Map<String,JSONObject> cases = new LinkedHashMap<>();
    private final Map<String,List<JSONObject>> byCard = new HashMap<>();
    public CaseBank(Context context) throws Exception {
        JSONObject payload=new JSONObject(CardRepository.readAsset(context,"case_bank_v30.json"));
        JSONArray data=payload.getJSONArray("cases");
        for(int i=0;i<data.length();i++){
            JSONObject c=data.getJSONObject(i);String id=c.getString("id");
            if(id.trim().isEmpty()||cases.containsKey(id))throw new IllegalStateException("Duplicate case: "+id);
            cases.put(id,c);
            JSONArray links=c.optJSONArray("linkedCardIds");
            if(links!=null)for(int j=0;j<links.length();j++){
                String cardId=links.optString(j);
                if(!cardId.isEmpty())byCard.computeIfAbsent(cardId,key->new ArrayList<>()).add(c);
            }
        }
        // R10独立题组：案例主档连到补录卡，原材料不向历史主档回填。
        JSONObject r10=new JSONObject(CardRepository.readAsset(context,"z00_r10_exam_cases.json"));
        JSONArray extra=r10.getJSONArray("cases");
        if(extra.length()!=4)throw new IllegalStateException("R10复合案例数量异常");
        for(int i=0;i<extra.length();i++){
            JSONObject c=extra.getJSONObject(i);String id=c.getString("id");
            if(cases.containsKey(id))throw new IllegalStateException("R10题组ID冲突："+id);
            cases.put(id,c);JSONArray links=c.getJSONArray("linkedCardIds");
            for(int j=0;j<links.length();j++){
                String cardId=links.optString(j);
                if(!cardId.isEmpty())byCard.computeIfAbsent(cardId,key->new ArrayList<>()).add(c);
            }
        }
    }
    public JSONObject get(String caseId){return cases.get(caseId);}
    public List<JSONObject> forCard(String id){List<JSONObject> a=byCard.get(id);return a==null?Collections.emptyList():a;}
    public int size(){return cases.size();}
}
