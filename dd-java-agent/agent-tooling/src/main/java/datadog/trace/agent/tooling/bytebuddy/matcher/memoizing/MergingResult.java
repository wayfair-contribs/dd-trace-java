package datadog.trace.agent.tooling.bytebuddy.matcher.memoizing;

import java.util.BitSet;

public abstract class MergingResult<T> implements MemoizingMatchers.Result {
  private final BitSet matches;
  private final int offset;

  public MergingResult() {
    this(new BitSet(), 0);
  }

  public MergingResult(BitSet matches, int offset) {
    this.matches = matches;
    this.offset = offset;
  }

  @Override
  public boolean matches(int localMatcherId) {
    matches.set(offset + localMatcherId);
    return true;
  }

  @Override
  public boolean merging() {
    return true;
  }

  protected MemoizingMatchers.Result merged() {
    return new MergedResult<>(matches, offset);
  }
}
