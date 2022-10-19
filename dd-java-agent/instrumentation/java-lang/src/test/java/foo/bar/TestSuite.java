package foo.bar;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSuite {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestSuite.class);

  public static String stringConcat(final String left, final String right) {
    LOGGER.debug("Before string concat {} {}", left, right);
    final String result = left.concat(right);
    LOGGER.debug("After string concat {}", result);
    return result;
  }

  public static String stringTrim(final String self) {
    LOGGER.debug("Before string trim {} ", self);
    final String result = self.trim();
    LOGGER.debug("After string trim {}", result);
    return result;
  }

  public static StringBuilder stringBuilderNew(final String param) {
    LOGGER.debug("Before new string builder {}", param);
    final StringBuilder result = new StringBuilder(param);
    LOGGER.debug("After new string builder {}", result);
    return result;
  }

  public static StringBuilder stringBuilderNew(final CharSequence param) {
    LOGGER.debug("Before new string builder {}", param);
    final StringBuilder result = new StringBuilder(param);
    LOGGER.debug("After new string builder {}", result);
    return result;
  }

  public static void stringBuilderAppend(final StringBuilder builder, final String param) {
    LOGGER.debug("Before string builder append {}", param);
    final StringBuilder result = builder.append(param);
    LOGGER.debug("After string builder append {}", result);
  }

  public static void stringBuilderAppend(final StringBuilder builder, final CharSequence param) {
    LOGGER.debug("Before string builder append {}", param);
    final StringBuilder result = builder.append(param);
    LOGGER.debug("After string builder append {}", result);
  }

  public static String stringBuilderToString(final StringBuilder builder) {
    LOGGER.debug("Before string builder toString {}", builder);
    final String result = builder.toString();
    LOGGER.debug("After string builder toString {}", result);
    return result;
  }

  public static String stringPlus(final String left, final String right) {
    LOGGER.debug("Before string plus {} {}", left, right);
    final String result = left + right;
    LOGGER.debug("After string builder toString {}", result);
    return result;
  }

  public static String stringToUpperCase(String in, Locale locale) {
    LOGGER.debug("Before string toUppercase {} ", in);
    if (null == locale) {
      final String result = in.toUpperCase();
      LOGGER.debug("After string toUppercase {}", result);
      return result;
    } else {
      final String result = in.toUpperCase(locale);
      LOGGER.debug("After string toUppercase {}", result);
      return result;
    }
  }

  public static String stringToLowerCase(String in, Locale locale) {
    LOGGER.debug("Before string toLowercase {} ", in);
    if (null == locale) {
      final String result = in.toLowerCase();
      LOGGER.debug("After string toLowercase {}", result);
      return result;
    } else {
      final String result = in.toLowerCase(locale);
      LOGGER.debug("After string toLowercase {}", result);
      return result;
    }
  }

  public static void main(String[] args) {
    {
      Locale.setDefault(new Locale("lt")); // setting Lithuanian as locale
      String str = "\u00cc";
      System.out.println(
          "Before case conversion is [" + str + "] and length is " + str.length()); // Ì
      String lowerCaseStr = str.toLowerCase();
      System.out.println(
          "Lower case is [" + lowerCaseStr + "] and length is " + lowerCaseStr.length()); // iı`
      for (int i = 0; i < lowerCaseStr.length(); i++) {
        System.out.println("Char at " + i + " " + lowerCaseStr.charAt(i));
      }
    }
  }
}
