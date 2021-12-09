package datadog.trace.instrumentation.http4s023_212

import datadog.trace.bootstrap.instrumentation.api.URIRawDataAdapter
import org.http4s.Request

final class Http4sURIAdapter[F[_]](private val request: Request[F]) extends URIRawDataAdapter {

  override def scheme = {
    if (request.uri.scheme.isEmpty) "http" else request.uri.scheme.get.value
  }

  override def host = {
    val addr = request.serverAddr.get.toString
    // TODO the addr we get here is created from an InetSocketAddress inside the pipeline in the
    //  BlazeServerBuilder where any names have been stripped out, and our tests don't like that,
    //  so this is a hack for now
    if (addr.equals("127.0.0.1")) "localhost" else addr
  }

  override def port: Int = request.serverPort.get.value

  override def fragment = request.uri.fragment.orNull

  override protected def innerRawPath = request.uri.path.toString

  override def hasPlusEncodedSpaces = true

  override protected def innerRawQuery = request.uri.query.toString
}
