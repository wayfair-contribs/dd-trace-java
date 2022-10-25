package datadog.smoketest

import datadog.trace.api.Platform
import groovy.json.JsonSlurper
import okhttp3.Request
import okhttp3.Response
import spock.lang.IgnoreIf

import java.util.function.Predicate

@IgnoreIf({
  !Platform.isJavaVersionAtLeast(8)
})
class IastSpringBootSmokeTest extends AbstractServerSmokeTest {

  private static final String TAG_NAME = '_dd.iast.json'

  @Override
  def logLevel() {
    return "debug"
  }

  @Override
  ProcessBuilder createProcessBuilder() {
    String springBootShadowJar = System.getProperty("datadog.smoketest.springboot.shadowJar.path")

    List<String> command = new ArrayList<>()
    command.add(javaPath())
    command.addAll(defaultJavaProperties)
    command.addAll([
      "-Ddd.appsec.enabled=true",
      "-Ddd.iast.enabled=true",
      "-Ddd.iast.request-sampling=100"
    ])
    command.addAll((String[]) ["-jar", springBootShadowJar, "--server.port=${httpPort}"])
    ProcessBuilder processBuilder = new ProcessBuilder(command)
    processBuilder.directory(new File(buildDirectory))
    // Spring will print all environment variables to the log, which may pollute it and affect log assertions.
    processBuilder.environment().clear()
    processBuilder
  }

  def "IAST subsystem starts"() {
    given: 'an initial request has succeeded'
    String url = "http://localhost:${httpPort}/greeting"
    def request = new Request.Builder().url(url).get().build()
    client.newCall(request).execute()

    when: 'logs are read'
    String startMsg = null
    String errorMsg = null
    checkLog {
      if (it.contains("Not starting IAST subsystem")) {
        errorMsg = it
      }
      if (it.contains("IAST is starting")) {
        startMsg = it
      }
      // Check that there's no logged exception about missing classes from Datadog.
      // We had this problem before with JDK9StackWalker.
      if (it.contains("java.lang.ClassNotFoundException: datadog/")) {
        errorMsg = it
      }
    }

    then: 'there are no errors in the log and IAST has started'
    errorMsg == null
    startMsg != null
    !logHasErrors
  }

  def "default home page without errors"() {
    setup:
    String url = "http://localhost:${httpPort}/greeting"
    def request = new Request.Builder().url(url).get().build()

    when:
    def response = client.newCall(request).execute()

    then:
    def responseBodyStr = response.body().string()
    responseBodyStr != null
    responseBodyStr.contains("Sup Dawg")
    response.body().contentType().toString().contains("text/plain")
    response.code() == 200

    checkLog()
    !logHasErrors
  }

  def "iast.enabled tag is present"() {
    setup:
    String url = "http://localhost:${httpPort}/greeting"
    def request = new Request.Builder().url(url).get().build()

    when:
    client.newCall(request).execute()

    then:
    waitForTraceCount(1)
    Boolean foundEnabledTag = false
    checkLog {
      if (it.contains("_dd.iast.enabled=1")) {
        foundEnabledTag = true
      }
    }
    foundEnabledTag
  }

  def "weak hash vulnerability is present"() {
    setup:
    String url = "http://localhost:${httpPort}/weakhash"
    def request = new Request.Builder().url(url).get().build()

    when:
    Response response = client.newCall(request).execute()

    then:
    response.body().string().contains("MessageDigest.getInstance executed")
    Thread.sleep(100) //This is needed so we allow enough time for the log to be written
    final found = hasVulnerability(type('WEAK_HASH').and(evidence('MD5')))
    found
  }

  def "weak hash vulnerability is present on boot"() {
    when:
    final found = hasVulnerability(type('WEAK_HASH').and(evidence('SHA1')).and(withSpan()))

    then:
    found
  }

  private boolean hasVulnerability(final Predicate<?> predicate) {
    def found = false
    final slurper = new JsonSlurper()
    checkLog { final String log ->
      final index = log.indexOf(TAG_NAME)
      if (index >= 0) {
        final json = slurper.parseText(parseVulnerability(log, index))
        found |= (json['vulnerabilities'] as Collection).stream().anyMatch(predicate)
      }
    }
    return found
  }

  private static String parseVulnerability(final String log, final int index) {
    final chars = log.toCharArray()
    final builder = new StringBuilder()
    def level = 0
    for (int i = log.indexOf('{', index); i < chars.length; i++) {
      final current = chars[i]
      if (current == '{' as char) {
        level++
      } else if (current == '}' as char) {
        level--
      }
      builder.append(chars[i])
      if (level == 0) {
        break
      }
    }
    return builder.toString()
  }

  private static Predicate<?> type(final String type) {
    return { vul -> vul.type == type }
  }

  private static Predicate<?> evidence(final String value) {
    return { vul -> vul.evidence.value == value }
  }

  private static Predicate<?> withSpan() {
    return { vul ->
      vul.location.span != null
    }
  }
}
