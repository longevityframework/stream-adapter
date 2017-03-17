package streamadapter.fromChunkerator

import streamadapter.CloseableChunkIter
import java.lang.InterruptedException
import org.joda.time.DateTime
import org.specs2.Specification
import streamadapter.Chunkerator

/** @tparam P the type of the publisher to convert to */
abstract class FromChunkeratorSpec[P[_]] extends Specification {
  
  def adapterName: String

  def adapt: Chunkerator[Int] => P[Int]

  def toIterator: P[Int] => Iterator[Int]

  def takeThirtyThreeOpt: Option[(P[Int]) => P[Int]]

  def is = s2"""
$adapterName should
  produce the same elements as upstream                 $sameElts
  produce a result immediately, even if upstream blocks $doesntBlock
  produce the same result when run twice                $reproducible
  produce an exception thrown by upstream               $threadsThrowables
$takeThirtyThreeFragment"""

  def takeThirtyThreeFragment = takeThirtyThreeOpt match {
    case Some(takeThirtyThree) => s2"""
  produce an abbreviated result if the publisher is closed early ${threeElts(takeThirtyThree)}
  close the iterator if the publisher is closed early            ${closesEarly(takeThirtyThree)}
"""
    case None => s2"""
"""
  }

  def elements = 0 until 5000

  class IIGen extends Chunkerator[Int] {
    var index = 0
    var closedIndexes: Seq[Int] = Seq()
    class II extends CloseableChunkIter[Int] {
      val i = elements.grouped(10)
      def hasNext = i.hasNext
      def next = {
        index += 1
        i.next
      }
      def close = closedIndexes :+= index
    }
    def apply = new II
  }

  class BlockingIIGen extends IIGen {
    class BlockingII extends II {
      private def sleep = try Thread.sleep(1000) catch { case t: InterruptedException => }
      override def hasNext = {
        sleep
        super.hasNext
      }
      override def next = {
        sleep
        super.next
      }
      override def close = {
        sleep
        super.close
      }
    }
    override def apply = new BlockingII
  }

  class ThrowingIGenException extends RuntimeException

  class ThrowingIGen extends Chunkerator[Int] {
    class TI extends CloseableChunkIter[Int] {
      val i = elements.grouped(10)
      var index = 0
      def hasNext = {
        if (index == elements.size / 10 - 2) throw new ThrowingIGenException
        i.hasNext
      }
      def next = {
        index += 1
        i.next
      }
      def close = ()
    }
    def apply = new TI
  }

  def chunkerator = new IIGen

  def blockingChunkerator = new BlockingIIGen

  def throwingChunkerator = new ThrowingIGen

  def sameElts = toIterator(adapt(chunkerator)) must beEqualTo(elements)

  def doesntBlock = {
    val start = DateTime.now.getMillis
    val in = adapt(blockingChunkerator)
    val end = DateTime.now.getMillis
    (end - start) must beLessThan(1000L)
  }

  def reproducible = {
    val u = adapt(chunkerator)
    toIterator(u).toList must beEqualTo(toIterator(u).toList)
  }

  def threadsThrowables = {
    toIterator(adapt(throwingChunkerator)).toList must throwA [ThrowingIGenException]
  }

  def threeElts(takeThirtyThree: (P[Int]) => P[Int]) = {
    toIterator(takeThirtyThree(adapt(chunkerator))) must beEqualTo(elements.take(33))
  }

  def closesEarly(takeThirtyThree: (P[Int]) => P[Int]) = {
    val iGen = chunkerator
    toIterator(takeThirtyThree(adapt(iGen)))

    { iGen.closedIndexes.size must beEqualTo(1)
    } and {
      // need a range here since smart streams are going to buffer
      iGen.closedIndexes(0) must be_>=(3) and be_<=(100)
    }
  }

}
