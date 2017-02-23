package usage // intentionally different package here to mimic real life usage

import java.io.Closeable
import org.specs2.Specification

/** @tparam P the type of the publisher to convert to */
class UsageSpec extends Specification {

  def is = s2"""
streamadapter.adaptPublisher should
  adapt an iter gen to a future seq painlessly     $toFutureSeq
  adapt an iter gen to an akka source painlessly   $toAkkaSource
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

  // first test with no PublisherAndEffect
  def toFutureSeq = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import streamadapter._
    import streamadapter.futureseq._
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    val futureSeq = adaptPublisher[NoEffect, EffectiveIterGen, NoEffect, EffectiveFutureSeq, Int](iterGen)
    val seq = Await.result(futureSeq, Duration.Inf)
    seq must beEqualTo (iterGenSeq)
  }

  // first test with no PublisherAndEffect
  def toAkkaSource = {
    import streamadapter._
    import streamadapter.akka._
    val source = adaptPublisher[NoEffect, EffectiveIterGen, NoEffect, EffectiveSource, Int](iterGen)
    0 must beEqualTo(0)
  }


  //import streamadapter.adaptPublisher0

//   def sameElts = toIterator(adaptPublisher(iterGen)) must beEqualTo(elements)

//   def doesntBlock = {
//     val start = DateTime.now.getMillis
//     val in = adaptPublisher(blockingIterGen)
//     val end = DateTime.now.getMillis
//     (end - start) must beLessThan(1000L)
//   }

//   def reproducible: org.specs2.execute.Result = {
//     val u = adaptPublisher(iterGen)
//     toIterator(u).toList must beEqualTo(toIterator(u).toList)
//   }

//   def threeElts(takeThree: (P[Int]) => P[Int]) = {
//     toIterator(takeThree(adaptPublisher(iterGen))) must beEqualTo(elements.take(3))
//   }

//   def closesEarly(takeThree: (P[Int]) => P[Int]) = {
//     val iGen = iterGen
//     toIterator(takeThree(adaptPublisher(iGen)))

//     { iGen.closedIndexes.size must beEqualTo(1)
//     } and {
//       // need a range here since smart streams are going to buffer
//       iGen.closedIndexes(0) must be_>=(3) and be_<=(100)
//     }
//   }

}
