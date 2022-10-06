package datadog.trace.agent.tooling.bytebuddy.memoize;

import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.ANNOTATION;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.CLASS;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.FIELD;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.INTERFACE;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.METHOD;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.TYPE;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.isClass;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class MemoizedMatchers implements HierarchyMatchers.Supplier {
  public static void registerAsSupplier() {
    HierarchyMatchers.registerIfAbsent(new MemoizedMatchers());
    Memoizer.reset();
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresAnnotation(
      ElementMatcher.Junction<? super NamedElement> matcher) {
    return Memoizer.prepare(ANNOTATION, matcher, false);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresField(
      ElementMatcher.Junction<? super FieldDescription> matcher) {
    return Memoizer.prepare(FIELD, matcher, false);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresMethod(
      ElementMatcher.Junction<? super MethodDescription> matcher) {
    return Memoizer.prepare(METHOD, matcher, false);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> extendsClass(
      ElementMatcher.Junction<? super TypeDescription> matcher) {
    return isClass.and(Memoizer.prepare(CLASS, matcher, true));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> implementsInterface(
      ElementMatcher.Junction<? super TypeDescription> matcher) {
    return isClass.and(Memoizer.prepare(INTERFACE, matcher, true));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasInterface(
      ElementMatcher.Junction<? super TypeDescription> matcher) {
    return Memoizer.prepare(INTERFACE, matcher, true);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasSuperType(
      ElementMatcher.Junction<? super TypeDescription> matcher) {
    return isClass.and(Memoizer.prepare(TYPE, matcher, true));
  }

  @Override
  public ElementMatcher.Junction<MethodDescription> hasSuperMethod(
      ElementMatcher.Junction<? super MethodDescription> matcher) {
    return null; // FIXME
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresContextField(
      String keyClassName, String contextClassName) {
    return null; // FIXME
  }
}
