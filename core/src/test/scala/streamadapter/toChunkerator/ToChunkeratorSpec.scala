package streamadapter.toChunkerator

import org.joda.time.DateTime
import streamadapter.Chunkerator
import streamadapter.ChunkeratorSpec

/** @tparam P the type of the publisher to convert from */
abstract class ToChunkeratorSpec[P[_]] extends ChunkeratorSpec {

  def create: Chunkerator[Int] => P[Int]

  def adapt: P[Int] => Chunkerator[Int]

  def implementsClose: Boolean

  def is = s2"""
$adapterName should
  produce the same elements as the supplied publisher                $sameElts
  produce a result immediately, even if the publisher blocks         $doesntBlock
  produce the same result when run twice                             $reproducible
$closesEarlyFragment"""

  def closesEarlyFragment = if (implementsClose) {
    s2"""
  stop querying from source if the downstream publisher closes early $closesEarly
"""
  } else {
    s2"""
"""
  }

  def sameElts = adapt(create(chunkerator)).apply.toVector.flatten must beEqualTo(elements.toVector)

  def doesntBlock = {
    val start = DateTime.now.getMillis
    val underTest = adapt(create(blockingChunkerator))
    val iter = underTest()
    val end = DateTime.now.getMillis
    iter.close
    (end - start) must beLessThan(3000L)
  }

  def reproducible = {
    val underTest = adapt(create(chunkerator))
    underTest.apply.toVector.flatten must beEqualTo (underTest.apply.toVector.flatten)
  }

  def closesEarly = {
    val track = trackingChunkerator
    val chunkerator = adapt(create(track))
    val iter = chunkerator()
    iter.next
    iter.next
    iter.next
    iter.close

    try Thread.sleep(3000) catch { case t: java.lang.InterruptedException => }

    // allow for a certain amount of buffering
    { track.viewCounts.keys.max must be_>=(0) and be_<=(50)
    } and {
      // TODO this should pass!
      track.closedIndexes.size must beEqualTo(1)
    } and {
      track.closedIndexes(0) must be_<=(50)
    }
  }

}
