package streamadapter

import _root_.fs2.Strategy
import _root_.fs2.Stream
import _root_.fs2.Task
import scala.concurrent.Await
import scala.concurrent.Promise

/** contains [[StreamAdapter stream adapters]] for FS2 streams */
package object fs2 {

  /** an FS2 stream bound to a `Task` effect */
  type FS2Stream[A] = Stream[Task, A]

  /** produces a publisher adapter from chunkerator to FS2 stream */
  implicit def chunkeratorToFS2Stream = {
    new StreamAdapter[Chunkerator, FS2Stream] {
      def adapt[A](chunkerator: Chunkerator[A]): Stream[Task, A] = {
        def iterToStream(i: CloseableChunkIter[A]): Stream[Task, A] = {
          if (i.hasNext) {
            Stream.emits(i.next) ++ iterToStream(i)
          } else {
            Stream.empty[Task, A]
          }
        }
        Stream.bracket[Task, CloseableChunkIter[A], A](
          Task.delay(chunkerator()))(
          iterToStream,
          i => Task.now(i.close))
      }
    }
  }

  /** produces a publisher adapter from FS2 stream to chunkerator */
  implicit def fs2StreamToChunkerator(implicit S: Strategy) = {
    new StreamAdapter[FS2Stream, Chunkerator] {
      def adapt[A](stream: Stream[Task, A]): Chunkerator[A] = { () =>

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

        // QUESTION: i would really prefer to not use a future here, but it seems necessary that i
        // spawn off a second thread in this scenario. I really want to try to limit the
        // dependencies needed to make this converter work to just cats and iteratee.io, but it
        // seems neither provides a Task. is there a better option that brings in maybe a tiny
        // dependency? i am thinking that i would prefer to use Future over bringing in any extra
        // dependency. what do you think?

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
            }).run.map({ t =>
              val ou = Await.result(consumed.future, streamadapter.timeout)
              consumed = Promise()
              produced.success(None)
              t
            }).unsafeRun
          }
        }
        ec.shutdown

        iterator
      }
    }
  }

}
