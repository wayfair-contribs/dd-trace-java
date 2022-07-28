package datadog.trace.api.iast


import datadog.trace.test.util.DDSpecification

class InstrumentationBridgeTest extends DDSpecification {

  private IASTModule defaultModule

  def setup() {
    defaultModule = InstrumentationBridge.MODULE
  }

  def cleanup() {
    InstrumentationBridge.MODULE = defaultModule
  }

  def "bridge calls module when a new hash is detected"() {
    setup:
    InstrumentationBridge.MODULE = Mock(IASTModule)

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    1 * InstrumentationBridge.MODULE.onHashingAlgorithm('SHA-1')
  }

  def "bridge calls don't fail with null module when a new hash is detected"() {
    setup:
    InstrumentationBridge.MODULE = null

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a new hash is detected"() {
    setup:
    InstrumentationBridge.MODULE = Mock(IASTModule)

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    1 * InstrumentationBridge.MODULE.onHashingAlgorithm(_) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new cipher is detected"() {
    setup:
    InstrumentationBridge.MODULE = Mock(IASTModule)

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    1 * InstrumentationBridge.MODULE.onCipherAlgorithm('AES')
  }

  def "bridge calls don't fail with null module when a new cipher is detected"() {
    setup:
    InstrumentationBridge.MODULE = null

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a new cipher is detected"() {
    setup:
    InstrumentationBridge.MODULE = Mock(IASTModule)

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    1 * InstrumentationBridge.MODULE.onCipherAlgorithm(_) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }
}