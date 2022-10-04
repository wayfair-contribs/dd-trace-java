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
import datadog.trace.instrumentation.iastinstrumenter.IastExclusionTrie;
import datadog.trace.util.stacktrace.StackWalker;
import datadog.trace.util.stacktrace.StackWalkerFactory;
import java.util.Locale;
import java.util.stream.Stream;
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
        stackWalker.walk(IastModuleImpl::findValidPackageForVulnerability);

    Vulnerability vulnerability =
        new Vulnerability(
            VulnerabilityType.WEAK_CIPHER,
            Location.forStack(stackTraceElement),
            new Evidence(algorithm));
    reporter.report(span, vulnerability);
  }

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
        stackWalker.walk(IastModuleImpl::findValidPackageForVulnerability);

    Vulnerability vulnerability =
        new Vulnerability(
            VulnerabilityType.WEAK_HASH,
            Location.forStack(stackTraceElement),
            new Evidence(algorithm));
    reporter.report(span, vulnerability);
  }

  @Override
  public void onJdbcQuery(@Nonnull String queryString) {
    final AgentSpan span = AgentTracer.activeSpan();
    final IastRequestContext ctx = IastRequestContext.get(span);
    if (ctx == null) {
      return;
    }
    TaintedObject taintedObject = ctx.getTaintedObjects().get(queryString);
    if (taintedObject == null) {
      return;
    }

    if (!overheadController.consumeQuota(Operations.REPORT_VULNERABILITY, span)) {
      return;
    }

    StackTraceElement stackTraceElement =
        stackWalker.walk(IastModuleImpl::findValidPackageForVulnerability);

    Vulnerability vulnerability =
        new Vulnerability(
            VulnerabilityType.SQL_INJECTION,
            Location.forStack(stackTraceElement),
            new Evidence(queryString, taintedObject.getRanges()));
    reporter.report(span, vulnerability);
  }

  private static StackTraceElement findValidPackageForVulnerability(
      Stream<StackTraceElement> stream) {
    final StackTraceElement[] first = new StackTraceElement[1];
    return stream
        .filter(
            stack -> {
              if (first[0] == null) {
                first[0] = stack;
              }
              return IastExclusionTrie.apply(stack.getClassName()) != 1;
            })
        .findFirst()
        .orElse(first[0]);
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
  public void onStringTrim(@Nullable final String self, @Nullable final String result) {
    // checks
    if (!canBeTainted(result)) {
      return;
    }
    if (!canBeTainted(self)) {
      return;
    }
    if (self.equals(result)) {
      return;
    }
    final IastRequestContext ctx = IastRequestContext.get();
    if (ctx == null) {
      return;
    }
    final TaintedObjects taintedObjects = ctx.getTaintedObjects();
    final TaintedObject taintedSelf = taintedObjects.get(self);
    if (taintedSelf == null) {
      return;
    }

    int offset = 0;
    while ((offset < self.length()) && (self.charAt(offset) <= ' ')) {
      offset++;
    }

    int resultLength = result.length();

    final Range[] rangesSelf = getRanges(taintedObjects, self);
    if (rangesSelf.length == 0) {
      return;
    }

    // Range adjusting
    int skippedRanges = 0;
    Range[] newRanges = new Range[rangesSelf.length];
    for (int rangeIndex = 0; rangeIndex < rangesSelf.length; rangeIndex++) {
      final Range rangeSelf = rangesSelf[rangeIndex];

      final int newEnd = rangeSelf.getStart() + rangeSelf.getLength() - offset;
      int newStart = rangeSelf.getStart() - offset;
      int newLength = rangeSelf.getLength();
      if (newStart < 0) {
        newLength = newLength + newStart;
        newStart = 0;
      }
      if (newEnd > resultLength) {
        newLength = resultLength - newStart;
      }
      if (newLength > 0) {
        newRanges[rangeIndex] = new Range(newStart, newLength, rangeSelf.getSource());
      } else {
        skippedRanges++;
      }
    }

    if (0 != skippedRanges) {
      // copy results
      Range[] nonSkippedRanges = new Range[rangesSelf.length - skippedRanges];
      int newRangeIndex = 0;
      for (int rangeIndex = 0; rangeIndex < newRanges.length; rangeIndex++) {
        if (null != newRanges[rangeIndex]) {
          nonSkippedRanges[newRangeIndex] = newRanges[rangeIndex];
          newRangeIndex++;
        }
      }
      newRanges = nonSkippedRanges;
    }
    taintedObjects.taint(result, newRanges);
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
    final Range[] rangesLeft = getRanges(taintedObjects, left);
    final Range[] rangesRight = getRanges(taintedObjects, right);
    if (rangesLeft.length == 0 && rangesRight.length == 0) {
      return;
    }
    final Range[] ranges =
        mergeRanges(left == null ? NULL_STR_LENGTH : left.length(), rangesLeft, rangesRight);
    taintedObjects.taint(result, ranges);
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
    final Range[] rangesRight = paramTainted.getRanges();
    final Range[] rangesLeft = getRanges(taintedObjects, builder);
    if (rangesLeft.length == 0 && rangesRight.length == 0) {
      return;
    }
    final Range[] ranges = mergeRanges(builder.length() - param.length(), rangesLeft, rangesRight);
    taintedObjects.taint(builder, ranges);
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

  private static Range[] getRanges(
      @Nonnull final TaintedObjects taintedObjects, @Nullable final Object target) {
    if (target == null) {
      return Ranges.EMPTY;
    }
    return getRanges(taintedObjects.get(target));
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
}
