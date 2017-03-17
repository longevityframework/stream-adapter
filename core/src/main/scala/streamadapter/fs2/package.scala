package streamadapter

import _root_.fs2.Strategy
import _root_.fs2.Stream
import _root_.fs2.Task

package object fs2 {

  // TODO generalize away from Task
  type FS2Stream[A] = Stream[Task, A]

  /** produces a publisher adapter from chunkerator to FS2 stream */
  implicit def chunkeratorToFS2Stream = {
    new PublisherAdapter[Chunkerator, FS2Stream] {
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
    new PublisherAdapter[FS2Stream, Chunkerator] {
      def adapt[A](stream: Stream[Task, A]): Chunkerator[A] = { () =>

        // TODO can i do this without promises?
        import scala.concurrent.Await
        import scala.concurrent.Promise

        // TODO add some chunking here for performance

        // this promise is completed when the producer takes an action. it either results in the next
        // value (Some(a)), or it signals completion with None
        var produced = Promise[Option[Seq[A]]]()

        // this promise is completed when the consumer consumes the next signal from the producer.
        // it either results in a signal to keep going (Some(())), or a signal to close (None)
        var consumed = Promise[Option[Unit]]()

        // at the start, the producer hasn't produced anything yet, and the consumer is waiting
        consumed.success(Some(()))

        // the consumer
        val iterator = new CloseableChunkIter[A] {
          def hasNext = {
            val oa = Await.result(produced.future, streamadapter.timeout)
            oa.nonEmpty
          }
          def next = {
            val oa = Await.result(produced.future, streamadapter.timeout)
            produced = Promise()
            consumed.success(Some(()))
            oa.get
          }
          def close = {
            consumed = Promise()
            consumed.success(None)
          }
        }

        // QUESTION: i would really prefer to not use a future here, but it seems necessary that i
        // spawn off a second thread in this scenario. I really want to try to limit the
        // dependencies needed to make this converter work to just cats and iteratee.io, but it
        // seems neither provides a Task. is there a better option that brings in maybe a tiny
        // dependency? i am thinking that i would prefer to use Future over bringing in any extra
        // dependency. what do you think?

        import scala.concurrent.Future
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.blocking
        Future {
          blocking {
            stream.chunks.takeWhile({ a =>
              val ou = Await.result(consumed.future, streamadapter.timeout)
              consumed = Promise()
              produced.success(Some(a.toVector))
              ou.nonEmpty
            }).run.map({ t =>
              val ou = Await.result(consumed.future, streamadapter.timeout)
              consumed = Promise()
              produced.success(None)
              t
            }).unsafeRun
          }
        }

        iterator
      }
    }
  }

}
