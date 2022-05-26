package datadog.trace.agent.tooling.bytebuddy.matcher.memoize;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.api.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizingHierarchyMatchers implements HierarchyMatchers.Supplier {

  // private final Map<ElementMatcher<ClassLoader>, BitSet> classLoaderMasks = new HashMap<>();

  static final ConcurrentMap<String, MemoizingMatchers.Matches> memoizedMatches =
      new ConcurrentHashMap<>();

  private final MemoizingMatchers<TypeDescription> typeMatchers =
      new MemoizingMatchers<>(
          new MemoizingMatchers.Exchange<TypeDescription>() {
            @Override
            public MemoizingMatchers.Matches get(TypeDescription target) {
              return memoizedMatches.get(target.getName());
            }

            @Override
            public void set(TypeDescription target, MemoizingMatchers.Matches matches) {
              memoizedMatches.putIfAbsent(target.getName(), matches);
            }
          });

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresAnnotation(
      ElementMatcher<? super NamedElement> matcher) {
    return none();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresField(
      ElementMatcher<? super FieldDescription> matcher) {
    return typeMatchers.memoize(
        new Function<TypeDescription, FieldList<? extends FieldDescription>>() {
          @Override
          public FieldList<? extends FieldDescription> apply(TypeDescription input) {
            return input.getDeclaredFields();
          }
        },
        whereAny(matcher));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresMethod(
      ElementMatcher<? super MethodDescription> matcher) {
    return null;
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> extendsClass(
      ElementMatcher<? super TypeDescription> matcher) {
    return none();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> implementsInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return none();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return none();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasSuperType(
      ElementMatcher<? super TypeDescription> matcher) {
    return none();
  }

  @Override
  public ElementMatcher.Junction<MethodDescription> hasSuperMethod(
      ElementMatcher<? super MethodDescription> matcher) {
    return none();
  }
}
