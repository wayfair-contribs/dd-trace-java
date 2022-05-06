package datadog.trace.agent.tooling.bytebuddy.matcher;

import static datadog.trace.util.AgentThreadFactory.AGENT_THREAD_GROUP;

import datadog.trace.api.Config;
import datadog.trace.util.ClassNameTrie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persistent excludes cache, incrementally updated over the life of the application. */
public class CachedExcludes {
  private static final Logger log = LoggerFactory.getLogger(CachedExcludes.class);

  private CachedExcludes() {}

  private static final ClassNameTrie.Builder excludes;

  static {
    String excludedClassesCache = Config.get().getExcludedClassesCache();
    if (null != excludedClassesCache) {
      excludes = new ClassNameTrie.Builder();
      try {
        Path cachePath = Paths.get(excludedClassesCache);
        if (Files.isReadable(cachePath)) {
          excludes.readFrom(cachePath);
        }
        Files.createDirectories(cachePath.toAbsolutePath().getParent());
        Runtime.getRuntime().addShutdownHook(new PersistHook(cachePath));
      } catch (IllegalStateException ex) {
        // cannot add shutdown hook as JVM is shutting down
      } catch (Exception e) {
        log.warn("Unable to cache class excludes in {}", excludedClassesCache, e);
      }
    } else {
      excludes = null;
    }
  }

  public static boolean isEnabled() {
    return excludes != null;
  }

  public static boolean isExcluded(String name) {
    return excludes != null && excludes.apply(name) > 0;
  }

  public static void exclude(String name) {
    excludes.put(name, 1);
  }

  private static class PersistHook extends Thread {
    private final Path cachePath;

    private PersistHook(Path cachePath) {
      super(AGENT_THREAD_GROUP, "dd-tracer-excludes-cache");
      this.cachePath = cachePath;
    }

    @Override
    public void run() {
      excludes.writeTo(cachePath);
    }
  }
}
