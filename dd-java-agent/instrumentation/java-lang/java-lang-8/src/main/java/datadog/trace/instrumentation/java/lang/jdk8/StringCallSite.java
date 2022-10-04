package datadog.trace.instrumentation.java.lang.jdk8;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.util.Maybe;
import datadog.trace.util.stacktrace.StackUtils;

@CallSite(spi = IastAdvice.class)
public class StringCallSite {

  @CallSite.After(
      "java.lang.String java.lang.String.join(java.lang.CharSequence, java.lang.CharSequence[])")
  public static String afterJoin(
      @CallSite.Argument final CharSequence delimiter,
      @CallSite.Argument final CharSequence[] elements,
      @CallSite.Return final String result) {
    InstrumentationBridge.onStringJoin(delimiter, elements, result);
    return result;
  }

  @CallSite.Around(
      "java.lang.String java.lang.String.join(java.lang.CharSequence, java.lang.Iterable)")
  public static String aroundJoin(
      @CallSite.Argument final CharSequence delimiter,
      @CallSite.Argument final Iterable<? extends CharSequence> elements)
      throws Throwable {
    try {
      Maybe<String> maybe = InstrumentationBridge.onStringJoin(delimiter, elements);
      if (!maybe.isPresent()) {
        return String.join(delimiter, elements);
      }
      return maybe.get();
    } catch (final Throwable e) {
      throw StackUtils.filterDatadog(e);
    }
  }
}
