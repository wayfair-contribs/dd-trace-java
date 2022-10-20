package datadog.trace.api.iast;

import javax.annotation.Nullable;

public interface IastModule {

  void onCipherAlgorithm(@Nullable String algorithm);

  void onHashingAlgorithm(@Nullable String algorithm);

  /**
   * An HTTP request parameter name is used. This should be used when it cannot be determined
   * whether the parameter comes in the query string or body (e.g. servlet's getParameter).
   */
  void onParameterName(@Nullable String paramName);

  /**
   * An HTTP request parameter value is used. This should be used when it cannot be determined
   * whether the parameter comes in the query string or body (e.g. servlet's getParameter).
   */
  void onParameterValue(@Nullable String paramName, @Nullable String paramValue);

  void onStringConcat(@Nullable String left, @Nullable String right, @Nullable String result);

  void onStringBuilderInit(@Nullable StringBuilder builder, @Nullable CharSequence param);

  void onStringBuilderAppend(@Nullable StringBuilder builder, @Nullable CharSequence param);

  void onStringBuilderToString(@Nullable StringBuilder builder, @Nullable String result);

  void onStringSubSequence(
      @Nullable String self, int beginIndex, int endIndex, @Nullable CharSequence result);

  void onStringJoin(@Nullable String result, CharSequence delimiter, CharSequence... elements);
}
