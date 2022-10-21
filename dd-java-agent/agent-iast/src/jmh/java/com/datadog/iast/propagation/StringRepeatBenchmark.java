package com.datadog.iast.propagation;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;

public class StringRepeatBenchmark
    extends AbstractBenchmark<StringRepeatBenchmark.Context> {

  @Override
  protected Context initializeContext() {
    final String notTainted = new String("I am not a tainted string");
    final String tainted = new String("I am a tainted string");
    final IastRequestContext iastRequestContext = new IastRequestContext();
    iastRequestContext
        .getTaintedObjects()
        .taint(
            tainted,
            new Range[] {
              new Range(0, tainted.length(), new Source((byte) 0, "key", "value"))
            });
    return new Context(iastRequestContext, notTainted, tainted);
  }

  /*
  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String baseline() {
    return context.notTainted.repeat(2);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String iastDisabled() {
    final String self = context.notTainted;
    final String result = self.toString();
    InstrumentationBridge.onStringToString(self, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String notTainted() {
    final String self = context.notTainted;
    final String result = self.toString();
    InstrumentationBridge.onStringToString(self, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String tainted() {
    final String self = context.tainted;
    final String result = self.toString();
    InstrumentationBridge.onStringToString(self, result);
    return result;
  }

   */

  protected static class Context extends BenchmarkContext {

    private final String notTainted;

    private final String tainted;

    protected Context(
        final IastRequestContext iasContext,
        final String notTainted,
        final String tainted) {
      super(iasContext);
      this.notTainted = notTainted;
      this.tainted = tainted;
    }
  }
}
