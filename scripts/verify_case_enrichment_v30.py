#!/usr/bin/env python3
"""Offline case enrichment checks; provenance guard + original input retention."""
from pathlib import Path
import json, hashlib, statistics, csv
R=Path(__file__).resolve().parents[1]
case_path=R/'app/src/main/assets/case_bank_v30.json'
cases=json.loads(case_path.read_text('utf-8'))['cases']
assert len(cases)==86
ids=[c['id'] for c in cases];assert len(ids)==len(set(ids))
master={c['id'] for c in json.loads((R/'app/src/main/assets/cards.json').read_text('utf-8'))}
summary={'baseline':84,'trainingEnriched':0,'collectedOriginalPreserved':0,'publicInspiredTraining':0,'newIndependentQuestions':0,'caseCount':len(cases),'questionCount':0}
rows=[]
for c in cases:
    original=c['originalMaterial'];sha=hashlib.sha256(original.encode()).hexdigest()
    assert sha==c['originalMaterialSha256']
    assert c['linkedCardIds'] and set(c['linkedCardIds'])<=master
    assert c.get('examOriginalVerified') is False and c.get('canFullOriginalExam') is False
    assert len(c['subquestions'])==len({q['id'] for q in c['subquestions']})
    for i,q in enumerate(c['subquestions'],1):
        assert q['number']==i and q['question'] and q['answer'] and q['answerStatus'].startswith('训练参考解析')
    summary['questionCount']+=len(c['subquestions'])
    if c['sourceBatch']=='EXT-OFFICIAL':
        summary['publicInspiredTraining']+=1; summary['newIndependentQuestions']+=len(c['subquestions'])
        assert c['adaptationSources'] and all(s['url'].startswith('https://') for s in c['adaptationSources'])
        assert len(c['material'])>=650 and '非考研原题' in c['sourceLabel']
        note='依据公开机制改编；人物场景虚构'
    elif c['sourceLabel']=='汇编材料（原卷未核验）':
        summary['collectedOriginalPreserved']+=1
        assert c['material']==original
        assert c['defaultMaterialView']=='original_collected_text'
        assert '非试卷原文' in c['trainingExpansion']
        note='未核签的历史汇编材料；原文逐字保留'
    else:
        summary['trainingEnriched']+=1
        assert c['originalMaterial'] in c['material']
        assert c['material']==c['trainingExpandedMaterial']
        assert c['caseExpansionIsFiction'] is True
        assert len(c['material'])>=550
        note='新增原创训练情节；不是原卷'
    rows.append({'case_id':c['id'],'source_batch':c['sourceBatch'],'case_type':note,'original_char_count':len(original),'display_char_count':len(c['material']),'expanded_training_char_count':len(c.get('trainingExpandedMaterial',c['material'])),'original_sha256':sha,'subquestions':len(c['subquestions'])})
reference=json.loads((R/'docs/case_subquestion_176_original_sha256.json').read_text('utf-8'))
seen=set()
for c in cases:
    for q in c['subquestions']:
        key=c['id']+'/'+q['id']
        if key in reference:
            raw=json.dumps({'question':q['question'],'answer':q['answer'],'answerStatus':q['answerStatus']},sort_keys=True,ensure_ascii=False,separators=(',',':'))
            assert hashlib.sha256(raw.encode()).hexdigest()==reference[key],key+' original Q/A altered'
            seen.add(key)
assert seen==set(reference) and len(reference)==176,'baseline subquestions not fully preserved'
summary['originalSubquestionsPreserved']=len(seen)
assert summary['trainingEnriched']==78 and summary['collectedOriginalPreserved']==6 and summary['publicInspiredTraining']==2 and summary['questionCount']==182 and summary['newIndependentQuestions']==6
csvpath=R/'docs/case_enrichment_item_audit.csv'
with csvpath.open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
lengths=[r['display_char_count'] for r in rows if r['case_type']=='新增原创训练情节；不是原卷']
summary['trainingLengthMin']=min(lengths);summary['trainingLengthMedian']=statistics.median(lengths);summary['trainingLengthMax']=max(lengths)
summary['historicalOriginalShaCount']=84
summary['androidBuildRun']=False;summary['realExamSigned']=False;summary['independentProfessionalReviewPassed']=False
(R/'docs/case_enrichment_validation.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('PASS',summary)
