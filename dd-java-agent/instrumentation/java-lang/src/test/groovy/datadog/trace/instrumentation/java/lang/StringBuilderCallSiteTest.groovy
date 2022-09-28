package datadog.trace.instrumentation.java.lang

import datadog.trace.agent.test.AgentTestRunner
import datadog.trace.api.config.TracerConfig
import datadog.trace.api.iast.IastModule
import datadog.trace.api.iast.InstrumentationBridge
import foo.bar.TestSuite

class StringBuilderCallSiteTest extends AgentTestRunner {

  @Override
  protected void configurePreAgent() {
    injectSysConfig(TracerConfig.SCOPE_ITERATION_KEEP_ALIVE, "1") // don't let iteration spans linger
    injectSysConfig("dd.iast.enabled", "true")
  }

  def 'test string builder new call site'(final CharSequence param, final String expected) {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)

    when:
    final result = param.class == String ?
      TestSuite.stringBuilderNew((String) param) :
      TestSuite.stringBuilderNew(param)

    then:
    result.toString() == expected
    if (param.class == String) {
      1 * iastModule.onStringBuilderAppend(_ as StringBuilder, (String) param)
    } else {
      1 * iastModule.onStringBuilderAppend(_ as StringBuilder, param)
    }
    0 * _

    where:
    param                            | expected
    new StringBuffer('Hello World!') | 'Hello World!'
    'Hello World!'                   | 'Hello World!'
  }

  def 'test string builder append call site'(final CharSequence param, final String expected) {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final target = new StringBuilder('Hello ')

    when:
    if (param.class == String) {
      TestSuite.stringBuilderAppend(target, (String) param)
    } else {
      TestSuite.stringBuilderAppend(target, param)
    }

    then:
    target.toString() == expected
    if (param.class == String) {
      1 * iastModule.onStringBuilderAppend(target, (String) param)
    } else {
      1 * iastModule.onStringBuilderAppend(target, param)
    }
    0 * _

    where:
    param                      | expected
    new StringBuffer('World!') | 'Hello World!'
    'World!'                   | 'Hello World!'
  }

  def 'test string builder toString call site'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)
    final target = new StringBuilder('Hello World!')

    when:
    final result = TestSuite.stringBuilderToString(target)

    then:
    result == 'Hello World!'
    1 * iastModule.onStringBuilderToString(target, _ as String)
    0 * _
  }

  def 'test string builder call site in plus operations (JDK8)'() {
    setup:
    final iastModule = Mock(IastModule)
    InstrumentationBridge.registerIastModule(iastModule)

    when:
    final result = TestSuite.stringPlus('Hello ', 'World!')

    then:
    result == 'Hello World!'
    1 * iastModule.onStringBuilderAppend(_ as StringBuilder, 'Hello ')
    1 * iastModule.onStringBuilderAppend(_ as StringBuilder, 'World!')
    1 * iastModule.onStringBuilderToString(_ as StringBuilder, _ as String)
    0 * _
  }
}
