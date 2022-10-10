package com.datadog.iast.propagation;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import datadog.trace.api.iast.InstrumentationBridge;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

public class StringBuilderToStringBenchmark
    extends AbstractBenchmark<StringBuilderToStringBenchmark.Context> {

  @Override
  protected Context initializeContext() {
    final StringBuilder notTaintedBuilder = new StringBuilder("I am not a tainted string builder");
    final StringBuilder taintedBuilder = new StringBuilder("I am a tainted string builder");
    final IastRequestContext iastRequestContext = new IastRequestContext();
    iastRequestContext
        .getTaintedObjects()
        .taint(
            taintedBuilder,
            new Range[] {
              new Range(0, taintedBuilder.length(), new Source((byte) 0, "key", "value"))
            });
    return new Context(iastRequestContext, notTaintedBuilder, taintedBuilder);
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String baseline() {
    return context.notTaintedBuilder.toString();
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=false"})
  public String iastDisabled() {
    final StringBuilder self = context.notTaintedBuilder;
    final String result = self.toString();
    InstrumentationBridge.onStringBuilderToString(self, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String notTainted() {
    final StringBuilder self = context.notTaintedBuilder;
    final String result = self.toString();
    InstrumentationBridge.onStringBuilderToString(self, result);
    return result;
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-Ddd.iast.enabled=true"})
  public String tainted() {
    final StringBuilder self = context.taintedBuilder;
    final String result = self.toString();
    InstrumentationBridge.onStringBuilderToString(self, result);
    return result;
  }

  protected static class Context extends AbstractBenchmark.BenchmarkContext {

    private final StringBuilder notTaintedBuilder;

    private final StringBuilder taintedBuilder;

    protected Context(
        final IastRequestContext iasContext,
        final StringBuilder notTaintedBuilder,
        final StringBuilder taintedBuilder) {
      super(iasContext);
      this.notTaintedBuilder = notTaintedBuilder;
      this.taintedBuilder = taintedBuilder;
    }
  }
}
