package com.datadog.iast;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.trace.bootstrap.instrumentation.api.ScopeSource;
import datadog.trace.bootstrap.instrumentation.api.TagContext;
import java.lang.reflect.Proxy;

public class IastAgentSpan {

  private static final Class<?>[] PROXY_INTERFACES =
      new Class<?>[] {AgentSpan.class, AutoCloseable.class, Initializable.class};
  private static final ClassLoader CLASS_LOADER = IastModuleImpl.class.getClassLoader();
  private static final AutoCloseable EMPTY_AUTO_CLOSEABLE = () -> {};

  static AgentSpan activeSpan() {
    return AgentTracer.activeSpan();
  }

  @SuppressWarnings("resource")
  static AgentSpan activeOrNewSpan() {
    final AgentSpan span = activeSpan();
    if (span != null) {
      return span;
    }
    final AgentScope[] scope = new AgentScope[1];
    return (AgentSpan)
        Proxy.newProxyInstance(
            CLASS_LOADER,
            PROXY_INTERFACES,
            (proxy, method, args) -> {
              if (method.getDeclaringClass() == Initializable.class) {
                scope[0] = initActiveSpan();
                return proxy;
              } else if (method.getDeclaringClass() == AutoCloseable.class) {
                closeSpan(scope[0]);
                scope[0] = null;
                return null;
              } else {
                return scope[0] == null ? null : method.invoke(scope[0].span(), args);
              }
            });
  }

  @SuppressWarnings("unchecked")
  static AutoCloseable initSpan(final AgentSpan object) {
    if (Proxy.isProxyClass(object.getClass())) {
      return ((Initializable<AutoCloseable>) object).init();
    } else {
      return EMPTY_AUTO_CLOSEABLE;
    }
  }

  private static AgentScope initActiveSpan() {
    final AgentTracer.TracerAPI tracer = AgentTracer.get();
    final TagContext tagContext =
        new TagContext().withRequestContextDataIast(new IastRequestContext());
    final AgentSpan span = tracer.startSpan("iast", tagContext, false);
    return tracer.activateSpan(span, ScopeSource.MANUAL);
  }

  private static void closeSpan(final AgentScope scope) {
    if (scope != null) {
      final AgentSpan span = scope.span();
      span.finish();
      scope.close();
    }
  }

  public interface Initializable<E> {
    E init();
  }
}
