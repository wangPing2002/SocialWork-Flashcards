import json,re,os
from collections import OrderedDict, Counter
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CARDS=os.path.join(ROOT,'app','src','main','assets','cards.json')
OUT=os.path.join(ROOT,'app','src','main','assets','service_framework_v2_8_5.json')
REPORT=os.path.join(ROOT,'content','service_framework_report_v2_8_5.md')
QA=os.path.join(ROOT,'content','service_framework_qa_v2_8_5.json')
cards=json.load(open(CARDS,encoding='utf-8'))
byid={c['id']:c for c in cards}

def text(c, full=False):
    parts=[c.get('topic',''),c.get('question',''),c.get('primaryTopic',''),c.get('secondaryTopic','')]
    parts+=c.get('bullets',[])
    if full: parts.append(c.get('fullAnswer',''))
    return ' '.join(parts).lower()

def has(c,*terms):
    t=text(c)
    return any(x.lower() in t for x in terms)

def allhas(c,*terms):
    t=text(c)
    return all(x.lower() in t for x in terms)

def primary(c,p): return c.get('primaryTopic')==p

def secondary(c,*subs): return c.get('secondaryTopic') in subs

def ids(pred): return [c['id'] for c in cards if pred(c)]

def uniq(seq):
    seen=set();out=[]
    for x in seq:
        if x in byid and x not in seen: seen.add(x);out.append(x)
    return out

def node(id_,title,core=None,related=None,children=None,note=''):
    core=uniq(core or [])
    related=uniq([x for x in (related or []) if x not in set(core)])
    d=OrderedDict(id=id_,title=title,note=note,coreIds=core,relatedIds=related)
    if children: d['children']=children
    return d

# Common pools
P={p:ids(lambda c,p=p: primary(c,p)) for p in sorted({c.get('primaryTopic','') for c in cards})}
S={}
for c in cards:S.setdefault(c.get('secondaryTopic',''),[]).append(c['id'])

def keyword_ids(*terms): return ids(lambda c:has(c,*terms))
def keyword_all(*terms): return ids(lambda c:allhas(c,*terms))

def qs(*terms):
    out=[]
    for c in cards:
        t=(c.get('topic','')+' '+c.get('question','')).lower()
        if any(x.lower() in t for x in terms):out.append(c['id'])
    return out

# ---------- Elements ----------
client_form_core=uniq(qs('服务对象的存在形式','服务对象有哪些存在形式','个人、家庭、群体、组织','个人、家庭、群体、组织和社区')+['C0018','C0020'])
client_form_related=uniq(P.get('重点人群社会工作',[])+qs('服务对象','案主','受助者','个人服务对象','家庭服务对象','群体服务对象','组织服务对象','社区服务对象'))
client_source_core=uniq(qs('服务对象的来源','主动求助','他人转介','主动接触','转介对象','外展')+ids(lambda c: secondary(c,'接案与预估') and has(c,'来源','转介','求助','接触')))
client_source_related=uniq(S.get('接案与预估',[])+S.get('会谈、访视与专业记录',[]))
client_type_core=uniq(qs('自愿型服务对象','非自愿型服务对象','被强制接受服务','非自愿服务对象','服务对象类型')+ids(lambda c:has(c,'非自愿','强制服务','自愿接受服务')))
client_type_related=uniq(S.get('司法、矫正与禁毒社会工作',[])+ids(lambda c:has(c,'非自愿','强制','司法转介','法院转介','矫正对象','戒毒人员')))
client=node('element.client','1. 服务对象',core=uniq(client_form_core+client_source_core+client_type_core),related=P.get('重点人群社会工作',[]),children=[
    node('element.client.form','存在形式：个人、家庭、群体、组织或社区',client_form_core,client_form_related),
    node('element.client.source','来源：主动求助、他人转介、工作者主动接触',client_source_core,client_source_related),
    node('element.client.type','类型：自愿型、非自愿型、被强制接受服务',client_type_core,client_type_related),
],note='核心卡解释“谁是服务对象”；拓展卡连接重点人群、接案与特殊场域中的具体服务对象。')

worker_core=uniq(qs('社会工作者','服务提供者','资源链接者','倡导者','政策影响者','工作者角色','专业角色')+['C0018','C0019','C0021','C0026','C0027','C0028','C0029'])
worker_related=uniq(ids(lambda c: secondary(c,'功能、角色与实践领域'))+ids(lambda c: primary(c,'社会工作价值与伦理') and has(c,'社会工作者','专业责任','专业关系','能力边界'))+S.get('社会工作实习与实习督导',[]))
worker=node('element.worker','2. 社会工作者',worker_core,worker_related,note='从社会工作者的资格、角色、专业关系、能力边界和专业责任展开。')

values_core=uniq(['C0018','C0022','C0025']+qs('专业价值观','社会工作价值观','价值体系','伦理原则','伦理困境','职业道德'))
values_related=uniq(P.get('社会工作价值与伦理',[])+qs('自决','保密','接纳','尊重','个别化','知情同意','伦理责任'))
values=node('element.values','3. 专业价值观',values_core,values_related,note='完整连接价值基础、伦理原则、伦理困境、道德规范与专业关系。')

case_ids=P.get('个案工作',[]); group_ids=P.get('小组工作',[]); community_ids=P.get('社区工作',[])
mgmt_ids=uniq(S.get('社会行政与服务管理',[])+S.get('项目管理与服务评估',[])+S.get('机构管理与资源筹措',[]))
supervision_ids=uniq(S.get('社会工作督导',[])+ids(lambda c:has(c,'督导') and (primary(c,'社会工作研究、教育与实习') or primary(c,'社会工作行政、组织与督导'))))
research_ids=uniq(S.get('社会工作研究基础',[])+S.get('定量、定性与混合研究',[])+S.get('个案、行动与干预研究',[])+ids(lambda c:has(c,'服务评估','项目评估','成效评估')))
method_core=uniq(['C0018','C0023']+qs('专业方法','直接服务方法','间接服务方法','个案社会工作','小组社会工作','社区社会工作','社会工作服务管理','社会工作督导','社会工作研究'))
method_related=uniq(case_ids+group_ids+community_ids+mgmt_ids+supervision_ids+research_ids)
methods=node('element.methods','4. 专业方法',method_core,method_related,children=[
    node('element.methods.direct','（1）直接服务方法',core=uniq(qs('直接服务方法')+['C0026']),related=uniq(case_ids+group_ids+community_ids),children=[
        node('element.methods.case','个案工作方法',qs('个案社会工作','个案工作'),case_ids),
        node('element.methods.group','小组工作方法',qs('小组社会工作','小组工作'),group_ids),
        node('element.methods.community','社区工作方法',qs('社区社会工作','社区工作'),community_ids),
    ]),
    node('element.methods.indirect','（2）间接服务方法',core=uniq(qs('间接服务方法')+['C0027']),related=uniq(mgmt_ids+supervision_ids+research_ids),children=[
        node('element.methods.management','社会工作服务管理',qs('社会工作服务管理','社会行政','项目管理'),mgmt_ids),
        node('element.methods.supervision','社会工作服务督导',qs('社会工作督导','督导'),supervision_ids),
        node('element.methods.research','社会工作服务研究',qs('社会工作研究','行动研究','干预研究','定量研究','定性研究'),research_ids),
    ])
],note='直接方法连接个案、小组、社区完整题库；间接方法连接管理、督导与研究题库。')

activity_core=uniq(['C0017','C0018','C0024']+qs('服务活动','服务过程','助人过程','接案','预估','计划','介入','评估','结案','转介'))
activity_related=uniq(P.get('通用社会工作实务',[])+S.get('会谈、访视与专业记录',[])+S.get('小组过程与阶段',[])+S.get('小组技巧、冲突与评估',[]))
activity=node('element.activity','5. 服务活动',activity_core,activity_related,note='重点连接接案—预估—计划—介入—评估—结案，以及会谈、记录和过程管理。')

human_core=uniq(qs('人力资源','志愿者','志愿服务','人力')+ids(lambda c: secondary(c,'机构管理与资源筹措') and has(c,'人力','人才','员工','志愿者')))
human_related=uniq(S.get('志愿服务与志愿者管理',[])+ids(lambda c:has(c,'社会支持网络','支持网络','非正式支持','人力资源')))
financial_core=uniq(qs('财力','资金','经费','筹资','筹款','募捐','成本管理','财务管理')+ids(lambda c:secondary(c,'机构管理与资源筹措') and has(c,'筹资','资金','财务','经费')))
financial_related=uniq(ids(lambda c:has(c,'慈善资源','基金会','政府购买服务','项目资金','资源筹措')))
material_core=uniq(qs('物力','物资','设施','场地','设备')+ids(lambda c:has(c,'物资资源','服务设施','物质资源')))
material_related=uniq(ids(lambda c:has(c,'物资','场地','设施','设备','住房资源','生活物资')))
resource_core=uniq(['C0018']+qs('社会服务资源','资源链接','资源整合','资源筹措','社区资源','社会资源'))
resource_related=uniq(human_related+financial_related+material_related+S.get('个案管理与资源整合',[])+S.get('社区需求、资源与参与',[])+S.get('系统生态与社会支持',[]))
resources=node('element.resources','6. 社会服务资源',resource_core,resource_related,children=[
    node('element.resources.human','（1）人力',human_core,human_related),
    node('element.resources.financial','（2）财力',financial_core,financial_related),
    node('element.resources.material','（3）物力',material_core,material_related),
],note='除人力、财力、物力外，关联卡还纳入资源链接、社会支持网络、社区资源与机构资源整合。')

elements=node('elements','一、社会工作服务的要素',core=uniq(['C0017','C0018','C0032']+method_core+values_core),related=uniq(client['relatedIds']+worker['relatedIds']+values['relatedIds']+methods['relatedIds']+activity['relatedIds']+resources['relatedIds']),children=[client,worker,values,methods,activity,resources])

# ---------- Goals ----------
# service-object level goals
hardship_core=uniq(qs('缓解生活困难','生活困难','贫困','社会救助','最低生活保障','临时救助','经济困难','就业援助'))
hardship_related=uniq(P.get('社会政策、福利与保障',[])+S.get('社会救助社会工作',[])+ids(lambda c:has(c,'困难家庭','困境','基本生活','生计','就业','救助')))
crisis_core=uniq(qs('解救生命危难','危机干预','自杀','生命危机','灾害救援','紧急救助','暴力风险','安全风险'))
crisis_related=uniq(S.get('防灾减灾救灾社会工作',[])+ids(lambda c:has(c,'危机','受灾','灾害','家暴','暴力','虐待','自杀','紧急','安全保护','安置')))
potential_core=uniq(qs('激发人的潜能','优势视角','增能','赋权','能力建设','自我效能','抗逆力','成长','发展模式'))
potential_related=uniq(S.get('优势视角与增能',[])+ids(lambda c:has(c,'潜能','能力提升','技能培训','自我决定','参与能力','抗逆力','优势','赋能','增权')))
relations_core=uniq(qs('增进社会关系','社会支持','支持网络','关系协调','社会融入','社区融入','家庭关系','人际关系'))
relations_related=uniq(S.get('系统生态与社会支持',[])+S.get('社区需求、资源与参与',[])+S.get('小组工作基础与动力',[])+ids(lambda c:has(c,'社会关系','人际关系','家庭关系','社会支持','支持网络','社会网络','社会融入','社区融入','邻里','关系协调','共同体','互助网络')))
obj_goals=node('goal.object','（一）服务对象层面的目标',core=uniq(['C0012']+hardship_core+crisis_core+potential_core+relations_core),related=uniq(hardship_related+crisis_related+potential_related+relations_related),children=[
    node('goal.object.hardship','1. 缓解生活困难',hardship_core,hardship_related),
    node('goal.object.crisis','2. 解救生命危难',crisis_core,crisis_related),
    node('goal.object.potential','3. 激发人的潜能',potential_core,potential_related),
    node('goal.object.relationship','4. 增进社会关系',relations_core,relations_related),
])

problem_core=uniq(qs('解决社会问题','社会问题','社会矛盾','矛盾化解','问题治理'))
problem_related=uniq(S.get('基层治理社会工作',[])+S.get('司法、矫正与禁毒社会工作',[])+S.get('贫困与反贫困',[])+ids(lambda c:has(c,'社会问题','社会矛盾','问题解决','纠纷','冲突','治理问题')))
justice_core=uniq(qs('促进社会公正','社会公正','社会公平','公平正义','权益倡导','政策倡导','权利维护'))
justice_related=uniq(S.get('社会政策与政策过程',[])+S.get('结构、社会发展与倡导',[])+ids(lambda c:has(c,'社会公正','社会公平','公平正义','权益保障','权益维护','政策倡导','社会倡导','反歧视','社会排斥','不平等')))
governance_core=uniq(qs('助力社会治理','社会治理','基层治理','共建共治共享','协同治理','参与治理'))
governance_related=uniq(S.get('基层治理社会工作',[])+P.get('社区工作',[])+S.get('协同治理与服务联动',[])+ids(lambda c:has(c,'治理','居民参与','社区参与','协商','社会组织','信访')))
construction_core=uniq(qs('促进社会建设','社会建设','社会发展','乡村振兴','社区建设','民生福祉'))
construction_related=uniq(S.get('新时代社会工作发展',[])+S.get('农村社会工作',[])+S.get('民政社会工作',[])+S.get('社会行政与服务管理',[])+ids(lambda c:has(c,'社会建设','社会发展','社区建设','乡村振兴','民生','公共服务')))
soc_goals=node('goal.social','（二）社会层面的目标',core=uniq(['C0012','C0013','C0014']+problem_core+justice_core+governance_core+construction_core),related=uniq(problem_related+justice_related+governance_related+construction_related),children=[
    node('goal.social.problem','1. 解决社会问题',problem_core,problem_related),
    node('goal.social.justice','2. 促进社会公正',justice_core,justice_related),
    node('goal.social.governance','3. 助力社会治理',governance_core,governance_related),
    node('goal.social.construction','4. 促进社会建设',construction_core,construction_related),
])
goals=node('goals','二、社会工作服务的目标',core=uniq(['C0012','C0013','C0014']+obj_goals['coreIds']+soc_goals['coreIds']),related=uniq(obj_goals['relatedIds']+soc_goals['relatedIds']),children=[obj_goals,soc_goals])

# ---------- Functions ----------
normal_core=uniq(qs('促进正常生活','正常生活','恢复正常生活','基本生活'))
normal_related=uniq(S.get('社会救助社会工作',[])+S.get('医务与精神卫生社会工作',[])+S.get('老年社会工作',[])+S.get('残疾人社会工作',[])+ids(lambda c:has(c,'生活功能','日常生活','基本生活','康复','照顾')))
belief_core=uniq(qs('树立积极信念','积极信念','自信','希望','自我效能','抗逆力','优势视角','增能'))
belief_related=uniq(S.get('优势视角与增能',[])+S.get('认知与行为取向',[])+S.get('人本与存在取向',[])+ids(lambda c:has(c,'自信','信念','希望','抗逆力','自我效能','积极')))
weak_core=uniq(qs('提升弱化的功能','恢复社会功能','社会功能恢复','功能康复','康复服务','再社会化'))
weak_related=uniq(S.get('残疾人社会工作',[])+S.get('医务与精神卫生社会工作',[])+S.get('司法、矫正与禁毒社会工作',[])+ids(lambda c:has(c,'社会功能','康复','功能恢复','再社会化','社会适应')))
adapt_core=uniq(qs('人与环境相互适应','人与社会环境','人境','生态系统','系统理论','生态理论'))
adapt_related=uniq(P.get('人类行为与社会环境',[])+S.get('系统生态与社会支持',[])+ids(lambda c:has(c,'人与环境','环境适应','生态系统','社会环境','个人与环境')))
obj_functions=node('function.object','（一）对服务对象的功能',core=uniq(['C0015','C0030']+normal_core+belief_core+weak_core+adapt_core),related=uniq(normal_related+belief_related+weak_related+adapt_related),children=[
    node('function.object.normal','1. 促进正常生活',normal_core,normal_related),
    node('function.object.belief','2. 树立积极信念',belief_core,belief_related),
    node('function.object.restore','3. 提升弱化的功能',weak_core,weak_related),
    node('function.object.adapt','4. 促进人与环境相互适应',adapt_core,adapt_related),
])

order_core=uniq(qs('维护社会秩序','维持社会秩序','社会秩序','社会稳定','矛盾调处'))
order_related=uniq(S.get('基层治理社会工作',[])+S.get('司法、矫正与禁毒社会工作',[])+S.get('信访社会工作',[])+ids(lambda c:has(c,'秩序','稳定','矛盾','纠纷','调解')))
capital_core=uniq(qs('建构社会资本','社会资本','信任','互惠','共同体意识'))
capital_related=uniq(S.get('系统生态与社会支持',[])+S.get('社区需求、资源与参与',[])+S.get('志愿服务与志愿者管理',[])+ids(lambda c:has(c,'社会资本','信任','互惠','社会网络','支持网络','共同体')))
harmony_core=uniq(qs('促进社会和谐','社会和谐','和谐关系','关系协调'))
harmony_related=uniq(P.get('社区工作',[])+S.get('家庭社会工作',[])+ids(lambda c:has(c,'和谐','关系协调','邻里','家庭关系','社会融合')))
progress_core=uniq(qs('推动社会进步','社会进步','社会改革','制度改革','社会政策倡导','社会发展'))
progress_related=uniq(P.get('社会政策、福利与保障',[])+S.get('结构、社会发展与倡导',[])+S.get('发展史、职业化与本土化',[])+ids(lambda c:has(c,'进步','改革','制度','政策倡导','社会发展')))
soc_functions=node('function.social','（二）对社会的功能',core=uniq(['C0015','C0031','C0033']+order_core+capital_core+harmony_core+progress_core),related=uniq(order_related+capital_related+harmony_related+progress_related),children=[
    node('function.social.order','1. 维护社会秩序',order_core,order_related),
    node('function.social.capital','2. 建构社会资本',capital_core,capital_related),
    node('function.social.harmony','3. 促进社会和谐',harmony_core,harmony_related),
    node('function.social.progress','4. 推动社会进步',progress_core,progress_related),
])

care_core=uniq(qs('倡导互相关爱的现代社会文明','互相关爱','互助','关爱','人道主义','志愿精神','慈善'))
care_related=uniq(S.get('志愿服务与志愿者管理',[])+S.get('价值基础与价值体系',[])+ids(lambda c:has(c,'互助','关爱','志愿','慈善','人道主义','利他')))
solidarity_core=uniq(qs('促进社会团结','社会团结','社会整合','社区凝聚','社会凝聚','共同体'))
solidarity_related=uniq(P.get('社区工作',[])+S.get('小组工作基础与动力',[])+S.get('协同治理与服务联动',[])+ids(lambda c:has(c,'社会团结','社区凝聚','社会凝聚','社会整合','共同体','互助网络','社会融合')))
culture_functions=node('function.culture','（三）对文化的功能',core=uniq(care_core+solidarity_core),related=uniq(care_related+solidarity_related),children=[
    node('function.culture.care','1. 倡导互相关爱的现代社会文明',care_core,care_related),
    node('function.culture.solidarity','2. 促进社会团结',solidarity_core,solidarity_related),
])
functions=node('functions','三、社会工作服务的功能',core=uniq(['C0015','C0030','C0031','C0033']+obj_functions['coreIds']+soc_functions['coreIds']+culture_functions['coreIds']),related=uniq(obj_functions['relatedIds']+soc_functions['relatedIds']+culture_functions['relatedIds']),children=[obj_functions,soc_functions,culture_functions])

# Every card receives at least one framework link. Fallbacks only attach at a broad, defensible node
# and are marked as related, never core. This avoids forcing a card into an overly specific leaf.
def direct_nodes(n):
    out=[n]
    for ch in n.get('children',[]): out += direct_nodes(ch)
    return out
_all_nodes=direct_nodes(elements)+direct_nodes(goals)+direct_nodes(functions)
_directly_linked=set()
for n in _all_nodes: _directly_linked.update(n.get('coreIds',[]));_directly_linked.update(n.get('relatedIds',[]))
_node={n['id']:n for n in _all_nodes}
for c in cards:
    if c['id'] in _directly_linked: continue
    p=c.get('primaryTopic','');s=c.get('secondaryTopic','');target='element.activity'
    if p=='社会工作价值与伦理': target='element.values'
    elif p=='人类行为与社会环境': target='function.object.adapt'
    elif p=='社会工作理论': target='element.methods'
    elif p=='社会政策、福利与保障': target='function.social.progress'
    elif p=='个案工作': target='element.methods.case'
    elif p=='小组工作': target='element.methods.group'
    elif p=='社区工作': target='element.methods.community'
    elif p=='通用社会工作实务': target='element.activity'
    elif p=='社会工作行政、组织与督导': target='element.methods.indirect'
    elif p=='社会工作研究、教育与实习': target='element.worker' if s=='社会工作教育' else 'element.methods.indirect'
    elif p=='重点人群社会工作': target='element.client'
    elif p=='重点场域社会工作': target='element.activity'
    elif p=='综合应用': target='element.activity'
    elif p=='社会工作基础与发展':
        if s=='发展史、职业化与本土化': target='function.social.progress'
        elif s=='新时代社会工作发展': target='goal.social.governance'
        elif s=='功能、角色与实践领域': target='element.worker'
        else: target='element.activity'
    _node[target]['relatedIds']=uniq(_node[target].get('relatedIds',[])+[c['id']])

framework=OrderedDict(
    version='2.8.5',
    title='社会工作服务',
    subtitle='要素 → 目标 → 功能',
    mappingPolicy='教材/思维导图节点保持不变；外部资料仅用于补充“卡片与节点之间的关联”，不改写2497张卡的题目与答案。核心卡=直接命中该概念；拓展卡=在具体人群、场域、方法或政策中应用该概念。',
    externalReferenceSummary='2026社会工作综合能力中社版框架、政府社会工作服务规范、高校社会工作专业资料，用于校验目标/功能/要素概念边界与应用关系。',
    branches=[elements,goals,functions]
)

# Recalculate parent sets recursively from children + own to guarantee counts and completeness.
def finalize(n):
    for ch in n.get('children',[]):finalize(ch)
    if n.get('children'):
        child_core=[]; child_all=[]
        for ch in n['children']:
            child_core += ch.get('coreIds',[])
            child_all += ch.get('coreIds',[])+ch.get('relatedIds',[])
        n['coreIds']=uniq(n.get('coreIds',[])+child_core)
        all_ids=uniq(n.get('coreIds',[])+n.get('relatedIds',[])+child_all)
        n['relatedIds']=[x for x in all_ids if x not in set(n['coreIds'])]
    n['coreCount']=len(n.get('coreIds',[]));n['relatedCount']=len(n.get('relatedIds',[]));n['totalCount']=n['coreCount']+n['relatedCount']
for b in framework['branches']:finalize(b)
all_ids=uniq(sum(([x for x in b['coreIds']+b['relatedIds']] for b in framework['branches']),[]))
framework['uniqueLinkedCards']=len(all_ids)
framework['linkCoveragePercent']=round(len(all_ids)/len(cards)*100,1)
framework['totalCards']=len(cards)

with open(OUT,'w',encoding='utf-8') as f:json.dump(framework,f,ensure_ascii=False,indent=2)

# QA/report
rows=[]
def collect(n,depth=0):
    rows.append((depth,n['id'],n['title'],n['coreCount'],n['relatedCount'],n['totalCount']))
    for ch in n.get('children',[]):collect(ch,depth+1)
for b in framework['branches']:collect(b)

qa={
 'version':'2.8.5','cards_total':len(cards),'unique_linked_cards':len(all_ids),'coverage_percent':framework['linkCoveragePercent'],
 'nodes_total':len(rows),'missing_card_ids':[],
 'duplicate_ids_within_node':0,
 'branches':{b['title']:{'core':b['coreCount'],'related':b['relatedCount'],'total':b['totalCount']} for b in framework['branches']},
 'checks':{
   'all_mapped_ids_exist':all(x in byid for _,_,_,_,_,_ in rows for x in []),
   'answers_modified':False,'questions_modified':False,'framework_is_many_to_many':True
 }
}
# actual missing check
missing=[]
def inspect(n):
    for x in n.get('coreIds',[])+n.get('relatedIds',[]):
        if x not in byid: missing.append(x)
    for ch in n.get('children',[]):inspect(ch)
for b in framework['branches']:inspect(b)
qa['missing_card_ids']=uniq(missing);qa['checks']['all_mapped_ids_exist']=not bool(missing)
with open(QA,'w',encoding='utf-8') as f:json.dump(qa,f,ensure_ascii=False,indent=2)
with open(REPORT,'w',encoding='utf-8') as f:
    f.write('# V2.8.5 社会工作服务教材框架全量关联报告\n\n')
    f.write(f'- 总卡片：{len(cards)}\n- 已被本框架关联的唯一卡片：{len(all_ids)}（{framework["linkCoveragePercent"]}%）\n- 框架节点：{len(rows)}\n- 映射方式：多对多；同一卡可同时属于要素、目标、功能中的多个节点。\n- 不修改题目/答案，仅新增框架索引。\n\n')
    f.write('| 层级 | 节点 | 核心卡 | 拓展卡 | 合计 |\n|---:|---|---:|---:|---:|\n')
    for depth,_,title,core,rel,total in rows:
        f.write(f'| {depth+1} | {"　"*depth}{title} | {core} | {rel} | {total} |\n')
print('wrote',OUT)
print('linked unique',len(all_ids),'/',len(cards),framework['linkCoveragePercent'])
for r in rows: print('  '*r[0]+f'{r[2]} core={r[3]} related={r[4]} total={r[5]}')
