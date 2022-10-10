package com.datadog.iast.propagation;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.datadog.iast.IastRequestContext;
import com.datadog.iast.IastSystem;
import datadog.trace.api.Config;
import datadog.trace.api.gateway.InstrumentationGateway;
import datadog.trace.api.gateway.RequestContextSlot;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.trace.bootstrap.instrumentation.api.TagContext;
import datadog.trace.common.writer.Writer;
import datadog.trace.core.CoreTracer;
import datadog.trace.core.DDSpan;
import java.util.List;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@OutputTimeUnit(NANOSECONDS)
@BenchmarkMode(Mode.SingleShotTime)
@Warmup(iterations = 50_000)
@Measurement(iterations = 5_000)
@Fork(value = 3)
public abstract class AbstractBenchmark<C extends AbstractBenchmark.BenchmarkContext> {

  protected AgentSpan span;
  protected AgentScope scope;
  protected C context;

  @Setup(Level.Trial)
  public void setup() {
    final InstrumentationGateway gateway = new InstrumentationGateway();
    IastSystem.start(gateway.getSubscriptionService(RequestContextSlot.IAST));
    final CoreTracer tracer =
        CoreTracer.builder().instrumentationGateway(gateway).writer(new NoOpWriter()).build();
    AgentTracer.forceRegister(tracer);
    context = initializeContext();
    final TagContext tagContext = new TagContext();
    if (Config.get().isIastEnabled()) {
      tagContext.withRequestContextDataIast(context.getIastContext());
    }
    span = tracer.startSpan("benchmark", tagContext, true);
    scope = tracer.activateSpan(span, ScopeSource.INSTRUMENTATION);
  }

  @Setup(Level.Iteration)
  public void start() {
    context = initializeContext();
    final AgentTracer.TracerAPI tracer = AgentTracer.get();
    final TagContext tagContext = new TagContext();
    if (Config.get().isIastEnabled()) {
      tagContext.withRequestContextDataIast(context.getIastContext());
    }
    span = tracer.startSpan("benchmark", tagContext, true);
    scope = tracer.activateSpan(span, ScopeSource.INSTRUMENTATION);
  }

  @TearDown(Level.Iteration)
  public void stop() {
    scope.close();
    span.finish();
  }

  protected abstract C initializeContext();

  protected abstract static class BenchmarkContext {

    private final IastRequestContext iastContext;

    protected BenchmarkContext(final IastRequestContext iasContext) {
      this.iastContext = iasContext;
    }

    public IastRequestContext getIastContext() {
      return iastContext;
    }
  }

  private static class NoOpWriter implements Writer {

    @Override
    public void write(final List<DDSpan> trace) {}

    @Override
    public void start() {}

    @Override
    public boolean flush() {
      return false;
    }

    @Override
    public void close() {}

    @Override
    public void incrementDropCounts(final int spanCount) {}
  }
}
