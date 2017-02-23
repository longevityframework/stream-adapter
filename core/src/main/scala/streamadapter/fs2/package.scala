package streamadapter

import _root_.fs2.Stream
import _root_.fs2.Task
import java.io.Closeable

package object fs2 {

  // TODO either generalize away from Task, or try to include some Pure or something
  type FS2Stream[A] = Stream[Task, A]

  /** produces a publisher adapter from iterator generator to FS2 stream */
  implicit def iterGenToFS2Stream = {
    new PublisherAdapter[IterGen, FS2Stream] {
      def adaptPublisher[A](iterGen: IterGen[A]): Stream[Task, A] = {
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
          i => Task.delay(i.close))
      }
    }
  }

  // TODO
  /** produces a publisher adapter from FS2 stream to iterator generator */
  implicit def fs2StreamToIterGen = {
    new PublisherAdapter[FS2Stream, IterGen] {
      def adaptPublisher[A](stream: Stream[Task, A]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          def hasNext = ???
          def next    = ???
          def close   = ???
        }
      }
    }
  }

}
