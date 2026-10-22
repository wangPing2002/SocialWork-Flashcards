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
    }
    public JSONObject get(String caseId){return cases.get(caseId);}
    public List<JSONObject> forCard(String id){List<JSONObject> a=byCard.get(id);return a==null?Collections.emptyList():a;}
    public int size(){return cases.size();}
}
