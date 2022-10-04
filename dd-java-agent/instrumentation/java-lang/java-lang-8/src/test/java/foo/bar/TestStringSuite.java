package foo.bar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStringSuite {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestStringSuite.class);

  public static String join(CharSequence delimiter, CharSequence[] elements) {
    LOGGER.debug("Before string join {} with {}", elements, delimiter);
    final String result = String.join(delimiter, elements);
    LOGGER.debug("After string join {}", result);
    return result;
  }

  public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
    LOGGER.debug("Before string join {} with {}", elements, delimiter);
    final String result = String.join(delimiter, elements);
    LOGGER.debug("After string join {}", result);
    return result;
  }
}
