package com.datadog.iast;

import com.datadog.iast.model.Evidence;
import com.datadog.iast.model.Location;
import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import com.datadog.iast.model.SourceType;
import com.datadog.iast.model.Vulnerability;
import com.datadog.iast.model.VulnerabilityType;
import com.datadog.iast.overhead.Operations;
import com.datadog.iast.overhead.OverheadController;
import com.datadog.iast.taint.Ranges;
import com.datadog.iast.taint.TaintedObject;
import com.datadog.iast.taint.TaintedObjects;
import datadog.trace.api.Config;
import datadog.trace.api.iast.IastModule;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.api.AgentTracer;
import datadog.trace.util.Maybe;
import datadog.trace.util.stacktrace.StackWalker;
import datadog.trace.util.stacktrace.StackWalkerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class IastModuleImpl implements IastModule {

  private static final int NULL_STR_LENGTH = "null".length();

  private final Config config;
  private final Reporter reporter;
  private final OverheadController overheadController;
  private final StackWalker stackWalker = StackWalkerFactory.INSTANCE;

  public IastModuleImpl(
      final Config config, final Reporter reporter, final OverheadController overheadController) {
    this.config = config;
    this.reporter = reporter;
    this.overheadController = overheadController;
  }

  @Override
  public void onCipherAlgorithm(@Nullable final String algorithm) {
    if (algorithm == null) {
      return;
    }
    final String algorithmId = algorithm.toUpperCase(Locale.ROOT);
    if (!config.getIastWeakCipherAlgorithms().matcher(algorithmId).matches()) {
      return;
    }
    final AgentSpan span = AgentTracer.activeSpan();
    if (!overheadController.consumeQuota(Operations.REPORT_VULNERABILITY, span)) {
      return;
    }
    // get StackTraceElement for the callee of MessageDigest
    StackTraceElement stackTraceElement =
        stackWalker.walk(
            stack ->
                stack
                    .filter(s -> !s.getClassName().equals("javax.crypto.Cipher"))
                    .findFirst()
                    .get());

    Vulnerability vulnerability =
        new Vulnerability(
            VulnerabilityType.WEAK_CIPHER,
            Location.forSpanAndStack(span.getSpanId(), stackTraceElement),
            new Evidence(algorithm));
    reporter.report(span, vulnerability);
  }

  @Override
  public void onHashingAlgorithm(@Nullable final String algorithm) {
    if (algorithm == null) {
      return;
    }
    final String algorithmId = algorithm.toUpperCase(Locale.ROOT);
    if (!config.getIastWeakHashAlgorithms().contains(algorithmId)) {
      return;
    }
    final AgentSpan span = AgentTracer.activeSpan();
    if (!overheadController.consumeQuota(Operations.REPORT_VULNERABILITY, span)) {
      return;
    }
    // get StackTraceElement for the caller of MessageDigest
    StackTraceElement stackTraceElement =
        stackWalker.walk(
            stack ->
                stack
                    .filter(s -> !s.getClassName().equals("java.security.MessageDigest"))
                    .findFirst()
                    .get());

    Vulnerability vulnerability =
        new Vulnerability(
            VulnerabilityType.WEAK_HASH,
            Location.forSpanAndStack(span.getSpanId(), stackTraceElement),
            new Evidence(algorithm));
    reporter.report(span, vulnerability);
  }

  @Override
  public void onParameterName(@Nullable final String paramName) {
    if (paramName == null || paramName.isEmpty()) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    taintedObjects.taintInputString(
        paramName, new Source(SourceType.REQUEST_PARAMETER_NAME, paramName, null));
  }

  @Override
  public void onParameterValue(
      @Nullable final String paramName, @Nullable final String paramValue) {
    if (paramValue == null || paramValue.isEmpty()) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    taintedObjects.taintInputString(
        paramValue, new Source(SourceType.REQUEST_PARAMETER_VALUE, paramName, paramValue));
  }

  @Override
  public void onStringConcat(
      @Nullable final String left, @Nullable final String right, @Nullable final String result) {
    if (!canBeTainted(result)) {
      return;
    }
    if (!canBeTainted(left) && !canBeTainted(right)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final TaintedObject taintedLeft = getTainted(taintedObjects, left);
    final TaintedObject taintedRight = getTainted(taintedObjects, right);
    if (taintedLeft == null && taintedRight == null) {
      return;
    }
    final Range[] ranges;
    if (taintedRight == null) {
      ranges = taintedLeft.getRanges();
    } else if (taintedLeft == null) {
      final int shift = left == null ? NULL_STR_LENGTH : left.length();
      ranges = new Range[taintedRight.getRanges().length];
      Ranges.copyShift(taintedRight.getRanges(), ranges, 0, shift);
    } else {
      ranges = mergeRanges(left.length(), taintedLeft.getRanges(), taintedRight.getRanges());
    }
    taintedObjects.taint(result, ranges);
  }

  @Override
  public void onStringBuilderInit(
      @Nullable final StringBuilder builder, @Nullable final CharSequence param) {
    if (builder == null || !canBeTainted(param)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final TaintedObject paramTainted = taintedObjects.get(param);
    if (paramTainted == null) {
      return;
    }
    taintedObjects.taint(builder, paramTainted.getRanges());
  }

  @Override
  @SuppressFBWarnings("ES_COMPARING_PARAMETER_STRING_WITH_EQ")
  public void onStringSubSequence(
      @Nullable String self, int beginIndex, int endIndex, @Nullable CharSequence result) {
    if (!canBeTainted(self) || !canBeTainted(result) || self == result) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final Range[] rangesSelf = getRanges(taintedObjects.get(self));
    if (rangesSelf.length == 0) {
      return;
    }
    Range[] newRanges = Ranges.forSubstring(beginIndex, result.length(), rangesSelf);
    if (newRanges != null && newRanges.length > 0) {
      taintedObjects.taint(result, newRanges);
    }
  }

  @Override
  public void onStringJoin(
      @Nullable String result, CharSequence delimiter, CharSequence... elements) {
    if (!canBeTainted(result)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    List<Range> newRanges = new ArrayList<>();
    int pos = 0;
    // Delimiter info
    Range[] delimiterRanges = getRanges(getTainted(taintedObjects, delimiter));
    boolean delimiterHasRanges = delimiterRanges.length > 0;
    int delimiterLength = delimiter.length();

    for (int i = 0; i < elements.length; i++) {
      CharSequence element = elements[i];
      pos =
          getPositionAndUpdateRangesInStringJoin(
              taintedObjects,
              newRanges,
              pos,
              delimiterRanges,
              delimiterLength,
              element,
              delimiterHasRanges && i < elements.length - 1);
    }
    if (!newRanges.isEmpty()) {
      taintedObjects.taint(result, newRanges.toArray(new Range[0]));
    }
  }

  @Override
  public Maybe<String> onStringJoin(
      CharSequence delimiter, Iterable<? extends CharSequence> elements) {

    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return Maybe.Values.empty();
    }

    Objects.requireNonNull(delimiter);
    Objects.requireNonNull(elements);
    StringJoiner joiner = new StringJoiner(delimiter);

    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    List<Range> newRanges = new ArrayList<>();
    int pos = 0;
    // Delimiter info
    int delimiterLength = delimiter.length();
    Range[] delimiterRanges = getRanges(getTainted(taintedObjects, delimiter));
    boolean delimiterHasRanges = delimiterRanges.length > 0;

    Iterator<? extends CharSequence> iterator = elements.iterator();
    boolean hasExceptions = false;
    while (iterator.hasNext()) {
      CharSequence element = iterator.next();
      joiner.add(element);
      try {
        if (!hasExceptions) {
          pos =
              getPositionAndUpdateRangesInStringJoin(
                  taintedObjects,
                  newRanges,
                  pos,
                  delimiterRanges,
                  delimiterLength,
                  element,
                  delimiterHasRanges && iterator.hasNext());
        }
      } catch (Throwable e) {
        hasExceptions = true;
      }
    }
    String result = joiner.toString();
    if (!newRanges.isEmpty() && !hasExceptions) {
      taintedObjects.taint(result, newRanges.toArray(new Range[0]));
    }
    return Maybe.Values.of(result);
  }

  @Override
  public void onStringBuilderAppend(
      @Nullable final StringBuilder builder, @Nullable final CharSequence param) {
    if (!canBeTainted(builder) || !canBeTainted(param)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final TaintedObject paramTainted = taintedObjects.get(param);
    if (paramTainted == null) {
      return;
    }
    final TaintedObject builderTainted = taintedObjects.get(builder);
    final int shift = builder.length() - param.length();
    if (builderTainted == null) {
      final Range[] paramRanges = paramTainted.getRanges();
      final Range[] ranges = new Range[paramRanges.length];
      Ranges.copyShift(paramRanges, ranges, 0, shift);
      taintedObjects.taint(builder, ranges);
    } else {
      final Range[] builderRanges = builderTainted.getRanges();
      final Range[] paramRanges = paramTainted.getRanges();
      final Range[] ranges = mergeRanges(shift, builderRanges, paramRanges);
      builderTainted.setRanges(ranges);
    }
  }

  @Override
  public void onStringBuilderToString(
      @Nullable final StringBuilder builder, @Nullable final String result) {
    if (!canBeTainted(builder) || !canBeTainted(result)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final TaintedObject to = taintedObjects.get(builder);
    if (to == null) {
      return;
    }
    taintedObjects.taint(result, to.getRanges());
  }

  private static Range[] getRanges(final TaintedObject taintedObject) {
    return taintedObject == null ? Ranges.EMPTY : taintedObject.getRanges();
  }

  private static TaintedObject getTainted(final TaintedObjects to, final Object value) {
    return value == null ? null : to.get(value);
  }

  private static boolean canBeTainted(@Nullable final CharSequence s) {
    return s != null && s.length() > 0;
  }

  private static Range[] mergeRanges(
      final int offset, @Nonnull final Range[] rangesLeft, @Nonnull final Range[] rangesRight) {
    final int nRanges = rangesLeft.length + rangesRight.length;
    final Range[] ranges = new Range[nRanges];
    if (rangesLeft.length > 0) {
      System.arraycopy(rangesLeft, 0, ranges, 0, rangesLeft.length);
    }
    if (rangesRight.length > 0) {
      Ranges.copyShift(rangesRight, ranges, rangesLeft.length, offset);
    }
    return ranges;
  }

  /**
   * Iterates over the element ranges and delimiter ranges (if is necessary) to update them and calc
   * the new pos value
   */
  private static int getPositionAndUpdateRangesInStringJoin(
      TaintedObjects taintedObjects,
      List<Range> newRanges,
      Integer pos,
      Range[] delimiterRanges,
      int delimiterLength,
      CharSequence element,
      boolean addDelimiterRanges) {
    if (canBeTainted(element)) {
      Range[] elementRanges = getRanges(getTainted(taintedObjects, element));
      if (elementRanges.length > 0) {
        for (Range range : elementRanges) {
          newRanges.add(Ranges.createIfDifferent(range, range.getStart() + pos, range.getLength()));
        }
      }
      pos += element.length();
    }
    if (addDelimiterRanges) {
      for (Range range : delimiterRanges) {
        newRanges.add(Ranges.createIfDifferent(range, range.getStart() + pos, range.getLength()));
      }
    }
    pos += delimiterLength;
    return pos;
  }
}
