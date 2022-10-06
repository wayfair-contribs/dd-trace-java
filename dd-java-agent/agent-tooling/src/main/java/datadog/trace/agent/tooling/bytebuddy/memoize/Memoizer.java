package datadog.trace.agent.tooling.bytebuddy.memoize;

import static net.bytebuddy.matcher.ElementMatchers.any;

import datadog.trace.agent.tooling.bytebuddy.TypeInfoCache;
import datadog.trace.api.Config;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

final class Memoizer {

  enum MatcherKind {
    ANNOTATION,
    FIELD,
    METHOD,
    CLASS,
    INTERFACE,
    TYPE // i.e. class or interface
  }

  private static final BitSet NO_MATCHES = new BitSet(0);

  private static final int SIZE_HINT = 320; // estimated number of matchers

  private static final BitSet annotationMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet fieldMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet methodMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet classMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet interfaceMatcherIds = new BitSet(SIZE_HINT);
  private static final BitSet inheritedMatcherIds = new BitSet(SIZE_HINT);

  private static final List<ElementMatcher> matchers = new ArrayList<>();

  private static final TypeInfoCache<BitSet> memos =
      new TypeInfoCache<>(Config.get().getResolverMemoPoolSize());

  private static final ThreadLocal<Map<String, BitSet>> localMemos =
      new ThreadLocal<Map<String, BitSet>>() {
        @Override
        protected Map<String, BitSet> initialValue() {
          return new HashMap<>();
        }
      };

  public static final ElementMatcher.Junction<TypeDescription> isClass =
      prepare(MatcherKind.CLASS, any(), false);

  public static void reset() {
    if (matchers.size() > 1) {
      memos.clear();
    }
  }

  public static <T> ElementMatcher.Junction<TypeDescription> prepare(
      MatcherKind kind, ElementMatcher.Junction<T> matcher, boolean inherited) {
    int matcherId = matchers.size();

    matchers.add(matcher);

    switch (kind) {
      case ANNOTATION:
        annotationMatcherIds.set(matcherId);
        break;
      case FIELD:
        fieldMatcherIds.set(matcherId);
        break;
      case METHOD:
        methodMatcherIds.set(matcherId);
        break;
      case CLASS:
        classMatcherIds.set(matcherId);
        break;
      case INTERFACE:
        interfaceMatcherIds.set(matcherId);
        break;
      case TYPE:
        classMatcherIds.set(matcherId);
        interfaceMatcherIds.set(matcherId);
        break;
    }

    if (inherited) {
      inheritedMatcherIds.set(matcherId);
    }

    return new MemoizingMatcher(matcherId);
  }

  static final class MemoizingMatcher
      extends ElementMatcher.Junction.ForNonNullValues<TypeDescription> {
    private final int matcherId;

    public MemoizingMatcher(int matcherId) {
      this.matcherId = matcherId;
    }

    @Override
    protected boolean doMatch(TypeDescription target) {
      return memoize(target).get(matcherId);
    }
  }

  private static BitSet memoize(TypeDescription target) {
    TypeInfoCache.SharedTypeInfo<BitSet> existingMemo = memos.find(target.getName());
    if (null != existingMemo) {
      return existingMemo.get();
    }
    return doMemoize(target, localMemos.get());
  }

  private static BitSet memoize(TypeDefinition superTarget, Map<String, BitSet> localMemos) {
    TypeInfoCache.SharedTypeInfo<BitSet> existingMemo = memos.find(superTarget.getTypeName());
    if (null != existingMemo) {
      return existingMemo.get();
    }
    return doMemoize(superTarget.asErasure(), localMemos);
  }

  private static BitSet doMemoize(TypeDescription target, Map<String, BitSet> localMemos) {
    BitSet memo = localMemos.get(target.getName());
    if (null != memo) {
      return memo;
    }

    localMemos.put(target.getName(), memo = new BitSet(matchers.size()));

    TypeDescription.Generic superTarget = target.getSuperClass();
    if (null != superTarget && !"java.lang.Object".equals(superTarget.getTypeName())) {
      inherit(memoize(superTarget, localMemos), memo);
    }

    for (TypeDescription.Generic intf : target.getInterfaces()) {
      inherit(memoize(intf, localMemos), memo);
    }

    for (AnnotationDescription ann : target.getDeclaredAnnotations()) {
      record(annotationMatcherIds, ann.getAnnotationType(), memo);
    }
    for (FieldDescription field : target.getDeclaredFields()) {
      record(fieldMatcherIds, field, memo);
    }
    for (MethodDescription method : target.getDeclaredMethods()) {
      record(methodMatcherIds, method, memo);
    }

    record(target.isInterface() ? interfaceMatcherIds : classMatcherIds, target, memo);

    if (memo.isEmpty()) {
      memo = NO_MATCHES;
    }

    memos.share(target.getName(), null, null, memo);

    localMemos.remove(target.getName());

    return memo;
  }

  private static void inherit(BitSet superMemo, BitSet memo) {
    int matcherId = superMemo.nextSetBit(0);
    while (matcherId >= 0) {
      if (inheritedMatcherIds.get(matcherId)) {
        memo.set(matcherId);
      }
      matcherId = superMemo.nextSetBit(matcherId + 1);
    }
  }

  @SuppressWarnings("unchecked")
  private static void record(BitSet matcherIds, Object target, BitSet memo) {
    int matcherId = matcherIds.nextSetBit(0);
    while (matcherId >= 0) {
      if (!memo.get(matcherId) && matchers.get(matcherId).matches(target)) {
        memo.set(matcherId);
      }
      matcherId = matcherIds.nextSetBit(matcherId + 1);
    }
  }
}
