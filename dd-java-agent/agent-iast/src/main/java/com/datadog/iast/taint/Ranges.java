package com.datadog.iast.taint;

import com.datadog.iast.model.Range;
import com.datadog.iast.model.Source;
import javax.annotation.Nonnull;

/** Utilities to work with {@link Range} instances. */
public final class Ranges {

  public static final Range[] EMPTY = new Range[0];

  private Ranges() {}

  public static Range[] forString(final @Nonnull String obj, final @Nonnull Source source) {
    return new Range[] {new Range(0, obj.length(), source)};
  }

  public static void copyShift(
      final @Nonnull Range[] src, final @Nonnull Range[] dst, final int dstPos, final int shift) {
    if (shift == 0) {
      System.arraycopy(src, 0, dst, dstPos, src.length);
    } else {
      for (int iSrc = 0, iDst = dstPos; iSrc < src.length; iSrc++, iDst++) {
        dst[iDst] = src[iSrc].shift(shift);
      }
    }
  }

  public static Range createIfDifferent(Range range, int start, int length) {
    if (start != range.getStart() || length != range.getLength()) {
      return new Range(start, length, range.getSource());
    } else {
      return range;
    }
  }

  public static Range[] forSubstring(int offset, int length, @Nonnull Range[] ranges) {

    int skippedRanges = getSubstringSkippedRanges(offset, length, ranges);

    // Range adjusting
    if (0 == ranges.length - skippedRanges) {
      return null;
    }

    Range[] newRanges = new Range[ranges.length - skippedRanges];
    int newRangeIndex = 0;
    for (final Range rangeSelf : ranges) {
      int newStart = rangeSelf.getStart() - offset;
      int newLength = rangeSelf.getLength();
      final int newEnd = newStart + newLength;
      if (newStart < 0) {
        newLength = newLength + newStart;
        newStart = 0;
      }
      if (newEnd > length) {
        newLength = length - newStart;
      }
      if (newLength > 0) {
        newRanges[newRangeIndex] = createIfDifferent(rangeSelf, newStart, newLength);
        newRangeIndex++;
      }
    }

    return newRanges;
  }

  private static int getSubstringSkippedRanges(int offset, int length, Range[] ranges) {
    int skippedRanges = 0;
    for (final Range rangeSelf : ranges) {
      if (rangeSelf.getStart() + rangeSelf.getLength() <= offset) {
        skippedRanges++;
      } else {
        break;
      }
    }

    for (int rangeIndex = ranges.length - 1; rangeIndex >= 0; rangeIndex--) {
      final Range rangeSelf = ranges[rangeIndex];
      if (rangeSelf.getStart() - offset >= length) {
        skippedRanges++;
      } else {
        break;
      }
    }
    return skippedRanges;
  }
}
