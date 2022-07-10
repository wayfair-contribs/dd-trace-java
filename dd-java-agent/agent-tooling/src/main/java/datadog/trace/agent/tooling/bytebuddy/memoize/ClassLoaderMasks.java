package datadog.trace.agent.tooling.bytebuddy.memoize;

import datadog.trace.agent.tooling.WeakCaches;
import datadog.trace.api.function.Function;
import datadog.trace.bootstrap.WeakCache;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Keeps track of which instrumentations are compatible with a given class-loader and which are
 * blocked, whether this is due to the result of an eager class-loader match or a more detailed
 * (lazy) muzzle check.
 */
public final class ClassLoaderMasks {

  private static final int ADDRESS_BITS_PER_WORD = 6;
  private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
  private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

  private static final int BLOCKED = 0b01;
  private static final int APPLIES = 0b10;

  private static final int STATUS_BITS = 0b11;

  static int wordsPerMask;

  private static final WeakCache<ClassLoader, AtomicLongArray> classLoaderMasks =
      WeakCaches.newWeakCache(32);

  private ClassLoaderMasks() {}

  /** Pre-sizes class-loader mask internals to fit the largest expected instrumentation-id. */
  public static void presize(int maxInstrumentationId) {
    wordsPerMask = ((maxInstrumentationId << 1) + BITS_PER_WORD - 1) >> ADDRESS_BITS_PER_WORD;
  }

  /** Has a mask already been built for this class-loader? */
  public static boolean hasClassLoaderMask(ClassLoader classLoader) {
    return null != classLoaderMasks.getIfPresent(classLoader);
  }

  /** Builds a new mask for this class-loader with selected instrumentations eagerly blocked. */
  public static void buildClassLoaderMask(
      ClassLoader classLoader, final BitSet blockedInstrumentationIds) {
    classLoaderMasks.computeIfAbsent(
        classLoader,
        new Function<ClassLoader, AtomicLongArray>() {
          @Override
          public AtomicLongArray apply(ClassLoader input) {
            return toClassLoaderMask(blockedInstrumentationIds);
          }
        });
  }

  /**
   * Is this (unblocked) instrumentation compatible with the given class-loader?
   *
   * @returns {@code null} if lazy checks such as muzzle are still pending
   */
  public static Boolean isInstrumentationCompatible(
      ClassLoader classLoader, int instrumentationId) {
    int status = status(classLoader, instrumentationId);
    return status == 0 ? null : status == APPLIES;
  }

  /** Record that the instrumentation is compatible with the given class-loader. */
  public static void applyInstrumentation(ClassLoader classLoader, int instrumentationId) {
    update(classLoader, instrumentationId, APPLIES);
  }

  /** Record that the instrumentation is blocked for the given class-loader. */
  public static void blockInstrumentation(ClassLoader classLoader, int instrumentationId) {
    update(classLoader, instrumentationId, BLOCKED);
  }

  private static int status(ClassLoader classLoader, int instrumentationId) {
    return status(classLoaderMasks.getIfPresent(classLoader), instrumentationId << 1);
  }

  private static void update(ClassLoader classLoader, int instrumentationId, int status) {
    update(classLoaderMasks.getIfPresent(classLoader), instrumentationId << 1, status);
  }

  private static int status(AtomicLongArray mask, int bitIndex) {
    int wordIndex = bitIndex >> ADDRESS_BITS_PER_WORD;
    return STATUS_BITS & (int) (mask.get(wordIndex) >> (bitIndex & BIT_INDEX_MASK));
  }

  private static void update(AtomicLongArray mask, int bitIndex, int status) {
    int wordIndex = bitIndex >> ADDRESS_BITS_PER_WORD;
    long statusBits = ((long) status) << (bitIndex & BIT_INDEX_MASK);
    long state = mask.get(wordIndex);
    while (!mask.compareAndSet(wordIndex, state, state | statusBits)) {
      state = mask.get(wordIndex);
    }
  }

  /**
   * Converts bitset of blocked ids to the equivalent class-loader mask. Each instrumentation id
   * becomes two bits in the mask representing BLOCKED (odd bit) and APPLIES (even bit) states.
   */
  static AtomicLongArray toClassLoaderMask(BitSet blockedInstrumentIds) {
    long[] mask = new long[wordsPerMask];
    int i = 0;
    for (long blockedIdBits : blockedInstrumentIds.toLongArray()) {
      if (blockedIdBits == 0) {
        i += 2; // skip past empty section of class-loader mask
      } else {
        // expand blocked ids to odd bit positions across 2 words
        mask[i++] = toMaskBits(blockedIdBits & 0x00000000FFFFFFFFL);
        mask[i++] = toMaskBits(blockedIdBits >>> 32);
      }
    }
    return new AtomicLongArray(mask);
  }

  /**
   * Expands lower 32-bits across 64-bits, so each blocked id is at its expected (odd bit) position.
   *
   * @see <a href=https://graphics.stanford.edu/~seander/bithacks.html#InterleaveBMN>Interleave bits
   *     by Binary Magic Numbers</a>
   */
  private static long toMaskBits(long lo) {
    lo = (lo | (lo << 16)) & 0x0000FFFF0000FFFFL;
    lo = (lo | (lo << 8)) & 0x00FF00FF00FF00FFL;
    lo = (lo | (lo << 4)) & 0x0F0F0F0F0F0F0F0FL;
    lo = (lo | (lo << 2)) & 0x3333333333333333L;
    return (lo | (lo << 1)) & 0x5555555555555555L;
  }
}
