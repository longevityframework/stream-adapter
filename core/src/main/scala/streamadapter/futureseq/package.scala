package streamadapter

import java.io.Closeable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.duration.Duration

/** TODO */
package object futureseq {

  /** TODO */
  type EffectiveFutureSeq[F[_], A] = Future[Seq[A]]

  /** produces a publisher adapter from iterator generator to future sequence */
  implicit def iterGenToFutureSeq(implicit context: ExecutionContext) = {
    new PublisherAdapter[NoEffect, EffectiveIterGen, NoEffect, EffectiveFutureSeq] {
      def adaptPublisher[A](iterGen: IterGen[A]): Future[Seq[A]] = {
        Future(blocking(iterGen().toSeq))
      }
    }
  }

  /** produces a publisher adapter from future sequence to iterator generator */
  implicit def futureSeqToIterGen(implicit context: ExecutionContext) = {
    new PublisherAdapter[NoEffect, EffectiveFutureSeq, NoEffect, EffectiveIterGen] {
      def adaptPublisher[A](futureSeq: Future[Seq[A]]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          private lazy val i = Await.result(futureSeq, Duration.Inf).toIterator
          def hasNext = i.hasNext
          def next    = i.next
          def close   = ()
        }
      }
    }
  }

}
