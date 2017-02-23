package streamadapter

import _root_.akka.NotUsed
import _root_.akka.stream.scaladsl.Source
import java.io.Closeable

/** TODO */
package object akka {

  /** TODO */
  type AkkaSource[A] = Source[A, NotUsed]

  /** produces a publisher adapter from iterator generator to akka source */
  implicit val iterGenToAkkaSource = {
    new PublisherAdapter[IterGen, AkkaSource] {
      def adaptPublisher[A](iterGen: IterGen[A]): Source[A, NotUsed] = {
        Source.unfoldResource[A, CloseableIter[A]](
          iterGen,
          iterator => if (iterator.hasNext) Some(iterator.next) else None,
          iterator => iterator.close)
      }
    }
  }

  // TODO
  /** produces a publisher adapter from akka source to iterator generator */
  implicit def akkaSourceToIterGen = {
    new PublisherAdapter[AkkaSource, IterGen] {
      def adaptPublisher[A](akkaSource: Source[A, NotUsed]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          def hasNext = ???
          def next    = ???
          def close   = ???
        }
      }
    }
  }

}
