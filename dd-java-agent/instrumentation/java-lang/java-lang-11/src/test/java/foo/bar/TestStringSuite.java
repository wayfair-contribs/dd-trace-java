package foo.bar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStringSuite {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestStringSuite.class);

  public static String stringRepeat(String self, int count) {
    LOGGER.debug("Before string repeat {} times", count);
    final String result = self.repeat(count);
    LOGGER.debug("After string repeat {}", result);
    return result;
  }
}
