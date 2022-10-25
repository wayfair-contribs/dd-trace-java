package com.datadog.iast.model

import datadog.trace.api.DDId
import datadog.trace.bootstrap.instrumentation.api.AgentSpan
import datadog.trace.test.util.DDSpecification

class LocationTest extends DDSpecification {

  void 'forStack'() {
    given:
    final span = Mock(AgentSpan)
    final spanId = DDId.from(123456)
    span.getSpanId() >> spanId
    final stack = new StackTraceElement("declaringClass", "methodName", "fileName", 42)

    when:
    final location = Location.forSpanAndStack(span, stack)

    then:
    location.getSpanId() == spanId
    location.getPath() == "declaringClass"
    location.getLine() == 42
  }
}
