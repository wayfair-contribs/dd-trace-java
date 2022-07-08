package datadog.trace.agent.tooling.bytebuddy;

import java.util.BitSet;

/**
 * Placeholder for generated {@link datadog.trace.agent.tooling.Instrumenter} class-name trie that
 * maps known instrumented types to their instrumentation-id(s). Note an instrumented type can match
 * multiple instrumentations.
 *
 * @see datadog.trace.agent.tooling.Instrumenters#currentInstrumentationId()
 */
public final class InstrumenterTrie {
  public static final boolean ENABLED = false;

  /**
   * Sets the instrumentation-ids of any instrumenters that explicitly match the given class-name.
   */
  public static void apply(String className, BitSet instrumentationIds) {
    throw new UnsupportedOperationException("Unexpected call to placeholder class");
  }
}
