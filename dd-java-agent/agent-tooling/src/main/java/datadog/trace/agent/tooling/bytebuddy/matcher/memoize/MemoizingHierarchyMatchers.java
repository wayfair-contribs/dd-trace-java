package datadog.trace.agent.tooling.bytebuddy.matcher.memoize;

import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.api.function.Function;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
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
    return typeMatchers.memoize(
        new Function<TypeDescription, TypeList>() {
          @Override
          public TypeList apply(TypeDescription input) {
            return input.getDeclaredAnnotations().asTypeList();
          }
        },
        whereAny(matcher));
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
    return typeMatchers.memoize(
        new Function<TypeDescription, MethodList<? extends MethodDescription>>() {
          @Override
          public MethodList<? extends MethodDescription> apply(TypeDescription input) {
            return input.getDeclaredMethods();
          }
        },
        whereAny(matcher));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> extendsClass(
      ElementMatcher<? super TypeDescription> matcher) {
    return new SafeHierarchyMatcher(
        typeMatchers.memoize(
            new Function<TypeDescription, List<TypeDescription>>() {
              @Override
              public List<TypeDescription> apply(TypeDescription input) {
                return extractHierarchyOneLevel(input, true, false);
              }
            },
            whereAny(matcher)),
        false,
        false);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> implementsInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return new SafeHierarchyMatcher(
        typeMatchers.memoize(
            new Function<TypeDescription, List<TypeDescription>>() {
              @Override
              public List<TypeDescription> apply(TypeDescription input) {
                return extractHierarchyOneLevel(input, false, true);
              }
            },
            whereAny(matcher)),
        false,
        true);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return new SafeHierarchyMatcher(
        typeMatchers.memoize(
            new Function<TypeDescription, List<TypeDescription>>() {
              @Override
              public List<TypeDescription> apply(TypeDescription input) {
                return extractHierarchyOneLevel(input, false, true);
              }
            },
            whereAny(matcher)),
        true,
        true);
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasSuperType(
      ElementMatcher<? super TypeDescription> matcher) {
    return new SafeHierarchyMatcher(
        typeMatchers.memoize(
            new Function<TypeDescription, List<TypeDescription>>() {
              @Override
              public List<TypeDescription> apply(TypeDescription input) {
                return extractHierarchyOneLevel(input, true, true);
              }
            },
            whereAny(matcher)),
        true,
        true);
  }

  @Override
  public ElementMatcher.Junction<MethodDescription> hasSuperMethod(
      ElementMatcher<? super MethodDescription> matcher) {
    return new SafeSuperMethodMatcher(
        typeMatchers.memoize(
            new Function<TypeDescription, MethodList<? extends MethodDescription>>() {
              @Override
              public MethodList<? extends MethodDescription> apply(TypeDescription input) {
                return input.getDeclaredMethods();
              }
            },
            whereAny(matcher)));
  }

  static final class SafeHierarchyMatcher
      extends ElementMatcher.Junction.ForNonNullValues<TypeDescription> {

    private final ElementMatcher<TypeDescription> oneLevelMatcher;
    private final boolean allowInterfaceTarget;
    private final boolean visitInterfaces;

    SafeHierarchyMatcher(
        ElementMatcher<TypeDescription> oneLevelMatcher,
        boolean allowInterfaceTarget,
        boolean visitInterfaces) {
      this.oneLevelMatcher = oneLevelMatcher;
      this.allowInterfaceTarget = allowInterfaceTarget;
      this.visitInterfaces = visitInterfaces;
    }

    @Override
    protected boolean doMatch(TypeDescription target) {
      return false;
    }
  }

  static final class SafeSuperMethodMatcher
      extends ElementMatcher.Junction.ForNonNullValues<MethodDescription> {

    private final ElementMatcher<TypeDescription> oneLevelMatcher;

    SafeSuperMethodMatcher(ElementMatcher<TypeDescription> oneLevelMatcher) {
      this.oneLevelMatcher = oneLevelMatcher;
    }

    @Override
    protected boolean doMatch(MethodDescription target) {
      return false;
    }
  }

  static List<TypeDescription> extractHierarchyOneLevel(
      TypeDescription target, boolean includeClasses, boolean includeInterfaces) {
    List<TypeDescription> types = new ArrayList<>();
    if (target.isInterface() ? includeInterfaces : includeClasses) {
      types.add(target);
    }
    if (includeClasses) {
      types.add(safeSuperClass(target));
    }
    if (includeInterfaces) {
      types.addAll(safeInterfaces(target));
    }
    return types;
  }

  static TypeDescription safeSuperClass(TypeDescription typeDescription) {
    return safeErasure(typeDescription.getSuperClass());
  }

  static List<TypeDescription> safeInterfaces(TypeDescription typeDescription) {
    List<TypeDescription> interfaces = new ArrayList<>();
    Iterator<TypeDescription.Generic> itr = typeDescription.getInterfaces().iterator();
    while (itr.hasNext()) {
      interfaces.add(safeErasure(itr.next()));
    }
    return interfaces;
  }

  static TypeDescription safeErasure(TypeDescription.Generic generic) {
    return null != generic ? generic.asErasure() : null;
  }
}
