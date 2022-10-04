package datadog.trace.api.iast


import datadog.trace.test.util.DDSpecification

class InstrumentationBridgeTest extends DDSpecification {

  def "bridge calls module when a new hash is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    1 * module.onHashingAlgorithm('SHA-1')
  }

  def "bridge calls don't fail with null module when a new hash is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a new hash is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onMessageDigestGetInstance('SHA-1')

    then:
    1 * module.onHashingAlgorithm(_) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new cipher is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    1 * module.onCipherAlgorithm('AES')
  }

  def "bridge calls don't fail with null module when a new cipher is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a new cipher is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onCipherGetInstance('AES')

    then:
    1 * module.onCipherAlgorithm(_) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string concat is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    1 * module.onStringConcat('Hello ', 'World!', 'Hello World!')
  }

  def "bridge calls don't fail with null module when a string concat is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string concat is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringConcat('Hello ', 'World!', 'Hello World!')

    then:
    1 * module.onStringConcat(_, _, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder init is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderInit(self, 'test')

    then:
    1 * module.onStringBuilderAppend(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder init is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderInit(new StringBuilder(), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder init is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderInit(new StringBuilder(), 'test')

    then:
    1 * module.onStringBuilderAppend(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder append is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderAppend(self, 'test')

    then:
    1 * module.onStringBuilderAppend(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder append is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderAppend(new StringBuilder(), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder append is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderAppend(new StringBuilder(), 'test')

    then:
    1 * module.onStringBuilderAppend(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a new string builder toString() is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final self = new StringBuilder()

    when:
    InstrumentationBridge.onStringBuilderToString(self, 'test')

    then:
    1 * module.onStringBuilderToString(self, 'test')
  }

  def "bridge calls don't fail with null module when a string builder toString is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringBuilderToString(new StringBuilder('test'), 'test')

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when a string builder toString is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringBuilderToString(new StringBuilder('test'), 'test')

    then:
    1 * module.onStringBuilderToString(_, _) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions for onParameterName and onParameterValue on null parameters"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onParameterValue(null, null)
    InstrumentationBridge.onParameterName()

    then:
    noExceptionThrown()
  }

  def "bridge calls module when a String.subSequence(int beginIndex, int endIndex) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringSubSequence("Hello", 1, 3, "el")

    then:
    1 * module.onStringSubSequence("Hello", 1, 3, "el")
  }

  def "bridge calls don't fail with null module when a String.subSequence(int beginIndex, int endIndex) method is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)

    when:
    InstrumentationBridge.onStringSubSequence("Hello", 1, 3, "el")

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when String.subSequence(int beginIndex, int endIndex) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)

    when:
    InstrumentationBridge.onStringSubSequence("Hello", 1, 3, "el")

    then:
    1 * module.onStringSubSequence("Hello", 1, 3, "el") >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls module when a String.join(CharSequence delimiter, CharSequence[] elements) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final elements = new CharSequence[2]
    elements[0] = "1"
    elements[1] = "3"
    final result = "1-3"
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements, result)

    then:
    1 * module.onStringJoin(result, delimiter, elements)
  }

  def "bridge calls module when a String.join(CharSequence delimiter, Iterable<? extends CharSequence> elements) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final elements = ["1", "3"]
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements)

    then:
    1 * module.onStringJoin(delimiter, elements)
  }

  def "bridge calls don't fail with null module when a String.join(CharSequence delimiter, CharSequence[] elements) method is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)
    final elements = new CharSequence[2]
    elements[0] = "1"
    elements[1] = "3"
    final result = "1-3"
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements, result)

    then:
    noExceptionThrown()
  }

  def "bridge calls don't fail with null module when a String.join(CharSequence delimiter, Iterable<? extends CharSequence> elements) method is detected"() {
    setup:
    InstrumentationBridge.registerIastModule(null)
    final elements = ["1", "3"]
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements)

    then:
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when String.join(CharSequence delimiter, CharSequence[] elements) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final elements = new CharSequence[2]
    elements[0] = "1"
    elements[1] = "3"
    final result = "1-3"
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements, result)

    then:
    1 * module.onStringJoin(result, delimiter, elements) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }

  def "bridge calls don't leak exceptions when String.join(CharSequence delimiter, Iterable<? extends CharSequence> elements) method is detected"() {
    setup:
    final module = Mock(IastModule)
    InstrumentationBridge.registerIastModule(module)
    final elements = ["1", "3"]
    final delimiter = "-"

    when:
    InstrumentationBridge.onStringJoin(delimiter, elements)

    then:
    1 * module.onStringJoin(delimiter, elements) >> { throw new Error('Boom!!!') }
    noExceptionThrown()
  }
}
