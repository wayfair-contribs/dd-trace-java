package com.datadog.iast

import datadog.trace.api.gateway.RequestContext
import datadog.trace.api.gateway.RequestContextSlot
import datadog.trace.bootstrap.instrumentation.api.AgentSpan
import spock.lang.Shared

import static com.datadog.iast.taint.TaintUtils.addFromTaintFormat
import static com.datadog.iast.taint.TaintUtils.fromTaintFormat
import static com.datadog.iast.taint.TaintUtils.getStringFromTaintFormat
import static com.datadog.iast.taint.TaintUtils.taintFormat

class IastModuleImplOnStringRepeatTest extends IastModuleImplTestBase {

  @Shared
  private List<Object> objectHolder = []

  def setup() {
    objectHolder.clear()
  }

  void 'onStringRepeat that can not be tainted (#self, #count)'(final String self, final int count, final String result) {
    when:
    module.onStringRepeat(self, count, result)
    then:
    0 * _
    where:
    self  | count | result
    ""    | 1     | ""
    "abc" | 0     | ""
    null  | 1     | ""
    null  | 0     | ""
    "abc" | 1     | "abc"
  }

  void 'onStringRepeat without span (#self, #count)'(final String self, final int count, final String result, final int mockCalls) {
    when:
    module.onStringRepeat(self, count, result)
    then:
    mockCalls * tracer.activeSpan() >> null
    0 * _
    where:
    self  | count | result   | mockCalls
    ""    | 0     | ""       | 0
    null  | 0     | ""       | 0
    ""    | 1     | ""       | 0
    null  | 1     | ""       | 0
    "abc" | 1     | 'abc'    | 0
    "abc" | 2     | 'abcabc' | 1
  }

  void 'onStringRepeat (#self, #count, #result)'() {
    given:
    final span = Mock(AgentSpan)
    tracer.activeSpan() >> span
    final reqCtx = Mock(RequestContext)
    span.getRequestContext() >> reqCtx
    final ctx = new IastRequestContext()
    reqCtx.getData(RequestContextSlot.IAST) >> ctx
    and:
    final taintedObjects = ctx.getTaintedObjects()
    self = addFromTaintFormat(taintedObjects, self)
    objectHolder.add(self)
    and:
    final result = getStringFromTaintFormat(expected)
    objectHolder.add(expected)
    final shouldBeTainted = fromTaintFormat(expected) != null
    when:
    module.onStringRepeat(self, count, result)
    then:
    1 * tracer.activeSpan() >> span
    1 * span.getRequestContext() >> reqCtx
    1 * reqCtx.getData(RequestContextSlot.IAST) >> ctx
    0 * _
    def to = ctx.getTaintedObjects().get(result)
    if (shouldBeTainted) {
      assert to != null
      assert to.get() == result
      assert taintFormat(to.get() as String, to.getRanges()) == expected
    } else {
      assert to == null
    }
    where:
    self                | count | expected
    "abc"               | 2     | "abcabc"
    "==>b<=="           | 2     | "==>b<====>b<=="
    "aa==>b<=="         | 2     | "aa==>b<==aa==>b<=="
    "==>b<==cc"         | 2     | "==>b<==cc==>b<==cc"
    "a==>b<==c"         | 2     | "a==>b<==ca==>b<==c"
    "a==>b<==c==>d<==e" | 2     | "a==>b<==c==>d<==ea==>b<==c==>d<==e"
  }
}
