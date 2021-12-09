package datadog.trace.instrumentation.http4s023_213;

import cats.data.Kleisli;
import net.bytebuddy.asm.Advice;
import org.http4s.Request;
import org.http4s.Response;
import org.http4s.blaze.server.BlazeServerBuilder;
import scala.concurrent.impl.Promise;

public class Http4sServerBuilderAdvice {
  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static <F> void enter(
      @Advice.This BlazeServerBuilder<F> zis,
      @Advice.Argument(value = 0, readOnly = false) Kleisli<F, Request<F>, Response<F>> httpApp) {
    httpApp = ServerWrapper$.MODULE$.wrapHttpApp(httpApp, zis.F());
  }

  /** Promise.Transformation was introduced in scala 2.13<br> */
  private static void muzzleCheck(final Promise.Transformation<?, ?> callback) {
    callback.submitWithValue(null);
  }
}
