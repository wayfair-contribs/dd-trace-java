package com.datadog.iast.propagation;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import datadog.trace.api.iast.InstrumentationBridge;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class StringBuilderAppendBenchmark
    extends AbstractBenchmark<StringBuilderAppendBenchmark.Context> {

  @Override
  protected Context initializeContext() {
    final String notTainted = new String("I am not a tainted string");
    final String tainted = new String("I am a tainted string");
    final StringBuilder notTaintedBuilder = new StringBuilder("I am not a tainted string builder");
    final StringBuilder taintedBuilder = new StringBuilder("I am a tainted string builder");
    final IastRequestContext iastRequestContext = new IastRequestContext();
    iastRequestContext
        .getTaintedObjects()
        .taint(
            tainted,
            new Range[] {new Range(0, tainted.length(), new Source((byte) 0, "key", "value"))});
    iastRequestContext
        .getTaintedObjects()
        .taint(
            taintedBuilder,
            new Range[] {
              new Range(0, taintedBuilder.length(), new Source((byte) 0, "key", "value"))
            });
    return new Context(iastRequestContext, notTainted, tainted, notTaintedBuilder, taintedBuilder);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public StringBuilder baseline() {
    return context.notTaintedBuilder.append(context.notTainted);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public StringBuilder iastDisabled() {
    final String param = context.notTainted;
    final StringBuilder self = context.notTaintedBuilder.append(param);
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder notTainted() {
    final String param = context.notTainted;
    final StringBuilder self = context.notTaintedBuilder.append(param);
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder paramTainted() {
    final String param = context.tainted;
    final StringBuilder self = context.notTaintedBuilder.append(param);
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder stringBuilderTainted() {
    final String param = context.notTainted;
    final StringBuilder self = context.taintedBuilder.append(param);
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public StringBuilder bothTainted() {
    final String param = context.tainted;
    final StringBuilder self = context.taintedBuilder.append(param);
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  protected static class Context extends AbstractBenchmark.BenchmarkContext {

    private final String notTainted;
    private final String tainted;

    private final StringBuilder notTaintedBuilder;

    private final StringBuilder taintedBuilder;

    protected Context(
        final IastRequestContext iasContext,
        final String notTainted,
        final String tainted,
        final StringBuilder notTaintedBuilder,
        final StringBuilder taintedBuilder) {
      super(iasContext);
      this.tainted = tainted;
      this.notTainted = notTainted;
      this.notTaintedBuilder = notTaintedBuilder;
      this.taintedBuilder = taintedBuilder;
    }
  }
}
