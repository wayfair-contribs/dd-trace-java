package datadog.trace.instrumentation.java.lang.jdk11

import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.config.TracerConfig
import datadog.trace.api.iast.IastModule
import datadog.trace.api.iast.InstrumentationBridge
import foo.bar.TestStringSuite
import spock.lang.Requires

@Requires({
  jvm.java11Compatible
})
class StringCallSiteTest extends AgentTestRunner {

  @Override
  protected void configurePreAgent() {
    injectSysConfig(TracerConfig.SCOPE_ITERATION_KEEP_ALIVE, "1") // don't let iteration spans linger
    injectSysConfig("dd.iast.enabled", "true")
  }

  def 'test string join call site'() {
    setup:
    final iastModule = Mock(IastModule)
    final self = 'abc'
    final count = 3
    final expected = 'abcabcabc'
    InstrumentationBridge.registerIastModule(iastModule)


    when:
    final result = TestStringSuite.stringRepeat(self, count)

    then:
    result == expected
    1 * iastModule.onStringRepeat(self, count, expected)
    0 * _
  }
}
