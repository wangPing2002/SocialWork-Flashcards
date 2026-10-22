#!/usr/bin/env python3
"""V3.0.0 offline static/data and state-model checks. Does not claim APK/expert verification."""
from pathlib import Path
import json,hashlib,collections,random,sys
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'; DOC=ROOT/'docs'
def asset(name):return json.loads((AS/name).read_text(encoding='utf-8'))
def check(cond,msg):
    if not cond:raise AssertionError(msg)
checks=0
def verify(cond,msg):
    global checks;check(cond,msg);checks+=1
cards=asset('cards.json'); ids={x['id']:x for x in cards}
verify(len(cards)==2497 and len(ids)==2497,'G1: 2497 unique card identities')
for i in range(1,2498):verify(f'C{i:04d}' in ids,'G1: sequential ids')
oldhash=json.loads((DOC/'answer_sha256_original.json').read_text(encoding='utf-8'))
for cid,c in ids.items():verify(hashlib.sha256(c.get('fullAnswer','').encode()).hexdigest()==oldhash.get(cid),f'G7: official answer changed: {cid}')
quick=asset('quick_memory_v3.json');verify(set(quick)==set(ids),'G1: memory alignment')
for cid,q in quick.items():
    verify(bool(q.get('framework')) and bool(q.get('cues')) and bool(q.get('mnemonic')), f'memory schema missing {cid}')

tax=asset('topic_taxonomy_v30.json'); grouping=tax['groups'];mapping=tax['cardTopics'];verify(set(mapping)==set(ids),'taxonomy card ids')
all_topics=[t for a in grouping.values() for t in a]; verify(len(set(all_topics))==len(all_topics),'topic duplication')
for cid,topic in mapping.items():
    verify(bool(topic['secondaryTopic']) and topic['primaryTopic'] in all_topics,f'taxonomy orphan {cid}')
    verify(topic['secondaryTopic']==ids[cid]['secondaryTopic'],f'unexpected legacy secondary edit {cid}')
verify(len(grouping)==6,'six conceptual families')

overlay=asset('question_overlay_v30.json')
for cid,r in overlay.items():
    verify(cid in ids and r['baselineQuestion']==ids[cid]['question'],f'question baseline mismatch {cid}')
    verify(oldhash[cid]==r['baselineAnswerSha256'],f'answer overlay SHA mismatch {cid}')
    if 'trainingQuestion' in r:
        verify(len(r['trainingQuestion'])>=12 and r['trainingQuestion']!=ids[cid]['question'],f'question changed without gain {cid}')
        verify(not ids[cid].get('exactExamSources') and not ids[cid].get('recallExamSources'),f'original exam overwritten {cid}')

cases=asset('case_bank_v30.json')['cases']; case_ids=set(); sub_ids=set();counts=collections.Counter()
for case in cases:
    cid=case['id'];verify(cid not in case_ids,f'duplicate case {cid}');case_ids.add(cid)
    verify(not case['examOriginalVerified'] and not case['canFullOriginalExam'],f'G2 unverified case promoted {cid}')
    verify(len(case['material'])>=60 and len(case['subquestions'])>0,f'G3 partial material used {cid}')
    verify(case['sourceLabel'] in ('原创强化训练案例（非历年原题）','汇编材料（原卷未核验）','依据最高人民法院典型案例要素改编（非考研原题）','依据最高检与中央社会工作部典型案例机制改编（非考研原题）'),f'case provenance {cid}')
    for item in case['linkedCardIds']:verify(item in ids,f'case to nonexisting card {cid}/{item}')
    for i,q in enumerate(case['subquestions'],1):
        qid=cid+'/'+q['id'];verify(qid not in sub_ids,f'duplicate subquestion {qid}');sub_ids.add(qid)
        verify(q['number']==i and bool(q['question'].strip()) and bool(q['answer'].strip()),f'case subquestion invalid {qid}')
        verify(q['answerStatus'].startswith('训练参考解析'),f'not labeled candidate {qid}')
    old=case.get('originalMaterial');verify(old and hashlib.sha256(old.encode()).hexdigest()==case.get('originalMaterialSha256'),f'original case snapshot hash: {cid}')
    if case['sourceBatch']=='EXT-OFFICIAL':
        verify(len(case['material'])>=650 and len(case.get('adaptationSources',[]))>0,f'official-inspired case sources: {cid}')
    elif case['sourceLabel']=='汇编材料（原卷未核验）':
        verify(case['material']==old and case.get('defaultMaterialView')=='original_collected_text',f'original case protected: {cid}')
        verify(len(case['trainingExpansion'])>150,f'optional fiction expansion missing: {cid}')
    else:
        verify(case['material']==case['trainingExpandedMaterial'] and case.get('caseExpansionIsFiction'),f'enriched text integrity: {cid}')
        verify(len(case['material'])>=550,f'training scenario too short: {cid}')
        verify(old in case['material'],f'training baseline omitted: {cid}')
    counts[case['sourceBatch']]+=1
verify(len(cases)==86 and len(sub_ids)==182,'case bank totals')
verify(sum(v for v in counts.values())==86,'case source index alignment')
# Guard against lost original case text: source compilation of Z01 and training material of Z06 may never be blank.
for marker in ('SEU-2019-437-CASE-01','SEU-2020-437-CASE-01','SEU-2021-437-CASE-01'):
    case=next(c for c in cases if c['id']==marker)
    verify(case['sourceLabel']=='汇编材料（原卷未核验）',f'2020+ original case evidence overclaimed {marker}')

java=(ROOT/'app/src/main/java/com/seu/socialworkcards/MainActivity.java').read_text(encoding='utf-8')
repo=(ROOT/'app/src/main/java/com/seu/socialworkcards/CardRepository.java').read_text(encoding='utf-8')
casejava=(ROOT/'app/src/main/java/com/seu/socialworkcards/CaseBank.java').read_text(encoding='utf-8')
for label,checkstr in {'search-type':'typeMode[0]','search-results':'typeMatches(c,typeMode[0])','case-reading':'void showCase(String caseId,boolean reset)','case-grading':'case_v31_ratings','case-draft':'case_v31_drafts','draft-autosave':'draft.addTextChangedListener','exam-sim-downgrade':'整题模拟（训练版，不是原卷）','case-back-navigation':'if("case".equals(currentScreen))','explicit-reveal':'揭晓本问解析','case-on-card':'openCaseFromCard(c)'}.items():verify(checkstr in java,f'UI static feature {label}')
verify('displayQuestion(Card c)' in repo and 'baselineQuestion' in repo,'training question guard')
verify('originalCaseMaterialViews' in java and '切换：阅读补充训练情境' in java and 'adaptationSources' in java,'case source/expansion switch static check')
verify('Duplicate case:' in casejava and 'linkedCardIds' in casejava,'case repository integrity')
# Deterministic user-state-model test, no Android runtime involved.
rng=random.Random(20261022);drafts={};ratings={};reveals=set();orig_answers={k:v['fullAnswer'] for k,v in ids.items()}
keys=list(sub_ids)
for step in range(1200):
    qid=rng.choice(keys)
    action=rng.randrange(6)
    if action==0:drafts[qid]='草稿:'+str(step)
    elif action==1:reveals.add(qid)
    elif action==2:reveals.discard(qid)
    elif action==3:ratings[qid]='本问掌握'
    elif action==4:ratings[qid]='仍需练习'
    else:verify(drafts.get(qid,'')==drafts.get(qid,'') and all(k in sub_ids for k in ratings),'simulation state keys')
verify(all(ids[k]['fullAnswer']==a for k,a in orig_answers.items()),'case rate does not mutate mother answers')
verify(all(k in sub_ids for k in drafts) and all(k in sub_ids for k in ratings),'draft/rating identity isolation')

stats={'status':'PASS (offline only)','checks':checks,'sourceCards':len(cards),'sourceMemory':len(quick),'questionOverlays':len(overlay),'trainingRewrites':sum('trainingQuestion' in x for x in overlay.values()),'cases':len(cases),'subquestions':len(sub_ids),'caseSources':dict(counts),'taxonomyGroups':len(grouping),'taxonomyTopics':len(all_topics),'draftRatingModelSteps':1200,'apkBuildExecuted':False,'AndroidRuntimeExecuted':False,'officialPaperSigned':False,'independentProfessionalReviewPassed':False,'unresolved':json.loads((DOC/'integration_evidence.json').read_text(encoding='utf-8'))['incompleteCases']}
(DOC/'v30_validation_result.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding='utf-8')
print('PASS',checks,'offline integrity/static/state-model assertions',len(cases),'cases',len(sub_ids),'subquestions',len(overlay),'overlay entries')
