package datadog.trace.agent.tooling.bytebuddy.matcher.memoize;

import datadog.trace.api.function.Function;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizingMatchers<T> {

  public interface Matches {
    boolean matches(int matcherId);
  }

  public interface Exchange<T> {
    Matches get(T target);

    void set(T target, Matches matches);
  }

  @SuppressWarnings("rawtypes")
  private static final Function identity =
      new Function() {
        @Override
        public Object apply(Object input) {
          return input;
        }
      };

  final AtomicInteger nextMatcherId = new AtomicInteger();

  final Map<Function<T, ?>, List<RecordingMatcher>> extractorsAndMatchers = new HashMap<>();

  final Exchange<T> exchange;

  public MemoizingMatchers(Exchange<T> exchange) {
    this.exchange = exchange;
  }

  @SuppressWarnings("unchecked")
  public <M> ElementMatcher.Junction<T> memoize(ElementMatcher<? super M> matcher) {
    return memoize(identity, matcher);
  }

  public <M> ElementMatcher.Junction<T> memoize(
      Function<T, M> extractor, ElementMatcher<? super M> matcher) {
    List<RecordingMatcher> matchers = extractorsAndMatchers.get(extractor);
    if (null == matchers) {
      extractorsAndMatchers.put(extractor, matchers = new ArrayList<>());
    }
    RecordingMatcher recordingMatcher = new RecordingMatcher(matcher);
    matchers.add(recordingMatcher);
    return recordingMatcher;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private final class RecordingMatcher extends ElementMatcher.Junction.ForNonNullValues<T> {
    private final int matcherId = nextMatcherId.getAndIncrement();

    private final ElementMatcher matcher;

    public RecordingMatcher(ElementMatcher matcher) {
      this.matcher = matcher;
    }

    @Override
    protected boolean doMatch(T target) {
      Matches matches = exchange.get(target);
      if (null != matches) {
        return matches.matches(matcherId);
      }

      BitSet bits = new BitSet();
      for (Map.Entry<Function<T, ?>, List<RecordingMatcher>> e : extractorsAndMatchers.entrySet()) {
        Object matchee = e.getKey().apply(target);
        for (RecordingMatcher m : e.getValue()) {
          if (m.matcher.matches(matchee)) {
            bits.set(m.matcherId);
          }
        }
      }
      exchange.set(target, new Memoized(bits));
      return bits.get(matcherId);
    }
  }

  static final class Memoized implements Matches {
    private final BitSet matches;

    Memoized(BitSet matches) {
      this.matches = matches;
    }

    @Override
    public boolean matches(int matcherId) {
      return matches.get(matcherId);
    }
  }
}
