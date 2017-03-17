package streamadapter

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking

/** TODO */
package object futurevec {

  /** TODO */
  type FutureVec[A] = Future[Vector[A]]

  /** produces a publisher adapter from chunkerator to future vector */
  implicit def chunkeratorToFutureVec(implicit context: ExecutionContext) = {
    new PublisherAdapter[Chunkerator, FutureVec] {
      def adapt[A](chunkerator: Chunkerator[A]): Future[Vector[A]] = {
        Future(blocking(chunkerator.toVector))
      }
    }
  }

  /** produces a publisher adapter from future vector to chunkerator */
  implicit def futureVecToChunkerator(implicit context: ExecutionContext) = {
    new PublisherAdapter[FutureVec, Chunkerator] {
      def adapt[A](futureVec: Future[Vector[A]]): Chunkerator[A] = { () =>
        new CloseableChunkIter[A] {
          private lazy val as = Await.result(futureVec, streamadapter.timeout)
          private var done = false
          def hasNext = !done
          def next = {
            done = true
            as
          }
          def close = ()
        }
      }
    }
  }

}
