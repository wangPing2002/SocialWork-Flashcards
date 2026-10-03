from pathlib import Path
import re, json, collections, argparse

parser=argparse.ArgumentParser(description='Import the C0001-C2497 markdown master into Android card assets.')
parser.add_argument('master',help='Path to the markdown master file')
parser.add_argument('--project-root',default=str(Path(__file__).resolve().parents[1]),help='SocialWork-Flashcards project root')
args=parser.parse_args()
SRC=Path(args.master).resolve()
ROOT=Path(args.project_root).resolve()
ASSETS=ROOT/'app/src/main/assets'
text=SRC.read_text(encoding='utf-8')

matches=list(re.finditer(r'^###\s+C(\d+)\s*$', text, re.M))
assert len(matches)==2497

PRACTICE_KEYS=['老年','儿童','青少年','妇女','残疾','医务','医院','矫正','司法','救助','社会救助','农村','企业','学校','家庭社会工作','家庭服务','退役','新就业','灾害','防灾','基层治理','信访','临终','照顾者','照护','养老','贫困','就业','优抚','流浪','社区康复']
MANAGE_KEYS=['督导','社会工作行政','服务管理','机构管理','社会服务机构','项目管理','项目评估','项目策划','志愿者管理','筹资','财务管理','预算','质量管理','质量控制','绩效','组织管理','机构评估','需求评估']
CASE_KEYS=['个案工作','个案管理','接案','专业关系','任务中心','危机介入','危机干预','理性情绪','认知行为','行为治疗','心理社会治疗','叙事治疗','人本治疗','结构家庭治疗','家庭治疗','精神分析','会谈技巧','个案服务']
GROUP_KEYS=['小组工作','小组动力','小组模式','小组阶段','小组领导','小组沟通','小组冲突','小组治疗','小组社会工作']
COMMUNITY_KEYS=['社区工作','地区发展','社会策划','社区照顾','社区教育','社区组织','社区领袖','社区参与','社区发展','社区行动','社区工作者']

def category_for(q, source):
    s=q+' '+source
    if any(k in s for k in MANAGE_KEYS): return '督导与管理'
    if any(k in s for k in GROUP_KEYS): return '小组工作'
    if any(k in s for k in COMMUNITY_KEYS): return '社区工作'
    if any(k in s for k in CASE_KEYS): return '个案工作'
    if any(k in s for k in PRACTICE_KEYS): return '社会工作实务'
    return '社会工作原理'

def topic_for(q):
    x=q.strip().rstrip('。？?；;')
    # Remove common command prefixes without changing substantive wording in question field.
    x=re.sub(r'^(请)?(简述|试述|论述|试论|解释|比较|分析|说明|评价|谈谈|概述|阐述)', '', x).strip('：:，, ')
    if len(x)<=28: return x
    for sep in ['，',',','；',';','？','?','：',':']:
        p=x.find(sep)
        if 8<=p<=28: return x[:p]
    return x[:28]+'…'

def clean_plain(s):
    if not s: return ''
    s=re.sub(r'<span\s+style=["\']color\s*:\s*#(?:C62828|D32F2F);?["\']\s*>','',s,flags=re.I)
    s=s.replace('</span>','')
    s=re.sub(r'\*\*([^*]+)\*\*',r'\1',s)
    s=re.sub(r'[`#>]+','',s)
    s=re.sub(r'\s+',' ',s).strip()
    return s

def core_bullets(answer):
    xs=re.findall(r'<span\s+style=["\']color\s*:\s*#(?:C62828|D32F2F);?["\']\s*>(.*?)</span>',answer,flags=re.I|re.S)
    out=[]
    for x in xs:
        x=clean_plain(x)
        if x and x not in out:
            out.append(x)
        if len(out)>=12: break
    if not out:
        # fallback first non-empty substantive lines
        for line in answer.splitlines():
            x=clean_plain(line).lstrip('（0123456789一二三四五六七八九十、.） ')
            if len(x)>=12 and x not in out:
                out.append(x)
            if len(out)>=5: break
    return out

def extract_fields(pre):
    fields={}
    for line in pre.splitlines():
        m=re.match(r'^-\s+\*\*(.+?)\*\*：\s*(.*)$',line)
        if m: fields[m.group(1).strip()]=m.group(2).strip()
    return fields

def parse_exam_sources(label):
    exact=[]; recall=[]
    if not label: return exact,recall
    # split into segments so each source keeps exact/recall status
    segs=re.split(r'[；;]', label)
    for seg in segs:
        ms=re.findall(r'(20\d{2})·(331|437)',seg)
        for y,s in ms:
            val=f'{y} {s}'
            if '🟨' in seg or '回忆版' in seg or '主题' in seg:
                if val not in recall: recall.append(val)
            else:
                if val not in exact: exact.append(val)
    return exact,recall


def norm_question(s):
    s=re.sub(r'[\s，,。；;、：:？！?（）()《》“”"\'·—\-]','',s or '')
    s=re.sub(r'^(请)?(简述|试述|论述|试论|解释|比较|分析|说明|评价|谈谈|概述|阐述)','',s)
    return s

def order_exam_sources(sources, truth_text, question):
    q=norm_question(question)
    scored=[]
    for pos,src in enumerate(sources):
        y,sub=src.split(' ')
        patt=re.compile(re.escape(y+'·'+sub)+r'[：:]([^｜|；;]+)')
        m=patt.search(truth_text or '')
        tq=norm_question(m.group(1) if m else '')
        score=0
        if tq and q==tq: score=10000
        elif tq and (q in tq or tq in q): score=5000+min(len(q),len(tq))
        else:
            score=sum(1 for ch in q if ch in set(tq)) if tq else 0
        scored.append((-score,pos,src))
    scored.sort()
    return [x[2] for x in scored]

def normalize_type(t,q):
    if '名词' in t: return '名词解释'
    if '简答' in t: return '简答题'
    if '论述' in t: return '论述题'
    if '案例' in t: return '案例分析题'
    if '材料' in t: return '材料题'
    # 2022 recall themes: infer conservatively from short concept-like wording
    if len(q)<=24: return '名词解释'
    return '简答题'

def rel_ids(v):
    return [x for x in re.findall(r'C\d{4}',v or '')]

cards=[]
for idx,m in enumerate(matches):
    cid='C'+m.group(1).zfill(4)
    start=m.end(); end=matches[idx+1].start() if idx+1<len(matches) else len(text)
    body=text[start:end].strip()
    am=re.search(r'^####\s+标准答案\s*$',body,re.M)
    assert am, cid
    fields=extract_fields(body[:am.start()])
    post=body[am.end():].strip()
    answer_lines=[]; meta_lines=[]; meta_started=False
    for line in post.splitlines():
        if re.match(r'^-\s+\*\*(核心教材依据|补充资料依据|答案来源层级|答案状态)\*\*：',line): meta_started=True
        (meta_lines if meta_started else answer_lines).append(line)
    meta=extract_fields('\n'.join(meta_lines))
    answer='\n'.join(answer_lines).strip()
    q=fields.get('真题化问题','').strip()
    typ=normalize_type(fields.get('题型',''),q)
    exact,recall=parse_exam_sources(fields.get('真题标注',''))
    exact=order_exam_sources(exact,fields.get('历年真题原题/主题',''),q)
    recall=order_exam_sources(recall,fields.get('历年真题原题/主题',''),q)
    origin=meta.get('核心教材依据','')
    if meta.get('补充资料依据'):
        origin += ('；' if origin else '')+'补充资料：'+meta['补充资料依据']
    card={
        'id':cid,
        'topic':topic_for(q),
        'category':category_for(q,fields.get('题目来源','')),
        'kind':typ,
        'questionType':typ,
        'question':q,
        'bullets':core_bullets(answer),
        'tip':'按题型组织完整考研主观题答案；先回忆核心得分点，再展开论证。',
        'hasExample':False,
        'hasLong':True,
        'origin':origin or fields.get('题目来源',''),
        'sourceField':fields.get('题目来源',''),
        'fullAnswer':answer,
        'predictedFrequency':'高' if fields.get('预测考频')=='高' else ('低' if fields.get('预测考频')=='低' else '中'),
        'predictedFrequencyRaw':fields.get('预测考频','中'),
        'relatedIds':rel_ids(fields.get('关联问题','')),
        'truthLabel':fields.get('真题标注',''),
        'truthQuestion':fields.get('历年真题原题/主题',''),
        'truthEvidence':fields.get('真题证据',''),
        'truthRelation':fields.get('真题关联',''),
        'answerStatus':meta.get('答案状态',''),
        'sourceLevel':meta.get('答案来源层级',''),
        'exactExamSources':exact,
        'recallExamSources':recall,
        'contentVersion':'2.8.0',
        'verified':meta.get('答案状态','').startswith('✅') or bool(answer),
    }
    cards.append(card)

assert len(cards)==2497 and len({c['id'] for c in cards})==2497
assert cards[0]['id']=='C0001' and cards[-1]['id']=='C2497'
assert all(c['question'] and c['fullAnswer'] for c in cards)

ASSETS.mkdir(parents=True,exist_ok=True)
(ASSETS/'cards.json').write_text(json.dumps(cards,ensure_ascii=False,indent=2),encoding='utf-8')
# Legacy assets retained as valid empty objects; V2.8 reads card-level metadata directly.
for fn in ['details.json','exam_answers.json','exam_meta.json']:
    (ASSETS/fn).write_text('{}\n',encoding='utf-8')

ids={c['id'] for c in cards}
invalid_related=sum(1 for c in cards for r in c.get('relatedIds',[]) if r not in ids)
qa={
 'version':'2.8.0',
 'sourceFile':SRC.name,
 'cards':len(cards),
 'idRange':[cards[0]['id'],cards[-1]['id']],
 'typeCounts':dict(collections.Counter(c['questionType'] for c in cards)),
 'categoryCounts':dict(collections.Counter(c['category'] for c in cards)),
 'frequencyCounts':dict(collections.Counter(c['predictedFrequency'] for c in cards)),
 'exactExamCards':sum(bool(c['exactExamSources']) for c in cards),
 'recallThemeCards':sum(bool(c['recallExamSources']) for c in cards),
 'truthLabelCards':sum(bool(c.get('truthLabel')) for c in cards),
 'answersWithC62828':sum('#C62828' in c['fullAnswer'] for c in cards),
 'answerWarningCards':sum(not c['answerStatus'].startswith('✅') for c in cards),
 'relatedLinkCount':sum(len(c.get('relatedIds',[])) for c in cards),
 'invalidRelatedLinks':invalid_related,
 'legacyCardIdsRemaining':sum(c['id'].startswith('m') for c in cards),
 'checks':{
   'all2497':len(cards)==2497,
   'allHaveQuestion':all(bool(c['question']) for c in cards),
   'allHaveAnswer':all(bool(c['fullAnswer']) for c in cards),
   'allHaveFrequency':all(c['predictedFrequency'] in {'高','中','低'} for c in cards),
   'uniqueIds':len({c['id'] for c in cards})==2497,
   'allRelatedIdsResolve':invalid_related==0,
   'noLegacyMIds':all(not c['id'].startswith('m') for c in cards),
   'allAnswerMarkupBalanced':all(c['fullAnswer'].count('<span')==c['fullAnswer'].count('</span>') for c in cards),
 }
}
(ROOT/'content/ui_qa_v2_8_0.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(qa,ensure_ascii=False,indent=2))
