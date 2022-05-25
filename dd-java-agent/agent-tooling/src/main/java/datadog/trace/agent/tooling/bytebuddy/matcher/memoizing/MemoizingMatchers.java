package datadog.trace.agent.tooling.bytebuddy.matcher.memoizing;

import datadog.trace.api.function.Function;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizingMatchers<T, M> {

  public interface State<M> {
    boolean matches(int matcherId);

    boolean merging();

    void memoize(int matcherId, ElementMatcher<M> matcher);

    void merge();
  }

  @SuppressWarnings("rawtypes")
  private static final State NO_MATCHES =
      new State() {
        @Override
        public boolean matches(int matcherId) {
          return false;
        }

        @Override
        public boolean merging() {
          return false;
        }

        @Override
        public void memoize(int matcherId, ElementMatcher matcher) {}

        @Override
        public void merge() {}
      };

  final List<MemoizingMatcher> matchers = new ArrayList<>();

  final AtomicInteger nextMatcherId;

  final Function<T, State<M>> stateFunction;

  public MemoizingMatchers(Function<T, State<M>> stateFunction) {
    this(new AtomicInteger(), stateFunction);
  }

  public MemoizingMatchers(AtomicInteger nextMatcherId, Function<T, State<M>> stateFunction) {
    this.nextMatcherId = nextMatcherId;
    this.stateFunction = stateFunction;
  }

  @SuppressWarnings("unchecked")
  public static <M> State<M> noMatches() {
    return NO_MATCHES;
  }

  public ElementMatcher.Junction<T> memoize(ElementMatcher<M> matcher) {
    MemoizingMatcher memoizingMatcher = new MemoizingMatcher(matcher);
    matchers.add(memoizingMatcher);
    return memoizingMatcher;
  }

  private final class MemoizingMatcher extends ElementMatcher.Junction.AbstractBase<T> {
    private final int matcherId = nextMatcherId.getAndIncrement();

    private final ElementMatcher<M> matcher;

    public MemoizingMatcher(ElementMatcher<M> matcher) {
      this.matcher = matcher;
    }

    @Override
    public boolean matches(T target) {
      State<M> state = stateFunction.apply(target);
      if (state.merging()) {
        for (MemoizingMatcher m : matchers) {
          state.memoize(m.matcherId, m.matcher);
        }
        state.merge();
      }
      return state.matches(matcherId);
    }
  }

  public abstract static class Merging<T, M> implements State<M> {
    protected final BitSet matches;

    protected final T target;

    public Merging(T target) {
      this(target, new BitSet());
    }

    public Merging(T target, BitSet matches) {
      this.matches = matches;
      this.target = target;
    }

    @Override
    public final boolean matches(int matcherId) {
      return matches.get(matcherId);
    }

    @Override
    public final boolean merging() {
      return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void memoize(int matcherId, ElementMatcher<M> matcher) {
      if (matcher.matches((M) target)) {
        matches.set(matcherId);
      }
    }

    protected final State<M> merged() {
      return new Merged<>(matches);
    }
  }

  static final class Merged<M> implements State<M> {
    private final BitSet matches;

    Merged(BitSet matches) {
      this.matches = matches;
    }

    @Override
    public boolean matches(int matcherId) {
      return matches.get(matcherId);
    }

    @Override
    public boolean merging() {
      return false;
    }

    @Override
    public void memoize(int matcherId, ElementMatcher<M> matcher) {}

    @Override
    public void merge() {}
  }
}
