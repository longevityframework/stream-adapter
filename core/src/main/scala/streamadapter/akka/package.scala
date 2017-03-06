package streamadapter

import _root_.akka.NotUsed
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Keep
import _root_.akka.stream.scaladsl.Sink
import _root_.akka.stream.scaladsl.Source
import java.io.Closeable
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** TODO */
package object akka {

  /** TODO */
  type AkkaSource[A] = Source[A, NotUsed]

  /** produces a publisher adapter from iterator generator to akka source */
  implicit val iterGenToAkkaSource = {
    new PublisherAdapter[IterGen, AkkaSource] {
      def adapt[A](iterGen: IterGen[A]): Source[A, NotUsed] = {
        Source.unfoldResource[A, CloseableIter[A]](
          iterGen,
          iterator => if (iterator.hasNext) Some(iterator.next) else None,
          iterator => iterator.close)
      }
    }
  }

  /** produces a publisher adapter from akka source to iterator generator */
  implicit def akkaSourceToIterGen(implicit materializer: ActorMaterializer) = {
    new PublisherAdapter[AkkaSource, IterGen] {
      def adapt[A](source: Source[A, NotUsed]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          private val queue = source.toMat(Sink.queue[A]())(Keep.right).run()
          private var closed = false
          private var pull: Future[Option[A]] = _
          private def preparePull = pull = queue.pull
          private def awaitPull = Await.result(pull, Duration.Inf)
          def hasNext = !closed && awaitPull.nonEmpty
          def next = {
            if (closed) throw new NoSuchElementException
            val a = awaitPull.get
            preparePull
            a
          }
          def close = if (!closed) {
            println("akkaSourceToIterGen close")
            queue.cancel
            closed = true
          }
          preparePull
        }
      }
    }
  }

}
