package datadog.trace.agent.tooling.bytebuddy.matcher;

import static datadog.trace.bootstrap.AgentClassLoading.PROBING_CLASSLOADER;

import datadog.trace.agent.tooling.Utils;
import datadog.trace.agent.tooling.WeakCaches;
import datadog.trace.api.Config;
import datadog.trace.api.Tracer;
import datadog.trace.api.function.Function;
import datadog.trace.bootstrap.PatchLogger;
import datadog.trace.bootstrap.WeakCache;
import datadog.trace.util.Strings;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassLoaderMatchers {
  private static final Logger log = LoggerFactory.getLogger(ClassLoaderMatchers.class);

  private static final ClassLoader BOOTSTRAP_CLASSLOADER = null;

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
      case "datadog.trace.bootstrap.DatadogClassLoader":
        return true;
    }
    if (HAS_CLASSLOADER_EXCLUDES) {
      return Config.get().getExcludedClassLoaders().contains(classLoaderName);
    }
    return false;
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

  /** Mapping of class-resource to its has-class matcher. */
  static final Map<String, ElementMatcher.Junction<ClassLoader>> hasClassMatchers =
      new LinkedHashMap<>();

  /** Cache of classloader-instance -> has-class mask. */
  static final WeakCache<ClassLoader, BitSet> hasClassCache = WeakCaches.newWeakCache();

  /** Function that generates a has-class mask for a given class-loader. */
  static final Function<ClassLoader, BitSet> buildHasClassMask =
      new Function<ClassLoader, BitSet>() {
        @Override
        public BitSet apply(ClassLoader input) {
          return buildHasClassMask(input);
        }
      };

  /**
   * @param className the class name to match.
   * @return true if class is available as a resource.
   */
  public static ElementMatcher.Junction<ClassLoader> hasClassNamed(String className) {
    String resourceName = Strings.getResourceName(className);
    ElementMatcher.Junction<ClassLoader> matcher = hasClassMatchers.get(resourceName);
    if (null == matcher) {
      hasClassMatchers.put(resourceName, matcher = new HasClassMatcher(hasClassMatchers.size()));
    }
    return matcher;
  }

  static BitSet buildHasClassMask(ClassLoader cl) {
    PROBING_CLASSLOADER.begin();
    try {
      BitSet hasClassMask = new BitSet();
      int hasClassId = 0;
      for (String resourceName : hasClassMatchers.keySet()) {
        try {
          if (cl.getResource(resourceName) != null) {
            hasClassMask.set(hasClassId);
          }
        } catch (final Throwable ignored) {
        }
        hasClassId++;
      }
      return hasClassMask;
    } finally {
      PROBING_CLASSLOADER.end();
    }
  }

  static final class HasClassMatcher extends ElementMatcher.Junction.AbstractBase<ClassLoader> {
    private final int hasClassId;

    private HasClassMatcher(int hasClassId) {
      this.hasClassId = hasClassId;
    }

    @Override
    public boolean matches(final ClassLoader cl) {
      return hasClassCache
          .computeIfAbsent(
              BOOTSTRAP_CLASSLOADER == cl ? Utils.getBootstrapProxy() : cl, buildHasClassMask)
          .get(hasClassId);
    }
  }
}
