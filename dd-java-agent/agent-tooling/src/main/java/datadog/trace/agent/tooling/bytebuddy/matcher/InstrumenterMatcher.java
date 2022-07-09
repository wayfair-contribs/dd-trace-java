package datadog.trace.agent.tooling.bytebuddy.matcher;

import java.util.BitSet;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Matches when a specific instrumentation is recorded as a match for the current transform target.
 */
public final class InstrumenterMatcher extends ElementMatcher.Junction.ForNonNullValues<TypeDescription> {

  private static final ThreadLocal<BitSet> matchedIds =
      new ThreadLocal<BitSet>() {
        @Override
        protected BitSet initialValue() {
          return new BitSet();
        }
      };

  private final int instrumentationId;

  public InstrumenterMatcher(int instrumentationId) {
    this.instrumentationId = instrumentationId;
  }

  @Override
  protected final boolean doMatch(TypeDescription target) {
    return matchedIds.get().get(instrumentationId);
  }

  /** Records an instrumentation-id as a match for the current transform target. */
  public static void set(int instrumentationId) {
    matchedIds.get().set(instrumentationId);
  }

  /** Records instrumentation-ids as a match for the current transform target. */
  public static void set(BitSet instrumentationIds) {
    matchedIds.get().or(instrumentationIds);
  }

  /** Clears previous match results in preparation for a new transform target. */
  public static void reset() {
    matchedIds.get().clear();
  }
}
