package com.datadog.iast.propagation;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import datadog.trace.api.iast.InstrumentationBridge;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class StringBuilderInitBenchmark
    extends AbstractBenchmark<StringBuilderInitBenchmark.Context> {

  @Override
  protected Context initializeContext() {
    final String notTainted = new String("I am not a tainted string");
    final String tainted = new String("I am a tainted string");
    final IastRequestContext iastRequestContext = new IastRequestContext();
    iastRequestContext
        .getTaintedObjects()
        .taint(
            tainted,
            new Range[] {new Range(0, tainted.length(), new Source((byte) 0, "key", "value"))});
    return new Context(iastRequestContext, notTainted, tainted);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public StringBuilder baseline() {
    return new StringBuilder(context.notTainted);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public StringBuilder iastDisabled() {
    final String param = context.notTainted;
    final StringBuilder self = new StringBuilder(param);
    InstrumentationBridge.onStringBuilderInit(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder notTainted() {
    final String param = context.notTainted;
    final StringBuilder self = new StringBuilder(param);
    InstrumentationBridge.onStringBuilderInit(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder tainted() {
    final String param = context.tainted;
    final StringBuilder self = new StringBuilder(param);
    InstrumentationBridge.onStringBuilderInit(self, param);
    return self;
  }

  protected static class Context extends AbstractBenchmark.BenchmarkContext {

    private final String notTainted;
    private final String tainted;

    protected Context(
        final IastRequestContext iasContext, final String notTainted, final String tainted) {
      super(iasContext);
      this.tainted = tainted;
      this.notTainted = notTainted;
    }
  }
}
