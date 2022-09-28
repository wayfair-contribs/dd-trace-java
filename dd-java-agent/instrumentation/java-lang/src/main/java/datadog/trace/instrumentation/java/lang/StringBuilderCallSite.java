package datadog.trace.instrumentation.java.lang;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;

@CallSite(spi = IastAdvice.class)
public class StringBuilderCallSite {

  @CallSite.AfterArray(
      value = {
        @CallSite.After("void java.lang.StringBuilder.<init>(java.lang.String)"),
        @CallSite.After("void java.lang.StringBuilder.<init>(java.lang.CharSequence)")
      })
  public static StringBuilder afterInit(
      @CallSite.This final StringBuilder self, @CallSite.Argument final CharSequence param) {
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return self;
  }

  @CallSite.AfterArray(
      value = {
        @CallSite.After("java.lang.StringBuilder java.lang.StringBuilder.append(java.lang.String)"),
        @CallSite.After(
            "java.lang.StringBuilder java.lang.StringBuilder.append(java.lang.CharSequence)")
      })
  public static StringBuilder afterAppend(
      @CallSite.This final StringBuilder self,
      @CallSite.Argument final CharSequence param,
      @CallSite.Return final StringBuilder result) {
    InstrumentationBridge.onStringBuilderAppend(self, param);
    return result;
  }

  @CallSite.After("java.lang.String java.lang.StringBuilder.toString()")
  public static String afterToString(
      @CallSite.This final StringBuilder self, @CallSite.Return final String result) {
    InstrumentationBridge.onStringBuilderToString(self, result);
    return result;
  }
}
