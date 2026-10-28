#!/usr/bin/env python3
"""V3.2 read-only regression gate; source selection, UI metadata, 331 subject isolation."""
import json,hashlib,re,sys,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[1]; A=ROOT/'app/src/main/assets'; D=ROOT/'docs'
errors=[]
def test(v,m):
    if not v:errors.append(m)
base=json.loads((A/'cards.json').read_text('utf-8-sig'))
v24=json.loads((A/'v24_437_378_cards.json').read_text('utf-8-sig'))['cards']
quick=json.loads((A/'quick_memory_v3.json').read_text('utf-8-sig'))
case=json.loads((A/'z_case_staging_v31.json').read_text('utf-8-sig'))['cards']
record=json.loads((D/'V32_active_answers_sha256.json').read_text('utf-8-sig'))
selected=json.loads((D/'V32_当前答案替换与展示规则.json').read_text('utf-8-sig'))
ids=[x['id'] for x in base]; mids=[x['id'] for x in v24]
test(ids==[f'C{i:04d}' for i in range(1,2498)],'original 2497 card IDs changed')
test(mids==[f'437M{i:03d}' for i in range(1,379)],'independent 378 card IDs changed')
test(not set(ids)&set(mids),'main and V24 collided')
test(set(ids)==set(record)==set(quick),'active source/memory card IDs drift')
test(not (A/'z_staging_overlays_v31.json').exists(),'old multi-version stage asset still packaged')

# Lean project: old overlay installers, retired scripts and large offline reference mirrors
# must live in the separate historical archive, not be committed back to Android repo.
for obsolete in ('V31_OVERLAY_MANIFEST.json','scripts/Apply-V31-Overlay.ps1',
                 'scripts/Apply-V32-Overlay.ps1','scripts/validate_v3_review.py',
                 'scripts/audit_answer_types_v2_9_0.py',
                 'content/references/reference_matching_2497_v2_9_0.json',
                 'content/v24_437_module/V24_437_378_full_record_source.json'):
    test(not (ROOT/obsolete).exists(), 'obsolete archived file returned: '+obsolete)

test(all(x['fullAnswer'].strip() and hashlib.sha256(x['fullAnswer'].encode('utf8')).hexdigest()==record.get(x['id']) for x in base),'active answer SHA mismatch')
test(all(x['bullets']==[x['fullAnswer']] for x in base),'old bullet answers still live')
test(all(x.get('answerEffectiveChars',0)>0 for x in base),'missing effective-word-count')
test(all(set(['framework','cues','example','mnemonic','recallQuestions']).issubset(q) for q in quick.values()),'quick memory schema invalid')
test(all(x['trainingScore']==10 and all(x['fiveDimensions'].get(k) for k in ['概念','特征','案例','评价','社工关联']) for x in v24),'V24 five dimensions or score invalid')
test(set(case).issubset(set(ids)),'case link missing')
test(sum(selected['sections_selected'].values())==2497,'Z active selection coverage invalid')
code=(ROOT/'app/src/main/java/com/seu/socialworkcards/MainActivity.java').read_text('utf8')
repo=(ROOT/'app/src/main/java/com/seu/socialworkcards/CardRepository.java').read_text('utf8')
engine=(ROOT/'app/src/main/java/com/seu/socialworkcards/MockExamEngine.java').read_text('utf8')
test('card.addView(modeBar);' not in code and 'TextView answerModeButton(' not in code,'bulky answer mode bar returned')
test('TextView toggle=tv(showMemory?' in code and 'renderCoreAnswer(result,x,previewMemory[0],v->' in code,'V3.1 compact answer/memory toggle missing')
test('View priority=sectionHeader("优先刷题"' not in code and 'View specialHeader=sectionHeader("437 专项独立模块"' not in code,'duplicated browser sections still on study home')
test('String[] shortcutLabels={"历年真题","真题关联","教材框架"}' in code,'library shortcuts were lost')
test('c.question=rawFront.replaceFirst(' in repo and 'c.kind="名词解释"' in repo,'437 term display prefix not normalized')
test('void renderCoreAnswer(LinearLayout inner,Card c)' in code and 'effectiveAnswerChars(answer)' in code,'unified active answer renderer missing')
test('if(repo.isV24(c))' in code and 'fiveDimensions' in code,'V24 not rendered through common answer component')
test('原卷分值独立保存' not in code and '切换候选/原版' not in code and '新内容仅供审阅' not in code,'technical audit metadata leaked into UI')
test('z_staging_overlays_v31.json' not in repo and 'stagedOptions' not in repo,'old candidate switching still exists')
test('if(isV24(c))return "437".equals(subject);' in repo,'V24 331 ban missing')
test('repo.matchesExamSubject(c,subject)&&typeCompatible(c,sec.targetType)' in engine,'mock exam initial hard guard missing')
test('if(used.contains(c.id)||!repo.matchesExamSubject(c,subject))continue' in engine,'mock exam fallback hard guard missing')
res={'ok':not errors,'main_cards':len(base),'v24_cards':len(v24),'current_answers':len(record),'memory_cards':len(quick),'cases_linked':len(case),'sources_used':selected['sections_selected'],'errors':errors,'apk_compiled':False,'device_ui_tested':False}
print(json.dumps(res,ensure_ascii=False,indent=2));sys.exit(0 if not errors else 1)
