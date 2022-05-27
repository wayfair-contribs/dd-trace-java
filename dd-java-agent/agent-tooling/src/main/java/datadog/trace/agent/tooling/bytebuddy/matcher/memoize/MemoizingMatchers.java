package datadog.trace.agent.tooling.bytebuddy.matcher.memoize;

import datadog.trace.api.function.Function;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class MemoizingMatchers<T> {

  public interface Matches {
    boolean matches(int matcherId);
  }

  public interface Exchange<T> {
    Matches get(T target);

    void set(T target, Matches matches);
  }

  public static final Matches NO_MATCHES =
      new Matches() {
        @Override
        public boolean matches(int matcherId) {
          return false;
        }
      };

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
      Function<T, ? extends Iterable<? extends M>> extractor, ElementMatcher<? super M> matcher) {
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
      for (Map.Entry<Function<T, ?>, List<RecordingMatcher>> entry :
          extractorsAndMatchers.entrySet()) {
        Object extracted = entry.getKey().apply(target);
        if (extracted == target || !(extracted instanceof Iterable<?>)) {
          for (RecordingMatcher recordingMatcher : entry.getValue()) {
            if (recordingMatcher.matcher.matches(extracted)) {
              bits.set(recordingMatcher.matcherId);
            }
          }
        } else {
          Deque<RecordingMatcher> pendingMatchers = new ArrayDeque<>(entry.getValue());
          for (Object item : ((Iterable<?>) extracted)) {
            if (item instanceof TypeDescription) {
              Matches cachedMatches = exchange.get((T) item);
              if (null != cachedMatches) {
                if (cachedMatches instanceof Memoized) {
                  bits.or(((Memoized) cachedMatches).matches);
                }
                continue;
              }
            }
            Iterator<RecordingMatcher> itr = pendingMatchers.iterator();
            if (!itr.hasNext()) {
              break;
            }
            while (itr.hasNext()) {
              RecordingMatcher recordingMatcher = itr.next();
              if (recordingMatcher.matcher.matches(item)) {
                bits.set(recordingMatcher.matcherId);
                itr.remove();
              }
            }
          }
        }
      }
      if (bits.isEmpty()) {
        exchange.set(target, NO_MATCHES);
      } else {
        exchange.set(target, new Memoized(bits));
      }
      return bits.get(matcherId);
    }
  }

  static final class Memoized implements Matches {
    final BitSet matches;

    Memoized(BitSet matches) {
      this.matches = matches;
    }

    @Override
    public boolean matches(int matcherId) {
      return matches.get(matcherId);
    }
  }
}
