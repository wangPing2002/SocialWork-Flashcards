#!/usr/bin/env python3
"""V3.2.1 UI/metadata-only audit, does not edit source assets."""
from pathlib import Path
import json,re,hashlib,sys,math
root=Path(__file__).resolve().parents[1]
a=root/'app/src/main/assets';j=root/'app/src/main/java/com/seu/socialworkcards'
errors=[]
def check(t,m):
    if not t:errors.append(m)
main=json.loads((a/'cards.json').read_text('utf-8'))
v24=json.loads((a/'v24_437_378_cards.json').read_text('utf-8'))['cards']
r10=json.loads((a/'z00_r10_exam_cards.json').read_text('utf-8'))['cards']
case=json.loads((a/'z00_r10_exam_cases.json').read_text('utf-8'))['cases']
training=json.loads((a/'training_score_suggestions_v321.json').read_text('utf-8'))
s=training['scores'];ids={c['id'] for c in main}
check(len(main)==2497 and len(v24)==378 and len(r10)==56 and len(case)==4,'protected card counts changed')
check(len(s)==2497 and set(s)==ids,'2497 training scores not fully mapped')
check(training['status']=='AI_CALIBRATED_NOT_SIGNED','automatically suggested scores not marked unsigned')
for c in main:
    x=s.get(c['id'],{})
    n=c['answerEffectiveChars']; pts=x.get('trainingScore',0)
    check(x.get('answerEffectiveChars')==n and pts>0 and 25*pts<=n<=30*pts,'score audit mismatch '+c['id'])
for c in r10:
    if c.get('isCaseGroup'):continue
    check(c.get('trainingScore',0)>0 and 25*c['trainingScore']<=c['answerEffectiveChars']<=30*c['trainingScore'],'R10 training issue '+c['id'])
for x in case:
    for q in x['subquestions']:
        check(q.get('trainingScore',0)>0 and 25*q['trainingScore']<=q.get('answerEffectiveChars',0)<=30*q['trainingScore'],'case subscore '+x['id']+'/'+q['id'])
activity=(j/'MainActivity.java').read_text('utf-8');repo=(j/'CardRepository.java').read_text('utf-8')
check('String[] shortcutLabels={"历年真题","真题关联","教材框架"}' in activity,'redundant R10 browsing entry still visible')
check('String[] shortcutLabels={"历年真题","新题补录"' not in activity,'new question duplicate entry still present')
check('out.addAll(repo.r10ExamCards)' in activity,'consolidated exam list dropped R10 cards')
check('if(isR10(c))return cleanR10DisplayNumber(c.question);' in repo,'R10 display-only numbering normalization absent')
check('return c!=null&&!isR10(c)&&!displayQuestion(c).equals(c.question);' in repo,'R10 numbering stripped caused false training rewrite badge')
check('c.trainingScoreSuggested=true;' in repo,'main score provenance not marked as provisional')
check('training_score_suggestions_v321.json' in repo,'training mapping not loaded')
check('c.trainingScoreSuggested?' in activity and '训练分值：' in activity,'training UI not unified')
check('int subTraining=q.optInt("trainingScore",0);' in activity,'case subquestion training scores not displayed')
check('TextView toggle=tv(showMemory?' in activity,'compact answer toggle lost')
check('if(isV24(c))return "437".equals(subject);' in repo,'331 isolation removed')
pattern=re.compile(r'^\s*(?:[0-9]{1,2}[.．、)）]|[(（][0-9]{1,2}[)）]|[一二三四五六七八九十]{1,3}[、.．])\s*')
normalizations=sum(pattern.sub('',c['question']).strip()!=c['question'] for c in r10)
check(normalizations==53,'source prefix display normalization count changed: '+str(normalizations))
check(all(c['question']==c['sourceQuestionOriginal'] for c in r10),'R10 source question replaced')
check(all(hashlib.sha256(c['question'].encode('utf-8')).hexdigest()==c['sourceQuestionSha256'] for c in r10),'R10 question SHA mismatch')
results={'ok':not errors,'main':len(main),'v24':len(v24),'r10Records':len(r10),'r10DisplayPrefixesRemoved':normalizations,'scoreSuggestions':len(s),'caseGroups':len(case),'answerTextsModified':False,'errors':errors,'apkCompiled':False,'deviceUiTested':False}
print(json.dumps(results,ensure_ascii=False,indent=2));sys.exit(0 if not errors else 1)
