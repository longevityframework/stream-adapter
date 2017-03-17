import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** TODO */
package object streamadapter {

  /** TODO */
  implicit def cross[P1[_], P2[_]](
    implicit pa1: PublisherAdapter[P1, Chunkerator],
    pa2: PublisherAdapter[Chunkerator, P2]): PublisherAdapter[P1, P2] =
    new PublisherAdapter[P1, P2] {
      def adapt[A](p1: P1[A]): P2[A] = pa2.adapt(pa1.adapt(p1))
    }

  /** TODO */
  def adapt[P1[_], P2[_], A](p1: P1[A])(implicit pa: PublisherAdapter[P1, P2]): P2[A] =
    pa.adapt(p1)

  private[streamadapter] def trampoline[A](f: => Future[A])(implicit context: ExecutionContext): Future[A] =
    Future(()).flatMap { _ => f }

  private[streamadapter] val timeout = Duration(ConfigFactory.load().getString("streamadapter.timeout"))

}
