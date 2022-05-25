package datadog.trace.agent.tooling.bytebuddy.matcher.memoizing;

import java.util.BitSet;

public final class MergedResult<T> implements MemoizingMatchers.Result {
  private final BitSet matches;
  private final int offset;

  public MergedResult(BitSet matches, int offset) {
    this.matches = matches;
    this.offset = offset;
  }

  @Override
  public boolean matches(int localMatcherId) {
    return matches.get(offset + localMatcherId);
  }

  @Override
  public boolean merging() {
    return false;
  }

  @Override
  public void merge() {}
}
