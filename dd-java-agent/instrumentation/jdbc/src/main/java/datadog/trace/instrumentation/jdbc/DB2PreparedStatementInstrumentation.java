package datadog.trace.instrumentation.jdbc;

import static datadog.trace.agent.tooling.bytebuddy.matcher.ClassLoaderMatchers.hasClassesNamed;
import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class DB2PreparedStatementInstrumentation extends AbstractPreparedStatementInstrumentation
    implements Instrumenter.ForTypeHierarchy {
  public DB2PreparedStatementInstrumentation() {
    super("jdbc", "db2");
  }

  // Optimization for expensive typeMatcher.
  static final ElementMatcher<ClassLoader> HAS_DB2_CLASSES =
      hasClassesNamed("com.ibm.db2.jcc.DB2PreparedStatement");

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    return HAS_DB2_CLASSES;
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return implementsInterface(named("com.ibm.db2.jcc.DB2PreparedStatement"));
  }
}
