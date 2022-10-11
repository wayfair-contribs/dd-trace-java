package datadog.trace.api.iast;

import java.util.Locale;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge between instrumentations and {@link IastModule} that contains the business logic relative
 * to vulnerability detection. The class contains a list of {@code public static} methods that will
 * be injected into the bytecode via {@code invokestatic} instructions. It's important that all
 * methods are protected from exception leakage.
 */
public abstract class InstrumentationBridge {

  private static final Logger LOG = LoggerFactory.getLogger(InstrumentationBridge.class);

  private static IastModule MODULE;

  private InstrumentationBridge() {}

  public static void registerIastModule(final IastModule module) {
    MODULE = module;
  }

  /**
   * Executed when access to a cryptographic cipher is detected
   *
   * <p>{@link javax.crypto.Cipher#getInstance(String)}
   */
  public static void onCipherGetInstance(@Nonnull final String algorithm) {
    try {
      if (MODULE != null) {
        MODULE.onCipherAlgorithm(algorithm);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onCipher threw.", t);
    }
  }

  /**
   * Executed when access to a message digest algorithm is detected
   *
   * <p>{@link java.security.MessageDigest#getInstance(String)}
   */
  public static void onMessageDigestGetInstance(@Nonnull final String algorithm) {
    try {
      if (MODULE != null) {
        MODULE.onHashingAlgorithm(algorithm);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onHash threw.", t);
    }
  }

  public static void onParameterName(final String parameterName) {
    try {
      if (MODULE != null) {
        MODULE.onParameterName(parameterName);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onHash threw.", t);
    }
  }

  public static void onParameterValue(final String paramName, final String paramValue) {
    try {
      if (MODULE != null) {
        MODULE.onParameterValue(paramName, paramValue);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onHash threw.", t);
    }
  }

  public static void onStringConcat(final String self, final String param, final String result) {
    try {
      if (MODULE != null) {
        MODULE.onStringConcat(self, param, result);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onStringConcat threw.", t);
    }
  }

  public static void onStringConstructor(CharSequence param, String result) {
    try {
      if (MODULE != null) {
        MODULE.onStringConstructor(param, result);
      }
    } catch (Throwable t) {
      onUnexpectedException("Callback for onStringConstructor has thrown", t);
    }
  }

  public static void onStringBuilderInit(final StringBuilder self, final CharSequence param) {
    try {
      if (MODULE != null) {
        MODULE.onStringBuilderAppend(self, param);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onStringBuilderInit threw.", t);
    }
  }

  public static void onStringBuilderAppend(final StringBuilder self, final CharSequence param) {
    try {
      if (MODULE != null) {
        MODULE.onStringBuilderAppend(self, param);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onStringBuilderAppend threw.", t);
    }
  }

  public static void onStringBuilderToString(final StringBuilder self, final String result) {
    try {
      if (MODULE != null) {
        MODULE.onStringBuilderToString(self, result);
      }
    } catch (final Throwable t) {
      onUnexpectedException("Callback for onStringBuilderToString threw.", t);
    }
  }

  private static void onUnexpectedException(final String message, final Throwable error) {
    LOG.warn(message, error);
  }

  public static String onStringFormat(Locale l, String fmt, Object[] args) {
    try {
      if (MODULE != null) {
        return MODULE.onStringFormat(l, fmt, args);
      }
    } catch (RealCallThrowable t) {
      t.rethrow();
    } catch (Throwable t) {
      onUnexpectedException("Callback for onStringBuilderToString threw.", t);
      return String.format(l, fmt, args);
    }
    return null; // unreachable
  }
}