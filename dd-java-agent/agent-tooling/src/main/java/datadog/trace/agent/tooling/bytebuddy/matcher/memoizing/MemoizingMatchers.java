package datadog.trace.agent.tooling.bytebuddy.matcher.memoizing;

import datadog.trace.api.function.Function;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizingMatchers<T> {

  public interface Result {
    boolean matches(int localMatcherId);

    boolean merging();

    void merge();
  }

  public static final Result NO_MATCHES =
      new Result() {
        @Override
        public boolean matches(int localMatcherId) {
          return false;
        }

        @Override
        public boolean merging() {
          return false;
        }

        @Override
        public void merge() {}
      };

  final List<ElementMatcher<? super T>> matchers = new ArrayList<>();

  final Function<T, Result> results;

  public MemoizingMatchers(Function<T, Result> results) {
    this.results = results;
  }

  public ElementMatcher.Junction<T> memoize(ElementMatcher.Junction<T> matcher) {
    final int localMatcherId = matchers.size();
    matchers.add(matcher);
    return new MemoizingMatcher(localMatcherId);
  }

  private final class MemoizingMatcher extends ElementMatcher.Junction.AbstractBase<T> {
    private final int localMatcherId;

    MemoizingMatcher(int localMatcherId) {
      this.localMatcherId = localMatcherId;
    }

    @Override
    public boolean matches(T target) {
      Result result = results.apply(target);
      if (result.merging()) {
        mergeMatches(target, result);
      }
      return result.matches(localMatcherId);
    }

    private void mergeMatches(final T target, final Result result) {
      for (int i = 0, len = matchers.size(); i < len; i++) {
        if (matchers.get(i).matches(target)) {
          result.matches(i);
        }
      }
      result.merge();
    }
  }
}
