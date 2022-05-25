package datadog.trace.agent.tooling.bytebuddy.matcher;

import static datadog.trace.bootstrap.AgentClassLoading.PROBING_CLASSLOADER;
import static datadog.trace.util.Strings.getResourceName;
import static net.bytebuddy.matcher.ElementMatchers.any;

import datadog.trace.agent.tooling.WeakCaches;
import datadog.trace.agent.tooling.bytebuddy.matcher.memoizing.MemoizingMatchers;
import datadog.trace.agent.tooling.bytebuddy.matcher.memoizing.MergingResult;
import datadog.trace.api.Config;
import datadog.trace.api.Tracer;
import datadog.trace.api.function.Function;
import datadog.trace.bootstrap.PatchLogger;
import datadog.trace.bootstrap.WeakCache;
import java.util.Arrays;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassLoaderMatchers {
  private static final Logger log = LoggerFactory.getLogger(ClassLoaderMatchers.class);

  public static final ElementMatcher<ClassLoader> ANY_CLASS_LOADER = any();

  static final ClassLoader BOOTSTRAP_CLASSLOADER = null;

  private static final String DATADOG_CLASSLOADER_NAME =
      "datadog.trace.bootstrap.DatadogClassLoader";
  private static final String DATADOG_DELEGATE_CLASSLOADER_NAME =
      "datadog.trace.bootstrap.DatadogClassLoader$DelegateClassLoader";

  private static final boolean HAS_CLASSLOADER_EXCLUDES =
      !Config.get().getExcludedClassLoaders().isEmpty();

  /* Cache of classloader-instance -> (true|false). True = skip instrumentation. False = safe to instrument. */
  private static final WeakCache<ClassLoader, Boolean> skipCache = WeakCaches.newWeakCache();

  /** A private constructor that must not be invoked. */
  private ClassLoaderMatchers() {
    throw new UnsupportedOperationException();
  }

  public static boolean skipClassLoader(final ClassLoader loader) {
    if (loader == BOOTSTRAP_CLASSLOADER) {
      // Don't skip bootstrap loader
      return false;
    }
    if (canSkipClassLoaderByName(loader)) {
      return true;
    }
    Boolean v = skipCache.getIfPresent(loader);
    if (v != null) {
      return v;
    }
    // when ClassloadingInstrumentation is active, checking delegatesToBootstrap() below is not
    // required, because ClassloadingInstrumentation forces all class loaders to load all of the
    // classes in Constants.BOOTSTRAP_PACKAGE_PREFIXES directly from the bootstrap class loader
    //
    // however, at this time we don't want to introduce the concept of a required instrumentation,
    // and we don't want to introduce the concept of the tooling code depending on whether or not
    // a particular instrumentation is active (mainly because this particular use case doesn't
    // seem to justify introducing either of these new concepts)
    v = !delegatesToBootstrap(loader);
    skipCache.put(loader, v);
    return v;
  }

  public static boolean canSkipClassLoaderByName(final ClassLoader loader) {
    String classLoaderName = loader.getClass().getName();
    switch (classLoaderName) {
      case "org.codehaus.groovy.runtime.callsite.CallSiteClassLoader":
      case "sun.reflect.DelegatingClassLoader":
      case "jdk.internal.reflect.DelegatingClassLoader":
      case "clojure.lang.DynamicClassLoader":
      case "org.apache.cxf.common.util.ASMHelper$TypeHelperClassLoader":
      case "sun.misc.Launcher$ExtClassLoader":
      case DATADOG_CLASSLOADER_NAME:
      case DATADOG_DELEGATE_CLASSLOADER_NAME:
        return true;
    }
    if (HAS_CLASSLOADER_EXCLUDES) {
      return Config.get().getExcludedClassLoaders().contains(classLoaderName);
    }
    return false;
  }

  /**
   * NOTICE: Does not match the bootstrap classpath. Don't use with classes expected to be on the
   * bootstrap.
   *
   * @param classNames list of names to match. returns true if empty.
   * @return true if class is available as a resource and not the bootstrap classloader.
   */
  public static ElementMatcher.Junction<ClassLoader> hasClassesNamed(String... classNames) {
    return memoizingMatchers.memoize(new ClassLoaderHasClassesNamedMatcher(classNames));
  }

  /**
   * NOTICE: Does not match the bootstrap classpath. Don't use with classes expected to be on the
   * bootstrap.
   *
   * @param className the className to match.
   * @return true if class is available as a resource and not the bootstrap classloader.
   */
  public static ElementMatcher.Junction<ClassLoader> hasClassesNamed(String className) {
    return memoizingMatchers.memoize(new ClassLoaderHasClassNamedMatcher(className));
  }

  /**
   * TODO: this turns out to be useless with OSGi: {@code
   * org.eclipse.osgi.internal.loader.BundleLoader#isRequestFromVM} returns {@code true} when class
   * loading is issued from this check and {@code false} for 'real' class loads. We should come up
   * with some sort of hack to avoid this problem.
   */
  private static boolean delegatesToBootstrap(final ClassLoader loader) {
    boolean delegates = true;
    if (!loadsExpectedClass(loader, Tracer.class)) {
      log.debug("Loader {} failed to delegate to bootstrap dd-trace-api class", loader);
      delegates = false;
    }
    if (!loadsExpectedClass(loader, PatchLogger.class)) {
      log.debug("Loader {} failed to delegate to bootstrap agent-bootstrap class", loader);
      delegates = false;
    }
    return delegates;
  }

  private static boolean loadsExpectedClass(
      final ClassLoader loader, final Class<?> expectedClass) {
    try {
      return loader.loadClass(expectedClass.getName()) == expectedClass;
    } catch (final Throwable ignored) {
      return false;
    }
  }

  static final WeakCache<ClassLoader, MemoizingMatchers.Result> memoizedResults =
      WeakCaches.newWeakCache(32);

  static final Function<ClassLoader, MemoizingMatchers.Result> memoizeResult =
      new Function<ClassLoader, MemoizingMatchers.Result>() {
        @Override
        public MemoizingMatchers.Result apply(final ClassLoader input) {
          return new MergingResult<ClassLoader>() {
            @Override
            public void merge() {
              memoizedResults.put(input, merged());
            }
          };
        }
      };

  static final MemoizingMatchers<ClassLoader> memoizingMatchers =
      new MemoizingMatchers<>(
          new Function<ClassLoader, MemoizingMatchers.Result>() {
            @Override
            public MemoizingMatchers.Result apply(ClassLoader input) {
              if (BOOTSTRAP_CLASSLOADER == input) {
                return MemoizingMatchers.NO_MATCHES;
              }
              return memoizedResults.computeIfAbsent(input, memoizeResult);
            }
          });

  private static class ClassLoaderHasClassesNamedMatcher
      extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

    private final String[] resources;

    private ClassLoaderHasClassesNamedMatcher(final String... classNames) {
      resources = classNames;
      for (int i = 0; i < resources.length; i++) {
        resources[i] = getResourceName(resources[i]);
      }
    }

    public boolean matches(final ClassLoader cl) {
      PROBING_CLASSLOADER.begin();
      try {
        for (final String resource : resources) {
          if (cl.getResource(resource) == null) {
            return false;
          }
        }
        return true;
      } catch (final Throwable ignored) {
        return false;
      } finally {
        PROBING_CLASSLOADER.end();
      }
    }

    @Override
    public String toString() {
      return "ClassLoaderHasClassesNamedMatcher{named=" + Arrays.toString(resources) + "}";
    }
  }

  private static class ClassLoaderHasClassNamedMatcher
      extends ElementMatcher.Junction.AbstractBase<ClassLoader> {

    private final String resource;

    private ClassLoaderHasClassNamedMatcher(final String className) {
      resource = getResourceName(className);
    }

    public boolean matches(final ClassLoader cl) {
      PROBING_CLASSLOADER.begin();
      try {
        return cl.getResource(resource) != null;
      } catch (final Throwable ignored) {
        return false;
      } finally {
        PROBING_CLASSLOADER.end();
      }
    }

    @Override
    public String toString() {
      return "ClassLoaderHasClassNamedMatcher{named='" + resource + "\'}";
    }
  }
}
