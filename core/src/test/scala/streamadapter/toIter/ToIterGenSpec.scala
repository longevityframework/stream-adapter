package streamadapter.toIter

import org.joda.time.DateTime
import org.specs2.Specification
import streamadapter.IterGen

/** @tparam P the type of the publisher to convert from */
abstract class ToIterGenSpec[P[_]] extends Specification {

  def adapterName: String

  // its important that this method only takes what it needs from the incoming seq
  def create: Seq[Int] => P[Int]

  def createBlocking: Seq[Int] => P[Int]

  def adapt: P[Int] => IterGen[Int]

  def implementsClose: Boolean

  def is = s2"""
$adapterName should
  produce the same elements as the supplied publisher                $sameElts
  produce a result immediately, even if the publisher blocks         $doesntBlock
  produce the same result when run twice                             $reproducible
$closesEarlyFragment"""
  def d2 =s2"""
"""
  def closesEarlyFragment = if (implementsClose) {
    s2"""
  stop querying from source if the downstream publisher closes early $closesEarly
"""
  } else {
    s2"""
"""
  }

  def elements = 0 until 5000

  def sameElts = {
    adapt(create(elements))().toSeq must beEqualTo(elements)
  }

  def doesntBlock = {
    val start = DateTime.now.getMillis
    val iterGen = adapt(createBlocking(elements))
    val iter = iterGen()
    val end = DateTime.now.getMillis
    iter.close
    (end - start) must beLessThan(1000L)
  }

  def reproducible = {
    val u = create(elements)
    adapt(u)().toSeq must beEqualTo (adapt(u)().toSeq)
  }

  def trackingElements = new TrackingElements

  class TrackingElements extends Seq[Int] {
    val elts = elements
    var viewCounts = Map[Int, Int]()
    def apply(i: Int) = {
      viewCounts += i -> (viewCounts.getOrElse(i, 0) + 1)
      elts.apply(i)
    }
    def iterator = throw new UnsupportedOperationException("you aren't supposed to use iterator method in this test framework, just apply")
    def length = elts.length
  }
  
  def closesEarly = {
    val track = trackingElements
    val iterGen = adapt(create(track))
    val iter = iterGen()
    iter.next
    iter.next
    iter.next
    iter.close
    //try Thread.sleep(1000) catch { case t: InterruptedException => }
    track.viewCounts.keys.max must be_>=(2) and be_<=(100) // allow for a certain amount of buffering
  }

}
