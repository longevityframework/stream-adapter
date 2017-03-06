package streamadapter

import _root_.fs2.Strategy
import _root_.fs2.Stream
import _root_.fs2.Task
import java.io.Closeable

package object fs2 {

  // TODO either generalize away from Task, or try to include some Pure or something
  type FS2Stream[A] = Stream[Task, A]

  /** produces a publisher adapter from iterator generator to FS2 stream */
  implicit def iterGenToFS2Stream = {
    new PublisherAdapter[IterGen, FS2Stream] {
      def adapt[A](iterGen: IterGen[A]): Stream[Task, A] = {
        def iteratorToStream(i: Iterator[A]): Stream[Task, A] = {
          if (i.hasNext) {
            // TODO: implement chunking here
            Stream.emit(i.next) ++ iteratorToStream(i)
          } else {
            Stream.empty[Task, A]
          }
        }
        Stream.bracket[Task, CloseableIter[A], A](
          Task.delay(iterGen()))(
          iteratorToStream,
          i => Task.now(i.close))
      }
    }
  }

  /** produces a publisher adapter from FS2 stream to iterator generator */
  implicit def fs2StreamToIterGen(implicit S: Strategy) = {
    new PublisherAdapter[FS2Stream, IterGen] {
      def adapt[A](stream: Stream[Task, A]): IterGen[A] = { () =>

        // TODO can i do this without promises?
        import scala.concurrent.Await
        import scala.concurrent.Promise
        import scala.concurrent.duration.Duration

        // TODO add some chunking here for performance

        // this promise is completed when the producer takes an action. it either results in the next
        // value (Some(a)), or it signals completion with None
        var produced = Promise[Option[A]]()

        // this promise is completed when the consumer consumes the next signal from the producer.
        // it either results in a signal to keep going (Some(())), or a signal to close (None)
        var consumed = Promise[Option[Unit]]()

        // at the start, the producer hasn't produced anything yet, and the consumer is waiting
        consumed.success(Some(()))

        // the consumer
        val iterator = new Iterator[A] with Closeable {
          def hasNext = {
            val oa = Await.result(produced.future, Duration.Inf)
            oa.nonEmpty
          }
          def next = {
            val oa = Await.result(produced.future, Duration.Inf)
            produced = Promise()
            consumed.success(Some(()))
            oa.get
          }
          def close = {
            Await.result(produced.future, Duration.Inf)
            produced = Promise()
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
            stream.takeWhile({ a =>
              val ou = Await.result(consumed.future, Duration.Inf)
              consumed = Promise()
              produced.success(Some(a))
              ou.nonEmpty
            }).run.map({ t =>
              val ou = Await.result(consumed.future, Duration.Inf)
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
