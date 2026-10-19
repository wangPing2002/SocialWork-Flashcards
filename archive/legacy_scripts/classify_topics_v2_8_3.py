from pathlib import Path
import argparse, collections, json

parser = argparse.ArgumentParser(description='Classify C0001-C2497 into V2.8.3 two-level learning topics.')
parser.add_argument('--project-root', default=str(Path(__file__).resolve().parents[1]))
args = parser.parse_args()
ROOT = Path(args.project_root).resolve()
CARDS = ROOT / 'app/src/main/assets/cards.json'
CONTENT = ROOT / 'content'

cards = json.loads(CARDS.read_text(encoding='utf-8'))
assert len(cards) == 2497
by_id = {c['id']: c for c in cards}
assert len(by_id) == 2497 and cards[0]['id'] == 'C0001' and cards[-1]['id'] == 'C2497'

P_FOUND='社会工作基础与发展'
P_VALUES='社会工作价值与伦理'
P_HUMAN='人类行为与社会环境'
P_THEORY='社会工作理论'
P_GENERAL='通用社会工作实务'
P_CASE='个案工作'
P_GROUP='小组工作'
P_COMM='社区工作'
P_ADMIN='社会工作行政、组织与督导'
P_RESEARCH='社会工作研究、教育与实习'
P_POLICY='社会政策、福利与保障'
P_PEOPLE='重点人群社会工作'
P_FIELD='重点场域社会工作'
P_COMP='综合应用'

PRIMARY_ORDER=[P_FOUND,P_VALUES,P_HUMAN,P_THEORY,P_GENERAL,P_CASE,P_GROUP,P_COMM,P_ADMIN,P_RESEARCH,P_POLICY,P_PEOPLE,P_FIELD,P_COMP]
PRIMARY_GROUPS={
    '核心知识':[P_FOUND,P_VALUES,P_HUMAN,P_THEORY],
    '专业方法':[P_GENERAL,P_CASE,P_GROUP,P_COMM],
    '专业体系':[P_ADMIN,P_RESEARCH,P_POLICY],
    '实务领域':[P_PEOPLE,P_FIELD],
    '综合训练':[P_COMP],
}

def any_key(s, keys):
    return any(k in s for k in keys)

def primary_for(c):
    n=int(c['id'][1:])
    if 1<=n<=80:return P_FOUND
    if 81<=n<=160:return P_VALUES
    if 161<=n<=299:return P_HUMAN
    if 300<=n<=411:return P_THEORY
    if 412<=n<=485:return P_POLICY
    if 486<=n<=588:return P_CASE
    if 589<=n<=685:return P_GENERAL
    if 686<=n<=735:return P_CASE
    if 736<=n<=784:return P_GROUP
    if 785<=n<=835:return P_COMM
    if 836<=n<=960:return P_ADMIN
    if 961<=n<=1000:return P_RESEARCH
    if 1001<=n<=1010:return P_COMP
    if 1011<=n<=1047:return P_ADMIN
    if 1048<=n<=1068:return P_RESEARCH
    if 1069<=n<=1091:return P_COMP
    if 1092<=n<=1172:return P_FIELD
    if 1173<=n<=1201:return P_PEOPLE
    if 1202<=n<=1325:return P_FIELD
    if 1326<=n<=1382:return P_PEOPLE
    if 1383<=n<=1448:return P_FIELD
    if 1449<=n<=1456:return P_COMP
    if 1457<=n<=1560:return P_FIELD
    if 1561<=n<=1801:return P_PEOPLE
    if 1802<=n<=1828:return P_FIELD
    if 1829<=n<=1853:return P_POLICY
    if 1854<=n<=1870:return P_FIELD
    if 1871<=n<=1911:return P_RESEARCH
    if 1912<=n<=1920:return P_COMP
    if 1921<=n<=1923:return P_GENERAL
    if 1924<=n<=1928:return P_FOUND
    if 1929<=n<=1933:return P_THEORY
    if 1934<=n<=1935:return P_FOUND
    if 1936<=n<=1938:return P_ADMIN
    if n==1939:return P_CASE
    if 1940<=n<=1941:return P_RESEARCH
    if 1942<=n<=1965:return P_PEOPLE
    if 1966<=n<=1983:return P_FIELD
    if 1984<=n<=1989:return P_POLICY
    if 1990<=n<=1992:return P_FIELD
    if 1993<=n<=2000:return P_RESEARCH
    if 2001<=n<=2133:
        # True-exam supplements each point to one matching canonical card.
        rid=(c.get('relatedIds') or [''])[0]
        if rid in by_id:
            return primary_for(by_id[rid])
        return P_COMP

    s=(c.get('sourceField','')+' '+c.get('question',''))
    practical=[
        ('《社会工作实务-250纠正》第三章',P_FIELD),('《社会工作实务-250纠正》第四章',P_FIELD),
        ('《社会工作实务-250纠正》第五章',P_PEOPLE),('《社会工作实务-250纠正》第六章',P_FIELD),
        ('《社会工作实务-250纠正》第七章',P_FIELD),('《社会工作实务-250纠正》第八章',P_PEOPLE),
        ('《社会工作实务-250纠正》第九章',P_FIELD),('《社会工作实务-250纠正》第十章',P_FIELD),
        ('《社会工作实务-250纠正》第十一章',P_FIELD),('《社会工作实务-250纠正》第十二章',P_FIELD),
        ('《社会工作实务-250纠正》第十三章',P_PEOPLE),('《社会工作实务-250纠正》第十四章',P_PEOPLE),
        ('《社会工作实务-250纠正》第十五章',P_PEOPLE),('《社会工作实务-250纠正》第十六章',P_PEOPLE),
        ('《社会工作实务-250纠正》第十七章',P_PEOPLE),('《社会工作实务-250纠正》第十八章',P_PEOPLE),
    ]
    for pat,p in practical:
        if pat in s:return p
    if '《社会工作实务-250纠正》第二章' in s or '《社会工作实务-250纠正》第一章' in s:return P_GENERAL

    concept={
        '第一章':P_FOUND,'第二章':P_FOUND,'第三章':P_VALUES,'第四章':P_THEORY,'第五章':P_POLICY,'第六章':P_HUMAN,
        '第七章':P_CASE,'第八章':P_GROUP,'第九章':P_COMM,'第十章':P_ADMIN,'第十一章':P_PEOPLE,'第十二章':P_PEOPLE,
        '第十三章':P_PEOPLE,'第十四章':P_PEOPLE,'第十五章':P_PEOPLE,'第十六章':P_PEOPLE,'第十七章':P_FIELD,
        '第十八章':P_FIELD,'第十九章':P_FIELD,'第二十章':P_POLICY,'第二十一章':P_FIELD,'第二十二章':P_FIELD,
        '第二十三章':P_RESEARCH,'第二十四章':P_RESEARCH,'第二十五章':P_RESEARCH,
    }
    if '《社会工作概论》' in s:
        for ch,p in concept.items():
            if f'《社会工作概论》{ch}' in s:return p

    middle={'第一章':P_FOUND,'第二章':P_FOUND,'第三章':P_FOUND,'第四章':P_VALUES,'第五章':P_HUMAN,'第六章':P_THEORY,
            '第七章':P_CASE,'第八章':P_GROUP,'第九章':P_COMM,'第十章':P_ADMIN,'第十一章':P_ADMIN,
            '第十二章':P_ADMIN,'第十三章':P_RESEARCH}
    if '2026《社会工作综合能力（中级）》' in s:
        for ch,p in middle.items():
            if f'2026《社会工作综合能力（中级）》{ch}' in s:return p
    return P_COMP

def secondary_for(c,p):
    n=int(c['id'][1:]); q=c.get('question',''); sf=c.get('sourceField',''); s=q+' '+sf+' '+c.get('topic','')
    if 2001<=n<=2133:
        rid=(c.get('relatedIds') or [''])[0]
        if rid in by_id:
            rc=by_id[rid]
            rp=primary_for(rc)
            return secondary_for(rc,rp)

    if p==P_FOUND:
        if any_key(s,['习近平','党的二十','中央社会工作','新时代','政治理论','群众路线','中国特色社会主义社会治理','全面领导']):return '新时代社会工作发展'
        if any_key(s,['产生','发展史','历史','职业化','专业化','本土化','恢复','重建','西方现代','济贫法','慈善组织','睦邻','教育先行']):return '发展史、职业化与本土化'
        if any_key(s,['功能','角色','实践领域','服务领域','构成要素','基本要素','助人功能','社会秩序']):return '功能、角色与实践领域'
        return '基本概念与专业属性'
    if p==P_VALUES:
        if any_key(s,['人工智能','数字','数据','新技术']):return '数字技术与新兴伦理'
        if any_key(s,['伦理困境','伦理决策','自决','保密','知情同意','双重关系','专业边界','伦理冲突','最小伤害','隐私','价值冲突']):return '伦理原则、困境与决策'
        if any_key(s,['道德规范','伦理规范','伦理责任','伦理守则']):return '专业伦理与道德规范'
        return '价值基础与价值体系'
    if p==P_HUMAN:
        if any_key(s,['成年早期','成年中期','成年晚期','中年','老年阶段']):return '成年与老年发展'
        if any_key(s,['婴幼儿','学龄前','学龄期','青少年期','儿童期','青春期']):return '儿童与青少年发展'
        if any_key(s,['生命周期','人生发展','发展阶段','阶段的划分','发展任务']):return '生命周期与发展阶段'
        return '人类行为与社会环境基础'
    if p==P_THEORY:
        if any_key(s,['协同治理','服务联动']):return '协同治理与服务联动'
        if any_key(s,['精神分析','心理社会','心理动力','依恋']):return '心理动力与心理社会'
        if any_key(s,['认知行为','理性情绪','行为治疗','行为主义','学习理论']):return '认知与行为取向'
        if any_key(s,['人本','存在主义','意义治疗','人本治疗']):return '人本与存在取向'
        if any_key(s,['系统理论','生态系统','生态视角','社会支持','一般系统']):return '系统生态与社会支持'
        if any_key(s,['优势视角','优势观点','增能','赋能','增权']):return '优势视角与增能'
        if any_key(s,['建构','叙事','后现代','社会建构']):return '建构、叙事与后现代'
        if any_key(s,['女性主义','马克思','倡导','社会发展视角','结构主义','激进']):return '结构、社会发展与倡导'
        return '理论基础与理论整合'
    if p==P_GENERAL:
        if any_key(s,['接案','预估']):return '接案与预估'
        if any_key(s,['计划','介入']):return '计划与介入'
        if any_key(s,['评估','结案','转介']):return '评估、结案与转介'
        return '通用服务过程与方法'
    if p==P_CASE:
        if '个案管理' in s:return '个案管理与资源整合'
        if any_key(s,['会谈','访视','家访','个案记录','记录应','提问','同感','澄清','摘要','对质']):return '会谈、访视与专业记录'
        if any_key(s,['接案','预估']):return '接案与预估'
        if any_key(s,['计划','介入']):return '计划与介入'
        if any_key(s,['评估','结案','转介']):return '评估、结案与转介'
        if any_key(s,['模式','心理社会','认知行为','理性情绪','任务中心','危机介入','危机干预','人本治疗','家庭治疗','精神分析']):return '个案服务模式'
        return '个案工作基础与专业关系'
    if p==P_GROUP:
        if any_key(s,['阶段','初期','中期','后期','结束期','开始阶段','终止']):return '小组过程与阶段'
        if any_key(s,['冲突','技巧','评估','带领','沟通技巧']):return '小组技巧、冲突与评估'
        if any_key(s,['模式','社会目标','治疗模式','互惠模式']):return '小组服务模式'
        return '小组工作基础与动力'
    if p==P_COMM:
        if any_key(s,['地区发展','社会策划','社会行动','社区服务模式','模式']):return '社区工作模式'
        if any_key(s,['社区照顾','能力建设','社会资本','互助网络']):return '社区照顾与能力建设'
        if any_key(s,['需求','需要评估','资源','居民参与','社区参与','社区骨干','社区领袖','组织']):return '社区需求、资源与参与'
        return '社区工作基础'
    if p==P_ADMIN:
        if '督导' in s:return '社会工作督导'
        if '志愿' in s:return '志愿服务与志愿者管理'
        if any_key(s,['项目管理','服务项目','项目评估','项目需求','项目目标','项目设计','项目计划','项目监测','项目生命周期']):return '项目管理与服务评估'
        if any_key(s,['机构','人力资源','资源筹措','筹资','财务','战略','员工','组织管理','绩效考核']):return '机构管理与资源筹措'
        return '社会行政与服务管理'
    if p==P_RESEARCH:
        if any_key(s,['实习','实习督导']):return '社会工作实习与实习督导'
        if any_key(s,['教育','课程','人才培养','学院教育','继续教育']):return '社会工作教育'
        if any_key(s,['行动研究','个案研究','干预研究','扎根理论','实践研究']):return '个案、行动与干预研究'
        if any_key(s,['定量','定性','混合研究','问卷','实验研究','访谈','观察']):return '定量、定性与混合研究'
        return '社会工作研究基础'
    if p==P_POLICY:
        if any_key(s,['贫困','反贫困','扶贫','社会排斥','社会剥夺']):return '贫困与反贫困'
        if any_key(s,['社会政策','政策制定','政策执行','政策评估','政策倡导','政策过程']):return '社会政策与政策过程'
        return '社会福利与社会保障'
    if p==P_PEOPLE:
        source=[('《社会工作实务-250纠正》第五章','新就业群体社会工作'),('《社会工作实务-250纠正》第八章','退役军人事务社会工作'),
                ('《社会工作实务-250纠正》第十三章','儿童社会工作'),('《社会工作概论》第十一章','儿童社会工作'),
                ('《社会工作实务-250纠正》第十四章','青少年社会工作'),('《社会工作概论》第十二章','青少年社会工作'),
                ('《社会工作实务-250纠正》第十五章','老年社会工作'),('《社会工作概论》第十三章','老年社会工作'),
                ('《社会工作实务-250纠正》第十六章','妇女社会工作'),('《社会工作概论》第十四章','妇女社会工作'),
                ('《社会工作实务-250纠正》第十七章','残疾人社会工作'),('《社会工作概论》第十五章','残疾人社会工作'),
                ('《社会工作实务-250纠正》第十八章','家庭社会工作'),('《社会工作概论》第十六章','家庭社会工作')]
        for pat,sec in source:
            if pat in sf:return sec
        if '新就业' in s:return '新就业群体社会工作'
        if any_key(s,['退役军人','优抚','军休','军转','光荣院','英烈']):return '退役军人事务社会工作'
        if any_key(s,['儿童社会工作','儿童福利','儿童发展','困境儿童','儿童保护']):return '儿童社会工作'
        if any_key(s,['青少年社会工作','青少年福利','青少年发展','青少年服务']):return '青少年社会工作'
        if any_key(s,['老年社会工作','老年人','老人','老年福利','老年小组']):return '老年社会工作'
        if any_key(s,['妇女社会工作','妇女','女性主义','社会性别主流化']):return '妇女社会工作'
        if any_key(s,['残疾人社会工作','残疾人','残障','康复']):return '残疾人社会工作'
        if any_key(s,['家庭社会工作','家庭治疗','家庭暴力','夫妻关系','亲子关系','家庭评估','家庭服务']):return '家庭社会工作'
        return '重点人群综合'
    if p==P_FIELD:
        source=[('《社会工作实务-250纠正》第三章','信访社会工作'),('《社会工作实务-250纠正》第四章','基层治理社会工作'),
                ('《社会工作实务-250纠正》第六章','社会救助社会工作'),('《社会工作实务-250纠正》第七章','司法、矫正与禁毒社会工作'),
                ('《社会工作实务-250纠正》第九章','学校社会工作'),('《社会工作实务-250纠正》第十章','医务与精神卫生社会工作'),
                ('《社会工作实务-250纠正》第十一章','企业与工业社会工作'),('《社会工作实务-250纠正》第十二章','防灾减灾救灾社会工作'),
                ('《社会工作概论》第十七章','医务与精神卫生社会工作'),('《社会工作概论》第十八章','企业与工业社会工作'),
                ('《社会工作概论》第十九章','农村社会工作'),('《社会工作概论》第二十一章','司法、矫正与禁毒社会工作'),
                ('《社会工作概论》第二十二章','民政社会工作')]
        for pat,sec in source:
            if pat in sf:return sec
        if '信访' in s:return '信访社会工作'
        if any_key(s,['基层治理','基层政权','党群服务','群防群治','社区自治','协商议事']):return '基层治理社会工作'
        if any_key(s,['社会救助','低保','救助对象','临时救助']):return '社会救助社会工作'
        if any_key(s,['司法','矫正','禁毒','戒毒','涉罪','监禁','新社会防卫']):return '司法、矫正与禁毒社会工作'
        if any_key(s,['防灾','救灾','灾害','韧性建设','受灾']):return '防灾减灾救灾社会工作'
        if '学校' in s or '校园' in s:return '学校社会工作'
        if any_key(s,['医务','医院','医疗','健康照顾','公共卫生','精神卫生','精神健康','慢性病','临终关怀']):return '医务与精神卫生社会工作'
        if any_key(s,['企业','工业社会工作','职业社会工作','员工','EAP']):return '企业与工业社会工作'
        if any_key(s,['农村','乡村','乡镇']):return '农村社会工作'
        if '民政' in s:return '民政社会工作'
        return '其他重点场域'
    if p==P_COMP:
        if 1912<=n<=1920 or 1449<=n<=1456:return '跨人群与跨领域综合'
        if any_key(s,['直接服务—管理—督导—研究','管理、督导和研究','研究、教育、督导和实务','专业协同']):return '专业体系综合'
        return '跨方法与跨层次综合'
    return '待分类'

for c in cards:
    p=primary_for(c)
    c['primaryTopic']=p
    c['secondaryTopic']=secondary_for(c,p)
    c['contentVersion']='2.8.3'

primary_counts=collections.Counter(c['primaryTopic'] for c in cards)
secondary_counts={p:collections.Counter(c['secondaryTopic'] for c in cards if c['primaryTopic']==p) for p in PRIMARY_ORDER}
all_secondary=sum(len(x) for x in secondary_counts.values())

mapping={c['id']:{'primaryTopic':c['primaryTopic'],'secondaryTopic':c['secondaryTopic']} for c in cards}
map_payload={'version':'2.8.3','cardCount':len(cards),'primaryOrder':PRIMARY_ORDER,'primaryGroups':PRIMARY_GROUPS,'cards':mapping}
CONTENT.mkdir(parents=True,exist_ok=True)
(CONTENT/'card_taxonomy_v2_8_3.json').write_text(json.dumps(map_payload,ensure_ascii=False,indent=2),encoding='utf-8')

qa={
    'version':'2.8.3','cards':len(cards),'primaryTopicCount':len(primary_counts),'secondaryTopicCount':all_secondary,
    'primaryCounts':dict(primary_counts),
    'secondaryCounts':{p:dict(secondary_counts[p]) for p in PRIMARY_ORDER},
    'unclassifiedPrimary':sum(c['primaryTopic'] not in PRIMARY_ORDER for c in cards),
    'unclassifiedSecondary':sum(c['secondaryTopic'] in {'','待分类','重点人群综合','其他重点场域'} for c in cards),
    'coverageComplete':all(c.get('primaryTopic') and c.get('secondaryTopic') for c in cards),
    'idCoverageComplete':set(mapping)=={f'C{i:04d}' for i in range(1,2498)},
    'trueExamInheritedFromRelated':sum(2001<=int(c['id'][1:])<=2133 and bool(c.get('relatedIds')) for c in cards),
}
(CONTENT/'taxonomy_qa_v2_8_3.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2),encoding='utf-8')
CARDS.write_text(json.dumps(cards,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(qa,ensure_ascii=False,indent=2))
