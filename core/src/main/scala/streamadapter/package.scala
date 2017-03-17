import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** a toolkit for adapting streams from one streaming library into another streaming library */
package object streamadapter {

  /** chains two stream adapters together to create a new one. the intermediary in the chain is
   * hardcoded to `Chunkerator`.
   *
   * the idea being, every streaming library has adapters into and out of `Chunkerator`, and then we
   * can adapt between two arbitrary streaming libraries.
   */
  implicit def chain[P1[_], P2[_]](
    implicit pa1: StreamAdapter[P1, Chunkerator],
    pa2: StreamAdapter[Chunkerator, P2]): StreamAdapter[P1, P2] =
    new StreamAdapter[P1, P2] {
      def adapt[A](p1: P1[A]): P2[A] = pa2.adapt(pa1.adapt(p1))
    }

  /** adapts a stream of elements from one streaming library into a stream from another streaming library
   * @tparam P1 the type of stream to adapt from
   * @tparam P2 the type of stream to adapt into
   * @tparam A the type of the elements of the stream
   * @param p1 the stream to adapt
   * @return the adapted stream
   */
  def adapt[P1[_], P2[_], A](p1: P1[A])(implicit pa: StreamAdapter[P1, P2]): P2[A] =
    pa.adapt(p1)

  private[streamadapter] def trampoline[A](f: => Future[A])(implicit context: ExecutionContext): Future[A] =
    Future(()).flatMap { _ => f }

  private[streamadapter] val timeout = Duration(ConfigFactory.load().getString("streamadapter.timeout"))

}
