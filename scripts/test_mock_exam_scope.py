from pathlib import Path
import subprocess,textwrap
import tempfile
ROOT=Path(__file__).resolve().parents[1]
work=Path(tempfile.mkdtemp(prefix='v31_mock_scope_'))
pkg=work/'com/seu/socialworkcards';pkg.mkdir(parents=True,exist_ok=True)
src=ROOT/'app/src/main/java/com/seu/socialworkcards/MockExamEngine.java'
(pkg/'MockExamEngine.java').write_bytes(src.read_bytes())
(pkg/'Card.java').write_text('''package com.seu.socialworkcards;import java.util.*;public class Card { public String id="",category="",questionType="",kind="",sourceModule="BASE",examSubjectScope="331,437"; public java.util.List<String> exactExamSources=new ArrayList<>(),recallExamSources=new ArrayList<>();public Card(String id,String type,String module){this.id=id;this.questionType=type;this.kind=type;this.sourceModule=module;} }''')
(pkg/'CardRepository.java').write_text('''package com.seu.socialworkcards;import java.util.*;public class CardRepository {public List<Card> cards=new ArrayList<>(),v24Cards=new ArrayList<>();public boolean isV24(Card c){return "V24_437".equals(c.sourceModule);}public boolean isRealExam(Card c){return false;}public boolean isExactExam(Card c){return false;}public boolean isRecallExam(Card c){return false;}public boolean matchesExamSubject(Card c,String sub){return ("331".equals(sub)||"437".equals(sub))&&(!isV24(c)||"437".equals(sub));}public String questionType(Card c){return c.questionType;}public String systemFrequency(Card c){return "中";}}''')
(pkg/'StudyStore.java').write_text('''package com.seu.socialworkcards;import java.util.*;public class StudyStore{public static class State{public List<String> history=new ArrayList<>();public String tag="未学习";}public boolean isFavorite(String id){return false;}public String frequencyOverride(String id){return "";}public State state(String id){return new State();}}''')
(pkg/'ReviewEngine.java').write_text('''package com.seu.socialworkcards;public class ReviewEngine {public double difficulty(Card c){return 0.5;}}''')
(pkg/'TestMock.java').write_text('''package com.seu.socialworkcards;import java.util.*;public class TestMock{public static void main(String[]args){
 CardRepository r=new CardRepository(); for(int i=0;i<378;i++)r.v24Cards.add(new Card(String.format("437M%03d",i+1),"名词解释","V24_437"));
 for(int i=0;i<80;i++){String typ=i<30?"名词解释":i<50?"简答题":i<64?"论述题":"案例分析题";r.cards.add(new Card(String.format("C%04d",i+1),typ,"BASE"));}
 MockExamEngine mock=new MockExamEngine(r,new StudyStore(),new ReviewEngine());boolean v24AppearedIn437=false;
 for(int n=0;n<300;n++){for(String sub:new String[]{"331","437"}){MockExamEngine.Paper p=mock.generate(sub);
 for(MockExamEngine.Section s:p.sections)for(Card c:s.cards){if("331".equals(sub)&&c.id.startsWith("437M"))throw new AssertionError("V24 leaked to 331");if(c.id.startsWith("437M")){if(!"437".equals(sub)||!"名词解释".equals(s.targetType))throw new AssertionError("bad source/type");v24AppearedIn437=true;}String t=c.questionType;boolean valid=s.targetType.equals("名词解释")?t.contains("名词"):s.targetType.equals("简答题")?t.contains("简答"):s.targetType.equals("论述题")?t.contains("论述"):(t.contains("案例")||t.contains("材料"));if(!valid)throw new AssertionError("type fallback violation "+t);}
 }}
 if(!v24AppearedIn437)throw new AssertionError("V24 not sampled in 437");
 r.cards.clear();for(int n=0;n<50;n++){MockExamEngine.Paper p=mock.generate("331");if(!p.flattened().isEmpty())throw new AssertionError("Empty 331 pool fallback must remain empty");}
 System.out.println("PASS 300x2 paper simulations; 331 never samples V24; 437 V24 noun eligibility; no fallback cross-types; empty 331 remains empty");}}''')
cmd=['javac','-encoding','UTF-8','-d',str(work)]+[str(q) for q in pkg.glob('*.java')]
a=subprocess.run(cmd,capture_output=True,text=True);print('compile',a.returncode,a.stderr[:4000]);assert a.returncode==0
b=subprocess.run(['java','-cp',str(work),'com.seu.socialworkcards.TestMock'],capture_output=True,text=True);print('run',b.returncode,b.stdout,b.stderr);assert b.returncode==0
