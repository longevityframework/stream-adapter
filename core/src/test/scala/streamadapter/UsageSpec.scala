package usage // intentionally different package here to mimic real life usage

import java.io.Closeable

// TODO rename file (put it in a subdir too)
// TODO: this should probably just be a class that compiles. in that case, i can easily use it for a
// usage example
object Usage extends App {

  val iterGenSeq = 0.until(10)

  val iterGen = { () =>
    new Iterator[Int] with Closeable {
      private val i = iterGenSeq.toIterator
      def hasNext = i.hasNext
      def next = i.next
      def close = while (hasNext) next
    }
  }

  import scala.concurrent._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  println(Await.ready(Future(throw new RuntimeException("D")), 1.second))

  def toFutureSeq = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Future
    import streamadapter._
    import streamadapter.futureseq._
    val futureSeq: Future[Seq[Int]] = adapt0[IterGen, FutureSeq, Int](iterGen)

    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    val seq = Await.result(futureSeq, Duration.Inf)
  }

  def toAkkaSource = {
    import streamadapter._
    import streamadapter.akka._
    val source: AkkaSource[Int] = adapt0[IterGen, AkkaSource, Int](iterGen)
  }

  def toCatsEnum = {
    import streamadapter._
    import streamadapter.cats._
    val enum: EvalEnumerator[Int] = adapt0[IterGen, EvalEnumerator, Int](iterGen)

    // get the publisher like so
    val adapter = implicitly[PublisherAdapter[IterGen, EvalEnumerator]]
  }

}
