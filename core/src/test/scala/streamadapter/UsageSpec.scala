package usage // intentionally different package here to mimic real life usage

import java.io.Closeable
import org.specs2.Specification

// TODO: this should probably just be a class that compiles. in that case, i can easily use it for a
// usage example
/** @tparam P the type of the publisher to convert to */
class UsageSpec extends Specification {

  def is = s2"""
streamadapter.adaptPublisher should
  adapt an iter gen to a future seq painlessly     $toFutureSeq
  adapt an iter gen to an akka source painlessly   $toAkkaSource
  adapt an iter gen to a cats enumerator painlessly   $toAkkaSource
"""

  val iterGenSeq = 0.until(10)

  val iterGen = { () =>
    new Iterator[Int] with Closeable {
      private val i = iterGenSeq.toIterator
      def hasNext = i.hasNext
      def next = i.next
      def close = while (hasNext) next
    }
  }

  def toFutureSeq = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import streamadapter._
    import streamadapter.futureseq._
    val futureSeq = adaptPublisher[IterGen, FutureSeq, Int](iterGen)

    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    val seq = Await.result(futureSeq, Duration.Inf)
    seq must beEqualTo (iterGenSeq)
  }

  def toAkkaSource = {
    import streamadapter._
    import streamadapter.akka._
    val source: AkkaSource[Int] = adaptPublisher[IterGen, AkkaSource, Int](iterGen)
    success
  }

  def toCatsEnum = {
    import streamadapter._
    import streamadapter.cats._
    val enum: EvalEnumerator[Int] = adaptPublisher[IterGen, EvalEnumerator, Int](iterGen)
    success
  }

}
