#!/usr/bin/env python3
"""Enrich *fictional* study cases without treating reconstructed text as exam originals.
Idempotent: originalMaterial and original SHA are preserved; an unchanged RC1 input is expected.
"""
from pathlib import Path
import json,re,hashlib,sys
ROOT=Path(__file__).resolve().parents[1]
AS=ROOT/'app/src/main/assets'; CONTENT=ROOT/'content/case_enrichment'
BANK=AS/'case_bank_v30.json'
def additions(name):
    parts=re.split(r'(?m)^@@ ([^\n]+)\n',(CONTENT/name).read_text(encoding='utf-8'))
    if parts[0].strip():raise AssertionError('unmatched preamble')
    records=dict(zip(parts[1::2],parts[2::2]))
    if len(records)!=(len(parts)-1)//2:raise AssertionError('duplicate ids in '+name)
    return {k:v.strip() for k,v in records.items()}
def sha(s):return hashlib.sha256(s.encode('utf-8')).hexdigest()

# Source-derived *mechanisms* are indicated; all names, context, dialogue and chronology below are synthetic.
PUBLIC_CASES=[
{
 'id':'EXT-2023-SC-DV-TRAIN-01','sourceBatch':'EXT-OFFICIAL','linkedCardIds':['C0644'],
 'title':'照片和报警记录不足时的反家暴保护与社工介入',
 'sourceLabel':'依据最高人民法院典型案例要素改编（非考研原题）',
 'material':'''【情境背景】42岁的周女士与丈夫共同生活，育有一名正在上小学的女儿。丈夫近期因失业和经济压力经常在家争吵，数月来周女士几次出现手臂淤青。她曾向亲戚说是自己碰伤，不希望女儿被同学议论。两周前，丈夫在争执时拿起厨房器具进行威胁，周女士拨打报警电话，但等工作人员到场时丈夫已经离开。她保留了当日手机拍摄的伤处照片和拨打报警电话的记录，没有完整的目击证言。

【首次求助】周女士来到社区社会工作站，拿出照片却说：“我怕证据不够，别人会觉得是夫妻吵架。”她希望继续让女儿上原来的学校，也暂时不决定离婚。社工了解后发现，周女士有稳定的临时工作收入，但银行卡和住房钥匙由丈夫掌握；她担心离开住所后短时间内无处落脚。女儿在家庭争吵时经常戴上耳机躲在房间，近期上课注意力下降，但学校尚未进行儿童安全评估。

【多方信息】丈夫在电话中否认施暴，称照片是周女士自己跌倒造成的，并要求立即告知社工与妻子谈了什么。居委会工作人员建议双方参加家庭调解；周女士则担心公开对质会引发报复。社区有妇联权益咨询、临时庇护转介、医疗伤情评估、法律援助等资源，能否获得特定服务仍须依法依规评估。社工获知最高人民法院公布的保护令案例强调，办理人身安全保护令的证据判断不同于普通民事案件的完整举证要求；但是否满足法定条件，应由有权法院结合实际证据和危险情形审查决定。

【继续变化】一周后周女士主动回访，称丈夫已从亲属处打听到她求助的消息，最近两次深夜敲门、追问她是不是要报警。周女士计划先在可信任亲属处短住，却担心孩子上学接送和日常费用。她表示愿意和社工共同拟定安全联络方式、必要资料保全清单和分阶段服务计划，但希望每次信息共享都先说明用途。''',
 'subquestions':[
 {'id':'Q1','number':1,'question':'根据材料识别周女士与女儿的风险和需要。为什么不应将本案仅视为普通夫妻沟通不良？',
  'answer':'本案呈现身体侵害迹象、威胁、持续追问求助信息及可能升级的骚扰；女儿可能受到目睹冲突和生活安排不稳的影响。风险判断需要综合伤害发生频率、升级、可取得的支持、安全住所、儿童处境和当事人的主观担忧，不能把施暴归因于经济压力或受暴者言行。专业服务既须尊重周女士暂不离婚的选择，也须开展紧迫安全与儿童保护评估。银行账户控制、住房依赖、隐私担忧和孩子上学安排构成实际离开障碍。对未核实的危险程度应清楚记录未知而非断言无风险。',
  'scorePoints':['识别身体与威胁风险及其变化','区分暴力与一般冲突','评估儿童处境及现实离开障碍','尊重婚姻选择但坚持安全优先']},
 {'id':'Q2','number':2,'question':'针对现有受伤照片、报警记录与双方相反陈述，社工应如何提供合法、审慎的求助支持？',
  'answer':'先向周女士说明可选择的医疗评估、报警、妇联、法律援助和人身安全保护令信息，协助其在安全条件下保存真实照片、报警与就医记录，但不替法院认定证据效力或承诺一定获准保护令。可以援引最高人民法院2023年11月公布的反家庭暴力典型案例，说明人民法院对保护令申请适用相应证明标准，法院可综合照片、报警记录、当事人陈述等情况判断；这不能被曲解为社工自行确定证明已经充分。征得同意后协调转介与陪同，明确安全联系方式和必要的保密例外；丈夫索要会谈内容不能构成自动披露理由。避免未经安全评估安排面对面对质。',
  'scorePoints':['提供真实可核查的法律求助信息','依法保全资料不代替法官裁决','知情同意和最小必要信息共享','禁止未经评估的对质性调解']},
 {'id':'Q3','number':3,'question':'请提出包含安全计划、家庭和资源支持、持续追踪的分阶段介入方案，并给出评估指标。',
  'answer':'第一阶段优先核实紧迫危险、儿童安全、可立即联系的支持者和安全住所；与周女士共同确定安全联系方式、应急求助路径、必要物品及关键文件准备，按实际情况联系有权限的专业部门。第二阶段处理医疗与法律咨询、短期居住、交通接送、生活保障以及女性支持资源，避免社工替当事人作离婚决定。第三阶段在风险允许时开展创伤知情的个案支持、经济与照护安排和社会支持网络重建。定期复评暴力或威胁是否持续、儿童生活安全、求助渠道是否可用、服务对象对方案的控制感、各项转介是否真正接通；未取得同意及没有法定依据，不向无关方扩散个案信息。',
  'scorePoints':['分阶段动态风险评估','安全联络与资源转介','儿童安全和生活连续性','服务对象自主与保密','可核查的过程和结果指标']}
 ],
 'adaptationSources':[{'name':'最高人民法院：反家庭暴力典型案例（第一批），案例二及证明标准说明（2023-11-25）','url':'https://www.court.gov.cn/zixun/xiangqing/418562.html','role':'法律事实与风险机制参考；新增角色、数字和对话为虚构'}]
},
{
 'id':'EXT-2025-SPP-JUV-TRAIN-01','sourceBatch':'EXT-OFFICIAL','linkedCardIds':['C2031'],
 'title':'三名涉案未成年人差异化支持：司法社工与家庭学校协同',
 'sourceLabel':'依据最高检与中央社会工作部典型案例机制改编（非考研原题）',
 'material':'''【事件与司法处遇】某地三名未成年人甲、乙、丙卷入一次结伙侵害他人财产并造成身体伤害的事件。甲在事件中的实际参与程度和证据情况与乙、丙不同，依法调查处理后，有关机关对三人分别作出不同的程序或处置决定。乙曾有多次严重不良行为，最终进入司法程序；丙经过法定审查被纳入附条件不起诉的考察帮教，甲则转入依法开展的严重不良行为预防矫治支持。上述司法决定均由具有相应权限的机关作出，社会工作者没有决定有罪、量刑或者作出附条件不起诉的权力。

【家庭与个人情境】司法社工在分次访谈中发现，甲与父亲同住，父亲长期从事轮班工作，对甲的日常生活缺少稳定陪伴；甲已结束义务教育阶段学习，想找一份手工技能工作，但对正规培训缺乏信心。乙与父母冲突激烈，在家多次被斥为“不争气”，谈到自己长处时经常沉默，却对绘画和动手制作有持续兴趣。丙性格内向，容易在朋友圈中随人行动，仍希望继续读书，担心同学得知案件经过后排斥自己。三人的需要有交叉，却不适合统一使用一种会谈方案。

【服务支持与协同】检察机关根据各自法律职责协调司法社工、学校、社区、家庭和职业服务资源。甲的支持重点是稳定亲子沟通、职业训练与生活规划；乙需要在法定处遇框架内开展心理社会评估、优势发现和家庭教育指导；丙的帮教重点是正向同伴网络、学习稳定与社会参与。社工提出分级分类服务计划，要求每名未成年人都能以可理解的方式参与目标讨论；家长希望得知全部会谈细节，学校又担心学生在班级被贴上“涉案”标签。

【后续冲突与评估】一次活动后，志愿者把丙参加服务的照片发到公开群组，虽然没有写出姓名，熟悉他的居民仍可能辨认。甲的父亲认为先去学技能会影响家里临时劳务安排，乙的父亲则要求社工采用“严格惩罚”的办法。项目负责人要求报告参加了多少场活动，社工认为还需要了解实际行为变化、家庭支持、返校或就业稳定以及再次涉险风险。团队必须在司法职责与社会工作专业性、个人信息保护和服务成效之间持续协调。''',
 'subquestions':[
 {'id':'Q1','number':1,'question':'结合三名少年的不同司法处遇、家庭情境和优势，阐明分级分类司法社会工作服务的必要性。',
  'answer':'三名少年的证据情况、法定处遇、成长需要和保护因素不同，不能因为参与同一事件就采用完全相同的教育矫治。司法决定由法定机关负责，社工应在授权职责内做社会调查、需要评估、家庭教育支持、技能链接和社会融入服务。甲需补充监护和职业路径，乙需处理家庭冲突、挖掘积极能力并配合法定教育矫治，丙需巩固学习和正向同伴关系。共同环节是尊重未成年人参与、保密和持续风险评估，而非简单用活动签到代表行为改善。',
  'scorePoints':['依法区分司法处遇与社工职责','逐人辨识风险/需求/优势','分级分类与个性化计划','未成年人参与及持续评估']},
 {'id':'Q2','number':2,'question':'设计司法社工、监护人、学校、社区与专业机构共同参与的协同服务路径，并说明职责边界。',
  'answer':'由有权限的司法机关明确法定处遇要求和委托范围，社工依据授权开展专业评估、服务目标协商与记录。与监护人开展家庭教育指导和亲子沟通支持，与学校讨论适当的学习衔接，与职业资源合作开展技能体验，与社区志愿项目提供安全的社会参与机会。对甲、乙、丙建立不同的责任人、时间节点与风险升级渠道。信息共享须说明用途，按最小必要原则取得适当授权并遵守未成年人保护规则；不应将涉案身份、会谈内容随意公开。专业评估不能替代调查取证和司法决定，服务计划不能违反任何法定程序安排。',
  'scorePoints':['司法与社工角色分工','家庭学校社区协同','分层服务行动和时间节点','依法保密与风险升级']},
 {'id':'Q3','number':3,'question':'针对照片传播、污名化和只统计活动场数的问题，说明如何开展权益保护与服务成效评估。',
  'answer':'应及时评估照片传播造成的识别与污名风险，依职责要求停止不必要的公开传播、采取适当纠正与保护措施，向未成年人以适龄方式说明信息使用情况。后续服务尽量采用去识别化报告和最小数据共享，建立监护人及未成年人参与的告知同意程序。评估除活动场次外，应依据各人目标追踪家庭沟通改善、学习或职业参与的稳定性、正向社会关系、风险因素变化和资源衔接情况。综合当事人反馈、机构服务记录与授权范围内的协作反馈，并说明观察期限、外部条件和不能凭短期自评推断长期再犯率的限制。',
  'scorePoints':['处理未成年人身份披露风险','反污名与参与权','个别化服务结果指标','过程证据、局限与持续追踪']}
 ],
 'adaptationSources':[{'name':'最高人民检察院、中央社会工作部：在未成年人检察工作中加强社会工作服务的典型案例（2025-05-28）','url':'https://www.hnhengdong.jcy.gov.cn/jcyw/202509/t20250918_7144726.shtml','role':'司法社工分层处遇与协同机制参考；所有人物经历与冲突细节为重新虚构'}]
}
]

def run():
    data=json.loads(BANK.read_text(encoding='utf-8'));cases=data['cases'];exp=additions('scenario_additions.txt');notes=additions('field_notes.txt')
    if len(cases) not in (84,86):raise AssertionError('unexpected input size')
    oldids={c['id'] for c in cases if c['sourceBatch']!='EXT-OFFICIAL'}
    if set(exp)!=oldids or set(notes)-oldids:raise AssertionError('case ids missing / surplus from narratives')
    for c in cases:
        if c['sourceBatch']=='EXT-OFFICIAL':continue
        is_original=c['sourceLabel']=='汇编材料（原卷未核验）'
        old=c.setdefault('originalMaterial',c['material']);c.setdefault('originalMaterialSha256',sha(old))
        if sha(old)!=c['originalMaterialSha256']:raise AssertionError('original altered: '+c['id'])
        extra='【原创补充训练情境·非试卷原文】\n'+exp[c['id']]
        if c['id'] in notes:extra+='\n\n【补充观察与访谈线索·原创】\n'+notes[c['id']]
        c['trainingExpansion']=extra
        c['trainingExpandedMaterial']=old+'\n\n'+extra
        c['caseExpansionReview']='AI生成的训练扩展材料；待独立专业终审，不具原卷或原事件证据资格'
        c['caseExpansionIsFiction']=True
        c['caseTrainingRevision']='V3.0.0-RC1'
        if is_original:
            # NEVER alter prior compiled text, even when incomplete.
            assert c['material']==old
            c['defaultMaterialView']='original_collected_text'
        else:
            c['material']=c['trainingExpandedMaterial'];c['defaultMaterialView']='enriched_training_material'
            c['sourceLabel']='原创强化训练案例（非历年原题）'
            if len(c['material'])<550:print('WARNING: under 550 chars',c['id'],len(c['material']))
    # Never overlap official mechanisms with school examination claims.
    existing={c['id'] for c in cases};cases.extend(c for c in PUBLIC_CASES if c['id'] not in existing)
    for c in PUBLIC_CASES:
        v=next(x for x in cases if x['id']==c['id']);v['examOriginalVerified']=False;v['canFullOriginalExam']=False
        v['canPracticeWholeCase']=True;v['caseExpansionIsFiction']=True
        v['caseExpansionReview']='依据公开权威典型案例中的专业机制重构情境；不保证场景要素与已裁判事实一致'
        v['defaultMaterialView']='adapted_training_material'
        v['originalMaterial']=v['material'];v['originalMaterialSha256']=sha(v['material'])
        for q in v['subquestions']:
            q['answerStatus']='训练参考解析（非校方标准答案）'
            q.setdefault('externalSources',[])
    data['schemaVersion']='3.0.0-RC1';data['notice']='78份原创案例加入多角色、时间线、访谈与行动约束；6份历史汇编原收录文字保持原样并只提供显著标识的虚构训练补充；另2份参考公开权威案例的机制重新编写。所有材料均不是已核验的东南大学原卷。'
    BANK.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest={
       'schemaVersion':'3.0.0-RC1', 'originalCaseCount':84,'enrichedOriginalTrainingCases':78,'unchangedCollectedHistoricalCases':6,
       'externalAdaptedCaseCount':2,'allCaseCount':len(cases),
       'cases':[{'id':c['id'],'initialSha256':c['originalMaterialSha256'],
        'originalLength':len(c['originalMaterial']),'displayLength':len(c['material']),
        'trainingExpandedLength':len(c.get('trainingExpandedMaterial',c['material'])),
        'provenance':c['sourceLabel'],'originalTextStillDisplayedByDefault':c['defaultMaterialView']=='original_collected_text'} for c in cases],
       'webSourceNotice':'External sources support domain mechanisms only, not the invented character narratives.'
    }
    (ROOT/'docs/case_material_enrichment_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('enriched fictional cases: 78; original collected cases: 6 preserved; official-inspired training cases: 2; total:',len(cases))
if __name__=='__main__':run()
