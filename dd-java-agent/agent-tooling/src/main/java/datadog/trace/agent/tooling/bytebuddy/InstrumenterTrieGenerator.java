package datadog.trace.agent.tooling.bytebuddy;

import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.Instrumenters;
import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.util.ClassNameTrie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Generates a {@link ClassNameTrie} implementation of {@link InstrumenterTrie} that indexes all
 * {@code META-INF/services/datadog.trace.agent.tooling.Instrumenter} services registered with the
 * 'instrumentation' module of the Java Agent.
 */
public final class InstrumenterTrieGenerator {
  private final ClassNameTrie.Builder builder = new ClassNameTrie.Builder();

  // marks results that match multiple instrumentation-ids
  private static final int MULTI_MATCH_MARKER = 0x1000;

  // lookup table that tracks multiple match results
  private final List<BitSet> multiMatches = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      throw new IllegalArgumentException("Expected: java-dir");
    }
    Path javaDir = Paths.get(args[0]).toAbsolutePath().normalize();
    if (!Files.isDirectory(javaDir)) {
      throw new IllegalArgumentException("Bad java directory: " + javaDir);
    }

    // satisfy some instrumenters that cache matchers in initializers
    HierarchyMatchers.registerIfAbsent(HierarchyMatchers.simpleChecks());
    SharedTypePools.registerIfAbsent(SharedTypePools.simpleCache());

    InstrumenterTrieGenerator generator = new InstrumenterTrieGenerator();

    for (Instrumenter instrumenter : Instrumenters.load(Instrumenter.class.getClassLoader())) {
      int instrumentationId = Instrumenters.currentInstrumentationId();
      if (instrumenter instanceof Instrumenter.ForSingleType) {
        String name = ((Instrumenter.ForSingleType) instrumenter).instrumentedType();
        generator.index(instrumenter, name, instrumentationId);
      } else if (instrumenter instanceof Instrumenter.ForKnownTypes) {
        for (String name : ((Instrumenter.ForKnownTypes) instrumenter).knownMatchingTypes()) {
          generator.index(instrumenter, name, instrumentationId);
        }
      }
    }

    generator.generateJavaFile(javaDir);
  }

  /** Indexes a single match from class-name to instrumentation-id. */
  private void index(Instrumenter instrumenter, String name, int instrumentationId) {
    if (null == name || name.isEmpty()) {
      throw new IllegalArgumentException(
          instrumenter.getClass() + " declares a null or empty class name");
    }
    int existingId = builder.apply(name);
    if (existingId < 0) {
      builder.put(name, instrumentationId);
    } else {
      BitSet instrumentationIds;
      if ((existingId & MULTI_MATCH_MARKER) != 0) {
        // add new instrumentation-id to existing multi-match, no need to update trie
        int multipleMatchId = existingId & ~MULTI_MATCH_MARKER;
        instrumentationIds = multiMatches.get(multipleMatchId);
      } else {
        // create multi-match covering old and new instrumentation-ids and update trie
        int multipleMatchId = multiMatches.size();
        builder.put(name, multipleMatchId | MULTI_MATCH_MARKER);
        instrumentationIds = new BitSet();
        multiMatches.add(instrumentationIds);
        instrumentationIds.set(existingId);
      }
      instrumentationIds.set(instrumentationId);
    }
  }

  /** Writes the Java form of the complete {@link InstrumenterTrie} to the file-system. */
  private void generateJavaFile(Path javaDir) throws IOException {
    String pkgName = InstrumenterTrie.class.getPackage().getName();
    String className = InstrumenterTrie.class.getSimpleName();
    Path javaPath = javaDir.resolve(pkgName.replace('.', '/')).resolve(className + ".java");
    List<String> lines = new ArrayList<>();
    lines.add("package " + pkgName + ";");
    lines.add("");
    lines.add("import datadog.trace.util.ClassNameTrie;");
    lines.add("import java.util.BitSet;");
    lines.add("");
    lines.add(
        "// Generated from 'META-INF/services/datadog.trace.agent.tooling.Instrumenter' - DO NOT EDIT!");
    lines.add("public final class " + className + " {");
    lines.add("");
    boolean hasLongJumps = ClassNameTrie.JavaGenerator.generateJavaTrie(lines, "", builder);
    lines.add("");
    // pack multi-matches into simple two-dimensional array
    lines.add("  private static int[][] MULTI_MATCHES = {");
    StringBuilder buf = new StringBuilder();
    buf.append("    ");
    for (BitSet bits : multiMatches) {
      if (buf.length() > 90) {
        lines.add(buf.toString());
        buf.setLength(0);
        buf.append("    ");
      }
      buf.append("{");
      int id = bits.nextSetBit(0);
      while (id >= 0) {
        buf.append(id);
        id = bits.nextSetBit(id + 1);
        if (id >= 0) {
          buf.append(", ");
        }
      }
      buf.append("}, ");
    }
    lines.add(buf.toString());
    lines.add("  };");
    lines.add("");
    lines.add("  public static final boolean ENABLED = true;");
    lines.add("");
    lines.add("  public static void apply(String key, BitSet instrumentationIds) {");
    if (hasLongJumps) {
      lines.add("    int match = ClassNameTrie.apply(TRIE_DATA, LONG_JUMPS, key);");
    } else {
      lines.add("    int match = ClassNameTrie.apply(TRIE_DATA, null, key);");
    }
    // apply any single or multi-match results to the given bit-set
    lines.add("    if (match >= " + MULTI_MATCH_MARKER + ") {");
    lines.add("      for (int id : MULTI_MATCHES[match - " + MULTI_MATCH_MARKER + "]) {");
    lines.add("        instrumentationIds.set(id);");
    lines.add("      }");
    lines.add("    } else if (match >= 0) {");
    lines.add("      instrumentationIds.set(match);");
    lines.add("    }");
    lines.add("  }");
    lines.add("");
    lines.add("  private " + className + "() {}");
    lines.add("}");
    Files.write(javaPath, lines, StandardCharsets.UTF_8);
  }
}
