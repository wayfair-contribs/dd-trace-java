package datadog.trace.instrumentation.java.lang.jdk11;

import datadog.trace.agent.tooling.csi.CallSite;
import datadog.trace.api.iast.IastAdvice;
import datadog.trace.api.iast.InstrumentationBridge;

// TODO add minJavaVersion = 11 when @Callsite updated
@CallSite(spi = IastAdvice.class)
public class StringCallSite {
  @CallSite.After("java.lang.String java.lang.String.repeat(int)")
  public static String afterRepeat(
      @CallSite.This final String self,
      @CallSite.Argument final int count,
      @CallSite.Return final String result) {
    InstrumentationBridge.onStringRepeat(self, count, result);
    return result;
  }
}
