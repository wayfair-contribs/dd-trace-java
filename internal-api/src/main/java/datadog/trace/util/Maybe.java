package datadog.trace.util;

public interface Maybe<V> {
  boolean isPresent();

  V get();

  abstract class Values {

    private Values() {}

    public static <V> Maybe<V> empty() {
      return (Maybe<V>) EMPTY;
    }

    public static <V> Maybe<V> of(V value) {
      return new Present<>(value);
    }

    private static final Maybe<?> EMPTY =
        new Maybe<Object>() {
          @Override
          public boolean isPresent() {
            return false;
          }

          @Override
          public Object get() {
            throw new IllegalArgumentException("Not present");
          }
        };

    private static class Present<V> implements Maybe<V> {
      private final V value;

      public Present(final V value) {
        this.value = value;
      }

      @Override
      public boolean isPresent() {
        return true;
      }

      @Override
      public V get() {
        return value;
      }
    }
  }
}
