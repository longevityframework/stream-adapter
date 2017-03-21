package streamadapter.fromChunkerator

import org.joda.time.DateTime
import streamadapter.Chunkerator
import streamadapter.ChunkeratorSpec

/** @tparam P the type of the publisher to convert to */
abstract class FromChunkeratorSpec[P[_]] extends ChunkeratorSpec {

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
  produce an abbreviated result if the publisher is closed early ${thirtyThreeElts(takeThirtyThree)}
  close the iterator if the publisher is closed early            ${closesEarly(takeThirtyThree)}
"""
    case None => s2"""
"""
  }

  def sameElts = toIterator(adapt(chunkerator)).toList must beEqualTo(elements.toList)

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
    toIterator(adapt(throwingChunkerator)).toList must throwA [ThrowingChunkeratorException]
  }

  def thirtyThreeElts(takeThirtyThree: (P[Int]) => P[Int]) = {
    toIterator(takeThirtyThree(adapt(chunkerator))).toList must beEqualTo(elements.take(33).toList)
  }

  def closesEarly(takeThirtyThree: (P[Int]) => P[Int]) = {
    val track = trackingChunkerator
    toIterator(takeThirtyThree(adapt(track)))

    // allow for a certain amount of buffering
    { track.viewCounts.keys.max must be_>=(0) and be_<=(50)
    } and {
      track.closedIndexes.size must beEqualTo(1)
    } and {
      track.closedIndexes(0) must be_<=(10)
    }
  }

}
