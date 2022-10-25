package com.datadog.iast

import datadog.trace.bootstrap.instrumentation.api.AgentScope
import datadog.trace.bootstrap.instrumentation.api.AgentSpan
import datadog.trace.bootstrap.instrumentation.api.ScopeSource
import datadog.trace.bootstrap.instrumentation.api.TagContext

class IastAgentSpanTest extends IastModuleImplTestBase {

  void 'activeSpan() returns the current active span'() {
    given:
    final mock = Mock(AgentSpan)

    when:
    final span = IastAgentSpan.activeSpan()

    then:
    mock == span
    1 * tracer.activeSpan() >> mock
    0 * _
  }

  void 'activeOrNewSpan() returns the current active span if not null'() {
    given:
    final mock = Mock(AgentSpan)

    when:
    final span = IastAgentSpan.activeOrNewSpan()

    then:
    mock == span
    1 * tracer.activeSpan() >> mock
    0 * _
  }

  void 'activeOrNewSpan() creates a new span if no context'() {
    when:
    final span = IastAgentSpan.activeOrNewSpan()

    then:
    span != null
    1 * tracer.activeSpan() >> null
    0 * _
  }

  void 'newly created span is lazily initialized'() {
    given:
    final mockSpan = Mock(AgentSpan)
    final mockScope = Mock(AgentScope)

    when: 'span is created'
    final span = IastAgentSpan.activeOrNewSpan() as IastAgentSpan.Initializable<AutoCloseable>

    then:
    1 * tracer.activeSpan() >> null
    0 * _

    when: 'span is lazily initialized'
    final result = span.init()

    then:
    result != null
    1 * tracer.startSpan('iast', _ as TagContext, false) >> mockSpan
    1 * tracer.activateSpan(mockSpan, ScopeSource.MANUAL) >> mockScope
    0 * _

    when: 'span is closed'
    result.close()

    then:
    1 * mockScope.close()
    1 * mockScope.span() >> mockSpan
    1 * mockSpan.finish()
    0 * _
  }
}
