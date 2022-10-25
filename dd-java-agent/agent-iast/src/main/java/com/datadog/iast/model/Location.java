package com.datadog.iast.model;

import datadog.trace.api.DDId;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;

public final class Location {

  private final String path;

  private final int line;

  private final AgentSpan span;

  private Location(final AgentSpan span, final String path, final int line) {
    this.span = span;
    this.path = path;
    this.line = line;
  }

  public static Location forSpanAndStack(final AgentSpan span, final StackTraceElement stack) {
    return new Location(span, stack.getClassName(), stack.getLineNumber());
  }

  public DDId getSpanId() {
    return span == null ? null : span.getSpanId();
  }

  public String getPath() {
    return path;
  }

  public int getLine() {
    return line;
  }
}
