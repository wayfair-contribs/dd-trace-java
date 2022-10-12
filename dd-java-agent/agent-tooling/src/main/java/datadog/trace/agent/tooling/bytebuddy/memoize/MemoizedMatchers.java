package datadog.trace.agent.tooling.bytebuddy.memoize;

import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.ANNOTATION;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.CLASS;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.FIELD;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.INTERFACE;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.METHOD;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.MatcherKind.TYPE;
import static datadog.trace.agent.tooling.bytebuddy.memoize.Memoizer.isClass;
import static net.bytebuddy.matcher.ElementMatchers.hasSignature;

import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.agent.tooling.context.ShouldInjectFieldsMatcher;
import java.util.HashSet;
import java.util.Set;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizedMatchers implements HierarchyMatchers.Supplier {
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
    return new HasSuperMethod(Memoizer.prepare(METHOD, matcher, true), matcher);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresContextField(
      String keyClassName, String contextClassName) {
    return null; // FIXME
  }

  static final class HasSuperMethod
      extends ElementMatcher.Junction.ForNonNullValues<MethodDescription> {

    private final ElementMatcher<TypeDescription> typeMatcher;
    private final ElementMatcher<? super MethodDescription> methodMatcher;

    HasSuperMethod(
        ElementMatcher<TypeDescription> typeMatcher,
        ElementMatcher<? super MethodDescription> methodMatcher) {
      this.typeMatcher = typeMatcher;
      this.methodMatcher = methodMatcher;
    }

    @Override
    protected boolean doMatch(MethodDescription target) {
      if (target.isConstructor()) {
        return false;
      }

      TypeDefinition type = target.getDeclaringType();
      if (!typeMatcher.matches(type.asErasure())) {
        return false;
      } else if (methodMatcher.matches(target)) {
        return true;
      }

      ElementMatcher<MethodDescription> signatureMatcher = hasSignature(target.asSignatureToken());
      Set<String> visited = new HashSet<>();

      if (interfaceMatches(type.getInterfaces(), signatureMatcher, visited)) {
        return true;
      }

      type = type.getSuperClass();
      while (null != type && typeMatcher.matches(type.asErasure())) {
        for (MethodDescription method : type.getDeclaredMethods()) {
          if (signatureMatcher.matches(method) && methodMatcher.matches(method)) {
            return true;
          }
        }
        if (interfaceMatches(type.getInterfaces(), signatureMatcher, visited)) {
          return true;
        }
        type = type.getSuperClass();
      }
      return false;
    }

    private boolean interfaceMatches(
        TypeList.Generic interfaces,
        ElementMatcher<MethodDescription> signatureMatcher,
        Set<String> visited) {
      for (TypeDefinition type : interfaces) {
        if (!visited.add(type.getTypeName()) || !typeMatcher.matches(type.asErasure())) {
          continue;
        }
        for (MethodDescription method : type.getDeclaredMethods()) {
          if (signatureMatcher.matches(method) && methodMatcher.matches(method)) {
            return true;
          }
        }
        if (interfaceMatches(type.getInterfaces(), signatureMatcher, visited)) {
          return true;
        }
      }
      return false;
    }
  }
}
