package com.datadog.iast.propagation;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import datadog.trace.api.iast.InstrumentationBridge;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class StringConcatBenchmark extends AbstractBenchmark<StringConcatBenchmark.Context> {

  @Override
  protected StringConcatBenchmark.Context initializeContext() {
    final String notTainted = new String("I am not a tainted string");
    final String tainted = new String("I am a tainted string");
    final IastRequestContext iastRequestContext = new IastRequestContext();
    iastRequestContext
        .getTaintedObjects()
        .taint(
            tainted,
            new Range[] {new Range(0, tainted.length(), new Source((byte) 0, "key", "value"))});
    return new StringConcatBenchmark.Context(iastRequestContext, notTainted, tainted);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String baseline() {
    final String self = context.notTainted;
    final String param = context.notTainted;
    return self.concat(param);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String iastDisabled() {
    final String self = context.notTainted;
    final String param = context.notTainted;
    final String result = self.concat(param);
    InstrumentationBridge.onStringConcat(self, param, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String notTainted() {
    final String self = context.notTainted;
    final String param = context.notTainted;
    final String result = self.concat(param);
    InstrumentationBridge.onStringConcat(self, param, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String paramTainted() {
    final String self = context.notTainted;
    final String param = context.tainted;
    final String result = self.concat(param);
    InstrumentationBridge.onStringConcat(self, param, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String stringTainted() {
    final String self = context.tainted;
    final String param = context.notTainted;
    final String result = self.concat(param);
    InstrumentationBridge.onStringConcat(self, param, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String bothTainted() {
    final String self = context.tainted;
    final String param = context.tainted;
    final String result = self.concat(param);
    InstrumentationBridge.onStringConcat(self, param, result);
    return result;
  }

  protected static class Context extends AbstractBenchmark.BenchmarkContext {

    private final String notTainted;
    private final String tainted;

    protected Context(
        final IastRequestContext iasContext, final String notTainted, final String tainted) {
      super(iasContext);
      this.notTainted = notTainted;
      this.tainted = tainted;
    }
  }
}