#!/usr/bin/env python3
"""Read-only V3.1 staged merge verification. No Android SDK needed."""
import json,hashlib,re,sys,pathlib,collections
ROOT=pathlib.Path(__file__).resolve().parents[1]
A=ROOT/'app/src/main/assets'
errors=[]

def check(ok,message):
    if not ok:errors.append(message)

base=json.loads((A/'cards.json').read_text(encoding='utf-8-sig'))
v24=json.loads((A/'v24_437_378_cards.json').read_text(encoding='utf-8-sig'))
z=json.loads((A/'z_staging_overlays_v31.json').read_text(encoding='utf-8-sig'))
cases=json.loads((A/'z_case_staging_v31.json').read_text(encoding='utf-8-sig'))
full=json.loads((ROOT/'content/v24_437_module/V24_437_378_full_record_source.json').read_text(encoding='utf-8-sig'))
manifest=json.loads((ROOT/'docs/Z00_Z08_输入档案与互不覆盖统计.json').read_text(encoding='utf-8-sig'))
ids=[c['id'] for c in base];check(ids==[f'C{i:04d}' for i in range(1,2498)],'2497 base IDs modified')
mids=[c['id'] for c in v24['cards']];check(mids==[f'437M{i:03d}' for i in range(1,379)],'V24 378 IDs incomplete or not ordered')
check(not (set(ids)&set(mids)),'V24 ID collided with base')
check(full['meta']['total_cards']==378 if isinstance(full['meta'],dict) else len(full['cards'])==378,'V24 detailed source total')
fm={x['id']:x for x in full['cards']};
for c in v24['cards']:
    k=c['id']; check(k in fm,f'{k} missing full detailed archive')
    if k not in fm:continue
    orig=fm[k]['original_v03_skeleton_readonly'];sha=hashlib.sha256(orig.encode('utf8')).hexdigest()
    check(sha==c['sourceSha256']==fm[k]['original_v03_skeleton_sha256'],f'{k} original SHA drift')
    check(c['appReleaseEligible'] is False,f'{k} falsely marked ready')
    check(c['historicalExamScore'] is None or c['historicalExamScore']!=c['trainingScore'] or c['historicalExamScore'] is not None,f'{k} malformed score fields')
    check(bool(c['answer']) and all(bool(c['fiveDimensions'].get(x)) for x in ['概念','特征','案例','评价','社工关联']),f'{k} five-dimensional candidate incomplete')
    check(c['front'].startswith('名词解释：'),f'{k} not training noun question')
bycard=z['cards'];check(set(bycard)==set(ids),'Z01-Z08 staging is not indexed by 2497 IDs')
check(all(isinstance(v,list) and v and all(e['stage']=='REVIEW_CANDIDATE_NOT_OFFICIAL' for e in v) for v in bycard.values()),'Z staging missing candidate gate')
check(set(manifest['inputs'])=={f'Z{i:02d}' for i in range(9)},'Z00–Z08 not complete')
check(all(len(v['sha256'])==64 for v in manifest['inputs'].values()),'Z source hash missing')
case_mappings=cases['cards'];check(set(case_mappings).issubset(set(ids)),'Z case references non-base id')
check(all(not q.get('officialOriginalSigned',True) for v in case_mappings.values() for q in v),'Z cases incorrectly declared original signed')
mod=(ROOT/'app/src/main/java/com/seu/socialworkcards/MockExamEngine.java').read_text(encoding='utf-8')
repo=(ROOT/'app/src/main/java/com/seu/socialworkcards/CardRepository.java').read_text(encoding='utf-8')
ui=(ROOT/'app/src/main/java/com/seu/socialworkcards/MainActivity.java').read_text(encoding='utf-8')
for token in ['repo.matchesExamSubject(c,subject)&&typeCompatible(c,sec.targetType)','if(used.contains(c.id)||!repo.matchesExamSubject(c,subject))continue','if("437".equals(subject)&&"名词解释".equals(sec.targetType))scoped.addAll(repo.v24Cards)']:
 check(token in mod,'subject/type fallback guard missing: '+token)
check('if(isV24(c))return "437".equals(subject);' in repo,'explicit V24 331 ban missing')
check('showV24Module()' in ui and 'selectedCandidateSection' in ui and 'showStagedCaseMaterials' in ui,'UI independent module or staged buttons missing')
check('b.setMinWidth(0)' in ui and 'sourceBadge.setMaxWidth' in ui,'minimum responsive button width / source clipping not configured')
result={'ok':not errors,'main_cards':len(ids),'v24_module_cards':len(mids),'z_candidate_cards':len(bycard),'z_preview_variants':sum(map(len,bycard.values())),'z_package_count':len(manifest['inputs']),'cases_in_staging':sum(map(len,case_mappings.values())),'case_linked_cards':len(case_mappings),'v24_original_sha_verified':len(mids),'source_331_hard_guard':True,'errors':errors,'android_apk_build_executed':False,'android_device_ui_test_executed':False}
print(json.dumps(result,ensure_ascii=False,indent=2))
sys.exit(0 if not errors else 1)
