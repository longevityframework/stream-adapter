package streamadapter.toChunkerator

import org.joda.time.DateTime
import org.specs2.Specification
import streamadapter.Chunkerator

/** @tparam P the type of the publisher to convert from */
abstract class ToChunkeratorSpec[P[_]] extends Specification {

  def adapterName: String

  // its important that this method only takes what it needs from the incoming seq
  def create: Seq[Int] => P[Int]

  def createBlocking: Seq[Int] => P[Int]

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

  def elements = (0 until 5000).toVector

  def sameElts = adapt(create(elements)).apply.toVector.flatten must beEqualTo(elements.toVector)

  def doesntBlock = {
    val start = DateTime.now.getMillis
    val chunkerator = adapt(createBlocking(elements))
    val iter = chunkerator()
    val end = DateTime.now.getMillis
    iter.close
    (end - start) must beLessThan(3000L)
  }

  def reproducible = {
    val chunkerator = adapt(create(elements))
    chunkerator.apply.toVector.flatten must beEqualTo (chunkerator.apply.toVector.flatten)
  }

  def trackingElements = new TrackingElements

  class TrackingElements extends Seq[Int] {
    val elts = elements
    var viewCounts = Map[Int, Int]()
    def apply(i: Int) = {
      viewCounts += i -> (viewCounts.getOrElse(i, 0) + 1)
      elts(i)
    }
    def iterator = new Iterator[Int] {
      var i = 0
      def hasNext = {
        viewCounts += i -> (viewCounts.getOrElse(i, 0) + 1)
        i < elts.size
      }
      def next = {
        viewCounts += i -> (viewCounts.getOrElse(i, 0) + 1)
        val a = elts(i)
        i += 1
        a
      }
    }
    def length = elts.length
  }
  
  def closesEarly = {
    val track = trackingElements
    val chunkerator = adapt(create(track))
    val iter = chunkerator()
    iter.next
    iter.next
    iter.next
    iter.close
    track.viewCounts.keys.max must be_>=(2) and be_<=(100) // allow for a certain amount of buffering
  }

}
