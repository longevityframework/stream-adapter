package streamadapter

import _root_.fs2.Stream
import cats.effect.IO
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise

/** contains [[StreamAdapter stream adapters]] for FS2 streams */
package object fs2 {

  /** an FS2 stream bound to a `IO` effect */
  type FS2Stream[A] = Stream[IO, A]

  /** produces a publisher adapter from chunkerator to FS2 stream */
  implicit def chunkeratorToFS2Stream = {
    new StreamAdapter[Chunkerator, FS2Stream] {
      def adapt[A](chunkerator: Chunkerator[A]): Stream[IO, A] = {
        def iterToStream(i: CloseableChunkIter[A]): Stream[IO, A] = {
          if (i.hasNext) {
            Stream.emits(i.next) ++ iterToStream(i)
          } else {
            Stream.empty
          }
        }
        Stream.bracket[IO, CloseableChunkIter[A], A](
          IO(chunkerator()))(iterToStream, i => IO.pure(i.close))
      }
    }
  }

  // see https://github.com/longevityframework/stream-adapter/issues/4
  /** produces a publisher adapter from FS2 stream to chunkerator */
  implicit def fs2StreamToChunkerator(implicit context: ExecutionContext) = {
    new StreamAdapter[FS2Stream, Chunkerator] {
      def adapt[A](stream: Stream[IO, A]): Chunkerator[A] = new Chunkerator[A] {
        def apply = {

          // this promise is completed when the producer takes an action. it either results in the next
          // value (Some(a)), or it signals completion with None
          var produced = Promise[Option[Seq[A]]]()

          // this promise is completed when the consumer consumes the next signal from the producer.
          var consumed = Promise[Unit]()

          // the consumer sets this flag when its user calls close
          var closed = false

          // at the start, the producer hasn't produced anything yet, and the consumer is waiting
          consumed.success(())

          // the consumer
          val iterator = new CloseableChunkIter[A] {
            def hasNext = {
              val oa = Await.result(produced.future, streamadapter.timeout)
              oa.nonEmpty
            }
            def next = {
              val oa = Await.result(produced.future, streamadapter.timeout)
              produced = Promise()
              consumed.success(())
              oa.get
            }
            def close = {
              closed = true
              if (!consumed.isCompleted) consumed.success(())
            }
          }

          import scala.concurrent.Future
          import scala.concurrent.blocking
          implicit val ec = streamadapter.fixedPoolExecutionContext(2)
          Future {
            blocking {
              stream.chunks.takeWhile({ a =>
                if (!closed) {
                  val ou = Await.result(consumed.future, streamadapter.timeout)
                  consumed = Promise()
                  produced.success(Some(a.toVector))
                }
                !closed
              }).compile.drain.map({ t =>
                val ou = Await.result(consumed.future, streamadapter.timeout)
                consumed = Promise()
                produced.success(None)
                t
              }).unsafeRunSync
            }
          }
          ec.shutdown

          iterator
        }
      }
    }
  }

}
