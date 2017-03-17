package streamadapter

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking

/** contains [[StreamAdapter stream adapters]] for `Future[Vector[_]]`. this is a degenerate case -
 * it is "reactive", but it only relies on Scala standard library.
 */
package object futurevec {

  /** a `Future[Vector[_]]` */
  type FutureVec[A] = Future[Vector[A]]

  /** produces a publisher adapter from chunkerator to future vector */
  implicit def chunkeratorToFutureVec(implicit context: ExecutionContext) = {
    new StreamAdapter[Chunkerator, FutureVec] {
      def adapt[A](chunkerator: Chunkerator[A]): Future[Vector[A]] = {
        Future(blocking(chunkerator.toVector))
      }
    }
  }

  /** produces a publisher adapter from future vector to chunkerator */
  implicit def futureVecToChunkerator(implicit context: ExecutionContext) = {
    new StreamAdapter[FutureVec, Chunkerator] {
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
