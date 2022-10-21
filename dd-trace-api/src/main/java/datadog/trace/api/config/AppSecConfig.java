package datadog.trace.api.config;

/**
 * Constant with names of configuration options for appsec.
 */
public final class AppSecConfig {
  public static final String APPSEC_ENABLED = "appsec.enabled";
  public static final String APPSEC_REPORTING_INBAND = "appsec.reporting.inband";
  public static final String APPSEC_RULES_FILE = "appsec.rules";
  public static final String APPSEC_REPORT_TIMEOUT_SEC = "appsec.report.timeout";
  public static final String APPSEC_IP_ADDR_HEADER = "appsec.ipheader";
  public static final String APPSEC_TRACE_RATE_LIMIT = "appsec.trace.rate.limit";
  public static final String APPSEC_WAF_METRICS = "appsec.waf.metrics";
  public static final String APPSEC_OBFUSCATION_PARAMETER_KEY_REGEXP = "appsec.obfuscation.parameter_key_regexp";
  public static final String APPSEC_OBFUSCATION_PARAMETER_VALUE_REGEXP = "appsec.obfuscation.parameter_value_regexp";
  public static final String APPSEC_HTTP_BLOCKED_TEMPLATE_HTML = "appsec.http.blocked.template.html";
  public static final String APPSEC_HTTP_BLOCKED_TEMPLATE_JSON = "appsec.http.blocked.template.json";

  static final String DEFAULT_APPSEC_ENABLED = "inactive";
  static final boolean DEFAULT_APPSEC_REPORTING_INBAND = false;
  static final int DEFAULT_APPSEC_TRACE_RATE_LIMIT = 100;
  static final boolean DEFAULT_APPSEC_WAF_METRICS = true;

  private AppSecConfig() {
  }
}
