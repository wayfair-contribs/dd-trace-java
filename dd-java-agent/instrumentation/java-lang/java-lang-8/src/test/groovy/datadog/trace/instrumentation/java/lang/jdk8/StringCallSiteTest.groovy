package datadog.trace.instrumentation.java.lang.jdk8

import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.config.TracerConfig
import datadog.trace.api.iast.IastModule
import datadog.trace.api.iast.InstrumentationBridge
import foo.bar.TestStringSuite
import spock.lang.Requires

@Requires({
  jvm.java8Compatible
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
    final expected = '012-345'
    InstrumentationBridge.registerIastModule(iastModule)

    when:
    final result = TestStringSuite.join('-', '012', '345')

    then:
    result == expected
    1 * iastModule.onStringJoin(expected, '-', '012', '345')
    0 * _
  }

  def 'test string join with Iterable call site'() {
    setup:
    final iastModule = Mock(IastModule)
    final expected = '012-345'
    InstrumentationBridge.registerIastModule(iastModule)
    final iterable = Arrays.asList('012', '345')

    when:
    final result = TestStringSuite.join('-', iterable)

    then:
    result == expected
    1 * iastModule.onStringJoin(expected, '-', '012', '345')
    0 * _
  }

  def 'test string join with Iterable fail on iterable copy'() {

    given:
    final iterable = Mock(Iterable)

    when:
    TestStringSuite.join('-', iterable)

    then:
    1 * iterable.forEach(_) >> { throw new Error('Boom!!!') }
    thrown Error
  }
}
