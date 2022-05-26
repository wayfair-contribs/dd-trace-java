package datadog.trace.agent.tooling.bytebuddy.matcher.memoize;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.api.function.Function;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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

  static final ConcurrentMap<String, MemoizingMatchers.Matches> memoizedTypeMatches =
      new ConcurrentHashMap<>();

  private final MemoizingMatchers<TypeDescription> typeMatchers =
      new MemoizingMatchers<>(
          new MemoizingMatchers.Exchange<TypeDescription>() {
            @Override
            public MemoizingMatchers.Matches get(TypeDescription target) {
              return memoizedTypeMatches.get(target.getName());
            }

            @Override
            public void set(TypeDescription target, MemoizingMatchers.Matches matches) {
              memoizedTypeMatches.putIfAbsent(target.getName(), matches);
            }
          });

  static final ConcurrentMap<String, MemoizingMatchers.Matches> memoizedHierarchyMatches =
      new ConcurrentHashMap<>();

  private final MemoizingMatchers<TypeDescription> hierarchyMatchers =
      new MemoizingMatchers<>(
          new MemoizingMatchers.Exchange<TypeDescription>() {
            @Override
            public MemoizingMatchers.Matches get(TypeDescription target) {
              return memoizedHierarchyMatches.get(target.getName());
            }

            @Override
            public void set(TypeDescription target, MemoizingMatchers.Matches matches) {
              memoizedHierarchyMatches.putIfAbsent(target.getName(), matches);
            }
          });

  static final Function<TypeDescription, TypeList> extractAnnotations =
      new Function<TypeDescription, TypeList>() {
        @Override
        public TypeList apply(TypeDescription input) {
          return input.getDeclaredAnnotations().asTypeList();
        }
      };

  static final Function<TypeDescription, FieldList<? extends FieldDescription>> extractFields =
      new Function<TypeDescription, FieldList<? extends FieldDescription>>() {
        @Override
        public FieldList<? extends FieldDescription> apply(TypeDescription input) {
          return input.getDeclaredFields();
        }
      };

  static final Function<TypeDescription, MethodList<? extends MethodDescription>> extractMethods =
      new Function<TypeDescription, MethodList<? extends MethodDescription>>() {
        @Override
        public MethodList<? extends MethodDescription> apply(TypeDescription input) {
          return input.getDeclaredMethods();
        }
      };

  static final Function<TypeDescription, Iterable<TypeDescription>> extractSuperClasses =
      new Function<TypeDescription, Iterable<TypeDescription>>() {
        @Override
        public Iterable<TypeDescription> apply(TypeDescription input) {
          return new SafeSuperClassIterable(input);
        }
      };

  static final Function<TypeDescription, Iterable<TypeDescription>> extractSuperTypes =
      new Function<TypeDescription, Iterable<TypeDescription>>() {
        @Override
        public Iterable<TypeDescription> apply(TypeDescription input) {
          return new SafeSuperTypeIterable(input);
        }
      };

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresAnnotation(
      ElementMatcher<? super NamedElement> matcher) {
    return typeMatchers.memoize(extractAnnotations, whereAny(matcher));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresField(
      ElementMatcher<? super FieldDescription> matcher) {
    return typeMatchers.memoize(extractFields, whereAny(matcher));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> declaresMethod(
      ElementMatcher<? super MethodDescription> matcher) {
    return typeMatchers.memoize(extractMethods, whereAny(matcher));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> extendsClass(
      ElementMatcher<? super TypeDescription> matcher) {
    return not(isInterface())
        .and(hierarchyMatchers.memoize(extractSuperClasses, whereAny(matcher)));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> implementsInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return not(isInterface())
        .and(hierarchyMatchers.memoize(extractSuperTypes, whereAny(isInterface().and(matcher))));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasInterface(
      ElementMatcher<? super TypeDescription> matcher) {
    return hierarchyMatchers.memoize(extractSuperTypes, whereAny(isInterface().and(matcher)));
  }

  @Override
  public ElementMatcher.Junction<TypeDescription> hasSuperType(
      ElementMatcher<? super TypeDescription> matcher) {
    return not(isInterface()).and(hierarchyMatchers.memoize(extractSuperTypes, whereAny(matcher)));
  }

  @Override
  public ElementMatcher.Junction<MethodDescription> hasSuperMethod(
      ElementMatcher<? super MethodDescription> matcher) {
    return new SafeSuperMethodMatcher(declaresMethod(matcher));
  }

  static final class SafeSuperClassIterable implements Iterable<TypeDescription> {
    final TypeDescription typeDescription;

    SafeSuperClassIterable(TypeDescription typeDescription) {
      this.typeDescription = typeDescription;
    }

    @Override
    public Iterator<TypeDescription> iterator() {
      return new Iterator<TypeDescription>() {
        TypeDescription next = typeDescription;

        @Override
        public boolean hasNext() {
          return null != next;
        }

        @Override
        public TypeDescription next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          TypeDescription result = next;
          next = safeSuperClass(next);
          return result;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  static final class SafeSuperTypeIterable implements Iterable<TypeDescription> {
    final TypeDescription typeDescription;

    SafeSuperTypeIterable(TypeDescription typeDescription) {
      this.typeDescription = typeDescription;
    }

    @Override
    public Iterator<TypeDescription> iterator() {
      return new Iterator<TypeDescription>() {
        Deque<TypeDescription> hierarchy = new ArrayDeque<>();

        TypeDescription next = typeDescription;

        @Override
        public boolean hasNext() {
          return null != next;
        }

        @Override
        public TypeDescription next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          TypeDescription result = next;
          if (!result.isInterface()) {
            TypeDescription superClass = safeSuperClass(result);
            if (null != superClass) {
              hierarchy.add(superClass);
            }
          }
          hierarchy.addAll(safeInterfaces(result));
          next = hierarchy.poll();
          return result;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
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
