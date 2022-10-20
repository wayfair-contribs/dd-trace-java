package datadog.trace.instrumentation.java.lang.jdk8;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;
import datadog.trace.util.stacktrace.StackUtils;
import java.util.ArrayList;
import java.util.List;

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
    // Iterate the iterable to guarantee the default behavior for custom mutable Iterables
    List<CharSequence> copy = new ArrayList<>();
    String result;
    try {
      elements.forEach(copy::add);
      result = String.join(delimiter, copy);
    } catch (final Throwable e) {
      throw StackUtils.filterDatadog(e);
    }
    InstrumentationBridge.onStringJoin(
        delimiter, copy.toArray(new CharSequence[copy.size()]), result);
    return result;
  }
}
