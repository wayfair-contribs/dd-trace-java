package datadog.trace.agent.tooling.bytebuddy;

import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.Instrumenters;
import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import java.io.IOException;
import java.util.BitSet;

/**
 * Validates the generated {@link InstrumenterTrie} produces the expected match results for all
 * {@code META-INF/services/datadog.trace.agent.tooling.Instrumenter} services registered with the
 * 'instrumentation' module of the Java Agent.
 */
public final class InstrumenterTrieValidator {

  public static void main(String[] args) throws IOException {

    // satisfy some instrumenters that cache matchers in initializers
    HierarchyMatchers.registerIfAbsent(HierarchyMatchers.simpleChecks());
    SharedTypePools.registerIfAbsent(SharedTypePools.simpleCache());

    for (Instrumenter instrumenter : Instrumenters.load(Instrumenter.class.getClassLoader())) {
      int instrumentationId = Instrumenters.currentInstrumentationId();
      if (instrumenter instanceof Instrumenter.ForSingleType) {
        String name = ((Instrumenter.ForSingleType) instrumenter).instrumentedType();
        validate(instrumenter, name, instrumentationId);
      } else if (instrumenter instanceof Instrumenter.ForKnownTypes) {
        for (String name : ((Instrumenter.ForKnownTypes) instrumenter).knownMatchingTypes()) {
          validate(instrumenter, name, instrumentationId);
        }
      }
    }
  }

  /** Validates a single match from class-name to instrumentation-id. */
  private static void validate(Instrumenter instrumenter, String name, int instrumentationId) {
    BitSet matches = new BitSet();
    InstrumenterTrie.apply(name, matches);
    if (!matches.get(instrumentationId)) {
      throw new IllegalStateException(
          instrumenter.getClass() + " expected to match " + name + " but didn't");
    }
  }
}
