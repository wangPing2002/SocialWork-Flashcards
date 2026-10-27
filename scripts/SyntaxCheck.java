import javax.tools.*;
import com.sun.source.util.*;
import java.io.*;
import java.util.*;
public class SyntaxCheck {
  public static void main(String[] args) throws Exception {
    JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
    StandardJavaFileManager fm=compiler.getStandardFileManager(diagnostics,null,null);
    Iterable<? extends JavaFileObject> files=fm.getJavaFileObjects(args);
    JavacTask task=(JavacTask)compiler.getTask(null,fm,diagnostics,Arrays.asList("-proc:none"),null,files);
    task.parse();
    int errors=0;
    for(Diagnostic<? extends JavaFileObject> d:diagnostics.getDiagnostics()) {
      if(d.getKind()==Diagnostic.Kind.ERROR){System.out.println("ERROR "+d.getSource()+":"+d.getLineNumber()+" "+d.getMessage(null));errors++;}
    }
    if(errors>0)System.exit(1);
    System.out.println("PASS Java source grammar on "+args.length+" source files (Android linking not checked)");
  }
}
