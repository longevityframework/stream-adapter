package streamadapter.fromIter

import java.io.Closeable
import java.lang.InterruptedException
import org.joda.time.DateTime
import org.specs2.Specification
import streamadapter.IterGen

/** @tparam P the type of the publisher to convert to */
abstract class FromIterGenSpec[P[_]] extends Specification {
  
  def adapterName: String

  def adapt: IterGen[Int] => P[Int]

  def toIterator: P[Int] => Iterator[Int]

  def takeThreeOpt: Option[(P[Int]) => P[Int]]

  def is = s2"""
$adapterName should
  produce the same elements as the supplied iterator            $sameElts
  produce a result immediately, even if the iterator blocks     $doesntBlock
  produce the same result when run twice                        $reproducible
$takeThreeFragment"""

  def takeThreeFragment = takeThreeOpt match {
    case Some(takeThree) => s2"""
  produce an abbreviated result if the producer is closed early ${threeElts(takeThree)}
  close the iterator if the producer is closed early            ${closesEarly(takeThree)}
"""
    case None => s2"""
"""
  }

  def elements = 0 until 5000

  class IIGen extends IterGen[Int] {
    var index = 0
    var closedIndexes: Seq[Int] = Seq()
    class II extends Iterator[Int] with Closeable {
      val i = elements.iterator
      def hasNext: Boolean = i.hasNext
      def next(): Int = {
        index += 1
        i.next
      }
      def close(): Unit = closedIndexes :+= index
    }
    def apply = new II
  }

  class BlockingIIGen extends IIGen {
    class BlockingII extends II {
      private def sleep = try Thread.sleep(1000) catch { case t: InterruptedException => }
      override def hasNext: Boolean = {
        sleep
        super.hasNext
      }
      override def next(): Int = {
        sleep
        super.next()
      }
      override def close(): Unit = {
        sleep
        super.close()
      }
    }
    override def apply = new BlockingII
  }

  def iterGen = new IIGen

  def blockingIterGen = new BlockingIIGen

  def sameElts = toIterator(adapt(iterGen)) must beEqualTo(elements)

  def doesntBlock = {
    val start = DateTime.now.getMillis
    val in = adapt(blockingIterGen)
    val end = DateTime.now.getMillis
    (end - start) must beLessThan(1000L)
  }

  def reproducible: org.specs2.execute.Result = {
    val u = adapt(iterGen)
    toIterator(u).toList must beEqualTo(toIterator(u).toList)
  }

  def threeElts(takeThree: (P[Int]) => P[Int]) = {
    toIterator(takeThree(adapt(iterGen))) must beEqualTo(elements.take(3))
  }

  def closesEarly(takeThree: (P[Int]) => P[Int]) = {
    val iGen = iterGen
    toIterator(takeThree(adapt(iGen)))

    { iGen.closedIndexes.size must beEqualTo(1)
    } and {
      // need a range here since smart streams are going to buffer
      iGen.closedIndexes(0) must be_>=(3) and be_<=(100)
    }
  }

}
