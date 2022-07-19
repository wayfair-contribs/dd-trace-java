package datadog.trace.instrumentation.hibernate;

import static datadog.trace.agent.tooling.bytebuddy.matcher.ClassLoaderMatchers.hasClassesNamed;

import net.bytebuddy.matcher.ElementMatcher;

public final class HibernateMatchers {

  // Optimization for expensive typeMatcher.
  public static final ElementMatcher<ClassLoader> HAS_HIBERNATE_CLASSES =
      hasClassesNamed("org.hibernate.Session");

  private HibernateMatchers() {}
}
