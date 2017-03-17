package streamadapter

import _root_.akka.NotUsed
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Keep
import _root_.akka.stream.scaladsl.Sink
import _root_.akka.stream.scaladsl.Source
import scala.concurrent.Await
import scala.concurrent.Future

/** contains [[StreamAdapter stream adapters]] for Akka sources */
package object akka {

  /** an Akka source with `NotUsed` for a materialization */
  type AkkaSource[A] = Source[A, NotUsed]

  /** produces a publisher adapter from chunkerator to akka source */
  implicit val chunkeratorToAkkaSource = {
    new StreamAdapter[Chunkerator, AkkaSource] {
      def adapt[A](chunkerator: Chunkerator[A]): Source[A, NotUsed] = {
        Source.unfoldResource[A, CloseableIter[A]](
          () => chunkerator.toIterator,
          iterator => if (iterator.hasNext) Some(iterator.next) else None,
          iterator => iterator.close)
      }
    }
  }

  /** produces a publisher adapter from akka source to chunkerator */
  implicit def akkaSourceToChunkerator(implicit materializer: ActorMaterializer) = {
    new StreamAdapter[AkkaSource, Chunkerator] {
      def adapt[A](source: Source[A, NotUsed]): Chunkerator[A] = { () =>
        new CloseableChunkIter[A] {
          private lazy val queue = source.grouped(10).toMat(Sink.queue())(Keep.right).run()
          private var closed = false
          private var pull: Future[Option[Seq[A]]] = _
          private def preparePull = pull = queue.pull
          private def awaitPull = Await.result(pull, streamadapter.timeout)
          def hasNext = !closed && awaitPull.nonEmpty
          def next = {
            if (closed) throw new NoSuchElementException
            val a = awaitPull.get
            preparePull
            a
          }
          def close = if (!closed) {
            queue.cancel
            closed = true
          }
          preparePull
        }
      }
    }
  }

}
