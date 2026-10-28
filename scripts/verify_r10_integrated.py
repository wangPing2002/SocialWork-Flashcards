from pathlib import Path
import json,hashlib,re,collections
R=Path(__file__).resolve().parents[1]
A=R/'app/src/main/assets'
read=lambda n:json.loads((A/n).read_text(encoding='utf-8'))
p=read('z00_r10_exam_cards.json');cases=read('z00_r10_exam_cases.json');main=read('cards.json');v24=read('v24_437_378_cards.json')
meta=p['metadata'];cards=p['cards'];subs=p['subquestionCards'];missing=p['missingSlots'];recall=p['recallOnly']
errors=[]
assert len(main)==2497 and len(v24['cards'])==378
assert (len(cards),len(subs),len(cases['cases']),len(missing),len(recall))==(56,10,4,7,1)
assert len([c for c in cards if not c.get('isCaseGroup')])==52
assert len({c['id'] for c in cards})==56 and len({s['id'] for s in subs})==10
assert all(c['sourceModule']=='Z00_R10' and c['examSubjectScope'] in ('331','437') for c in cards)
assert not set(c['id'] for c in cards)&set(c['id'] for c in main)
assert not set(c['id'] for c in cards)&set(c['id'] for c in v24['cards'])
sha=lambda s:hashlib.sha256(s.encode('utf-8')).hexdigest()
def eff(s):return sum(x.isalnum() for x in re.sub(r'【[^】]*】','',re.sub(r'<[^>]*>','',s)))
for c in cards:
 assert sha(c['question'])==c['sourceQuestionSha256'],c['id']
 assert c['trainingScoreGate']=='AI_PROPOSED_NOT_SIGNED'
 assert c['sourceStatus']=='汇编题文·未核官方原卷'
 assert len(c['memory']['cues'])==3
 if not c.get('isCaseGroup'):
  assert sha(c['sourceAnswer'])==c['answerSha256']
  assert re.sub(r'<[^>]*>','',c['fullAnswer'])==c['sourceAnswer']
  assert '#C62828' in c['fullAnswer']
  assert eff(c['fullAnswer'])==c['answerEffectiveChars']
  assert c['trainingScore']*25<=c['answerEffectiveChars']<=c['trainingScore']*30
 else:assert c['mockEligible'] == (c['id'] in ('R10-047','R10-064'))
for s in subs:
 assert sha(s['sourceQuestionOriginal'])==s['sourceQuestionSha256']
 assert eff(s['answer'])==s['answerEffectiveChars']
 assert 25*s['trainingScore']<=s['answerEffectiveChars']<=30*s['trainingScore']
for g in cases['cases']:
 assert len(g['subquestions']) in (2,3)
 assert sha(g['originalCollectedQuestion'])==g['originalCollectedQuestionSha256']
 assert sha(g['originalMaterial'])==g['originalMaterialSha256']
 assert not g['examOriginalVerified'] and not g['canFullOriginalExam']
 assert len([s for s in subs if s['caseId']==g['id']])==len(g['subquestions'])
 assert all(s['r10SubcardId'] in {sc['id'] for sc in subs} for s in g['subquestions'])
assert all(re.sub(r'<[^>]*>','',q['answerDisplayHtml'])==q['answer'] for g in cases['cases'] for q in g['subquestions'])
assert all(len(q['memory']['cues'])==3 for g in cases['cases'] for q in g['subquestions'])
assert all(not c.get('mockEligible') for c in cards if c.get('isCaseGroup') and c['id'] in ('R10-031','R10-032'))
repo=(R/'app/src/main/java/com/seu/socialworkcards/CardRepository.java').read_text(encoding='utf-8')
mock=(R/'app/src/main/java/com/seu/socialworkcards/MockExamEngine.java').read_text(encoding='utf-8')
activity=(R/'app/src/main/java/com/seu/socialworkcards/MainActivity.java').read_text(encoding='utf-8')
casebank=(R/'app/src/main/java/com/seu/socialworkcards/CaseBank.java').read_text(encoding='utf-8')
for s in ('r10ExamCards','r10MockAllowed','subject.equals(c.examSubjectScope)','z00_r10_exam_cards.json','isR10('):assert s in repo,s
assert 'scoped.addAll(repo.r10ExamCards)' in mock
for s in ('quickMemoryStudyFor','realExamCards()','new ArrayList<>','训练分值：','String[] shortcutLabels={"历年真题","真题关联","教材框架"}') :assert s in activity
assert 'z00_r10_exam_cases.json' in casebank
print(json.dumps({'ok':True,'main':len(main),'v24':len(v24['cards']),
 'r10QuestionCards':len(cards),'r10SubquestionAnswers':len(subs),'r10TotalAnswerUnits':52+len(subs),
 'caseGroups':len(cases['cases']),'missingPreserved':len(missing),'recallOnly':len(recall),
 'r10MockEligible':sum(bool(c['mockEligible']) for c in cards),
 'sourceOfficialVerified':False,'apkCompiled':False,'deviceUiTested':False,'errors':errors},ensure_ascii=False,indent=2))
