package datadog.trace.instrumentation.java.lang

import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.config.TracerConfig
import datadog.trace.api.iast.IastModule
import datadog.trace.api.iast.InstrumentationBridge
import foo.bar.TestStringSuite

class StringCallSiteTest extends AgentTestRunner {

  @Override
  protected void configurePreAgent() {
    injectSysConfig(TracerConfig.SCOPE_ITERATION_KEEP_ALIVE, "1") // don't let iteration spans linger
    injectSysConfig("dd.iast.enabled", "true")
  }

  def 'test string concat call site'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)

    when:
    final result = TestStringSuite.concat('Hello ', 'World!')

    then:
    result == 'Hello World!'
    1 * iastModule.onStringConcat('Hello ', 'World!', 'Hello World!')
    0 * _
  }

  def 'test string substring call site'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final self = '012345'
    final expected = '12345'

    when:
    final result = TestStringSuite.substring(self, 1)

    then:
    result == expected
    1 * iastModule.onStringSubSequence(self, 1, self.length(), expected)
    0 * _
  }

  def 'test string substring with endIndex call site'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final self = '012345'
    final expected = '1234'

    when:
    final result = TestStringSuite.substring(self, 1, 5)

    then:
    result == expected
    1 * iastModule.onStringSubSequence(self, 1, 5, expected)
    0 * _
  }

  def 'test string subSequence call site'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final self = '012345'
    final expected = '1234'

    when:
    final result = TestStringSuite.subSequence(self, 1, 5)

    then:
    result == expected
    1 * iastModule.onStringSubSequence(self, 1, 5, expected)
    0 * _
  }
}
