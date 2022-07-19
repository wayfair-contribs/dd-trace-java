package datadog.trace.instrumentation.jdbc;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.implementsInterface;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;

import com.google.auto.service.AutoService;
import datadog.trace.agent.tooling.Instrumenter;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class DB2ConnectionInstrumentation extends AbstractConnectionInstrumentation
    implements Instrumenter.ForTypeHierarchy {
  public DB2ConnectionInstrumentation() {
    super("jdbc", "db2");
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    return DB2PreparedStatementInstrumentation.HAS_DB2_CLASSES;
  }

  @Override
  public ElementMatcher<TypeDescription> hierarchyMatcher() {
    return implementsInterface(named("com.ibm.db2.jcc.DB2Connection"));
  }
}
