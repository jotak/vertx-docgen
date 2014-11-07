package io.vertx.docgen;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Compiler {

  final File dest;
  final Collection<File> sources;
  final TestGenProcessor processor;
  final StandardJavaFileManager fileManager;
  final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
  final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

  public Compiler(Collection<File> sources, File dest) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    this.fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charset.forName("UTF-8"));
    this.dest = dest;
    this.sources = sources;
    this.processor = new TestGenProcessor();
  }

  void failCompile() {
    JavaCompiler.CompilationTask task = createTask(sources, diagnostics);
    if (task.call()) {
      throw new AssertionError("Was expecting compilation to fail");
    }
  }

  void assertCompile() {
    JavaCompiler.CompilationTask task = createTask(sources, diagnostics);
    if (!task.call()) {
      StringWriter buffer = new StringWriter();
      buffer.append("Could not compile").append(":\n");
      for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
        buffer.append(diagnostic.toString()).append("\n");
      }
      throw new AssertionError(buffer.toString());
    }
  }

  private JavaCompiler.CompilationTask createTask(Collection<File> sources, DiagnosticListener<? super JavaFileObject> diagnostics) {
    if (!dest.mkdirs()) {
      assertTrue(dest.exists());
    }
    assertTrue(dest.isDirectory());
    try {
      fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(dest));
    } catch (IOException e) {
      throw new AssertionError("Could not set location", e);
    }
    Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjects(sources.toArray(new File[sources.size()]));
    JavaCompiler.CompilationTask task = compiler.getTask(new OutputStreamWriter(System.out), fileManager, diagnostics, Collections.<String>emptyList(), Collections.<String>emptyList(), files);
    task.setLocale(Locale.ENGLISH);
    task.setProcessors(Collections.singleton(processor));
    return task;
  }

}
