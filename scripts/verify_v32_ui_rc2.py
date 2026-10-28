#!/usr/bin/env python3
"""Read-only V3.2 RC2 UI structure regression."""
from pathlib import Path
import json,hashlib,re,sys
root=Path(__file__).resolve().parents[1]
main=(root/'app/src/main/java/com/seu/socialworkcards/MainActivity.java').read_text('utf-8')
repo=(root/'app/src/main/java/com/seu/socialworkcards/CardRepository.java').read_text('utf-8')
v24=json.loads((root/'app/src/main/assets/v24_437_378_cards.json').read_text('utf-8'))['cards']
errs=[]
def check(x,m):
 if not x: errs.append(m)
check(len(v24)==378,'V24 not 378')
check(all(x['front'].startswith('名词解释：') for x in v24),'unexpected raw V24 front; display normalizer requires review')
check('c.question=rawFront.replaceFirst(' in repo,'437 title prefix not normalized at display')
check('card.addView(modeBar);' not in main,'bulky answer buttons still in learning card')
check('TextView toggle=tv(showMemory?' in main,'original compact toggle missing')
check('result.removeAllViews();' in main and 'renderCoreAnswer(result,x,previewMemory[0],v->' in main,'related-card toggle not unified')
home=main.split('void showHome(){',1)[1].split('View miniStat(',1)[0]
check('优先刷题' not in home and '教材系统题库' not in home and '437 专项独立模块' not in home,'duplicate browse sections in learning home')
check('随机生成模拟卷' in main and '开始今日学习' in home,'learning and mock entry missing')
check('if(isV24(c))return "437".equals(subject);' in repo,'331 guard changed')
print(json.dumps({'ok':not errs,'v24_cards':len(v24),'bulky_mode_bar':False,'study_home_browse_duplicates':False,'errors':errs},ensure_ascii=False,indent=2))
sys.exit(bool(errs))
