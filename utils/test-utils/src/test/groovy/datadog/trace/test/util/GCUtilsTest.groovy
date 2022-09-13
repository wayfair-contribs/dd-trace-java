package datadog.trace.test.util

import spock.lang.Specification
import spock.lang.Timeout

import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.MINUTES)
class GCUtilsTest extends Specification {

  def 'awaitGC'() {
    given:
    Object o = new Object()
    WeakReference<?> ref = new WeakReference<>(o)
    o = null

    when:
    GCUtils.awaitGC()

    then:
    ref.get() == null

    where:
    i << (1..10)
  }
}
