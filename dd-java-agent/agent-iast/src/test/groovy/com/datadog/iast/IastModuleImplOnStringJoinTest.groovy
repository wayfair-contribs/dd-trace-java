package com.datadog.iast


import datadog.trace.api.gateway.RequestContext
import datadog.trace.api.gateway.RequestContextSlot
import datadog.trace.bootstrap.instrumentation.api.AgentSpan
import spock.lang.Shared

import static com.datadog.iast.taint.TaintUtils.addFromTaintFormat
import static com.datadog.iast.taint.TaintUtils.fromTaintFormat
import static com.datadog.iast.taint.TaintUtils.getStringFromTaintFormat
import static com.datadog.iast.taint.TaintUtils.taintFormat

class IastModuleImplOnStringJoinTest extends IastModuleImplTestBase {

  @Shared
  private List<Object> objectHolder = []

  def setup() {
    objectHolder.clear()
  }

  void 'onStringJoin without span (#delimiter, #elements)'(final CharSequence delimiter, final CharSequence[] elements, final int mockCalls) {
    given:
    final result = String.join(delimiter, elements)

    when:
    module.onStringJoin(result, delimiter, elements)

    then:
    mockCalls * tracer.activeSpan() >> null
    0 * _

    where:
    delimiter              | elements                                             | mockCalls
    ""                     | ["123", "456"]                                       | 1
    "-"                    | ["123", "456"]                                       | 1
    ""                     | []                                                   | 0
    "-"                    | []                                                   | 0
    ""                     | [new StringBuilder("123"), new StringBuilder("456")] | 1
    "-"                    | [new StringBuilder("123"), new StringBuilder("456")] | 1
    new StringBuilder()    | ["123", "456"]                                       | 1
    new StringBuilder("-") | ["123", "456"]                                       | 1
    new StringBuilder()    | []                                                   | 0
    new StringBuilder("-") | []                                                   | 0
    new StringBuilder("")  | [new StringBuilder("123"), new StringBuilder("456")] | 1
    new StringBuilder("-") | [new StringBuilder("123"), new StringBuilder("456")] | 1
  }


  void 'onStringJoin (#delimiter, #elements)'(final CharSequence delimiter, final CharSequence[] elements, final String expected) {
    given:
    final span = Mock(AgentSpan)
    tracer.activeSpan() >> span
    final reqCtx = Mock(RequestContext)
    span.getRequestContext() >> reqCtx
    final ctx = new IastRequestContext()
    reqCtx.getData(RequestContextSlot.IAST) >> ctx

    and:
    final result = getStringFromTaintFormat(expected)
    objectHolder.add(expected)
    final shouldBeTainted = fromTaintFormat(expected) != null

    and:
    final taintedObjects = ctx.getTaintedObjects()
    final fromTaintedDelimiter = addFromTaintFormat(taintedObjects, delimiter)
    objectHolder.add(fromTaintedDelimiter)

    and:
    final fromTaintedElements = new CharSequence[elements.length]
    elements.eachWithIndex { element, i ->
      def el = addFromTaintFormat(taintedObjects, element)
      objectHolder.add(el)
      fromTaintedElements[i] = el
    }

    when:
    module.onStringJoin(result, fromTaintedDelimiter, fromTaintedElements)

    then:
    assert result == String.join(fromTaintedDelimiter, fromTaintedElements)

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
    delimiter              | elements                                                                                    | expected
    "-"                    | ["0==>12<==3", "456", "7==>89<=="]                                                          | "0==>12<==3-456-7==>89<=="
    "-"                    | [new StringBuilder("0==>12<==3"), new StringBuilder("456"), new StringBuilder("7==>89<==")] | "0==>12<==3-456-7==>89<=="
    new StringBuilder("-") | ["0==>12<==3", "456", "7==>89<=="]                                                          | "0==>12<==3-456-7==>89<=="
    new StringBuilder("-") | [new StringBuilder("0==>12<==3"), new StringBuilder("456"), new StringBuilder("7==>89<==")] | "0==>12<==3-456-7==>89<=="
    "-"                    | ["0123", "456", "789"]                                                                      | "0123-456-789"
    "==>TAINTED<=="        | ["0123", "456", "789"]                                                                      | "0123==>TAINTED<==456==>TAINTED<==789"
    ", "                   | ["untainted", null]                                                                         | "untainted, null"
    ", "                   | ["untainted", "another"]                                                                    | "untainted, another"
    ", "                   | ["stringParam==>taintedString<==", "another"]                                               | "stringParam==>taintedString<==, another"
    ", "                   | ["stringParam==>taintedString<==", null]                                                    | "stringParam==>taintedString<==, null"
    ", "                   | ["stringParam:another", "==>taintedString<=="]                                              | "stringParam:another, ==>taintedString<=="
    ", "                   | [
      "stringParam1,stringParam2,stringParam3:==>taintedString<==",
      "==>taintedString<==",
      "==>taintedString<=="
    ]                                                                                                                    | "stringParam1,stringParam2,stringParam3:==>taintedString<==, ==>taintedString<==, ==>taintedString<=="
  }
}
