package streamadapter.toIter

import _root_.play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.CloseableIter
import streamadapter.play.iterGenToPlayEnumerator
import streamadapter.play.playEnumeratorToIterGen

class PlayFromIterGenSpec extends ToIterGenSpec[Enumerator] {

  def adapterName = "playEnumeratorToIterGen"

  def adapt = playEnumeratorToIterGen.adapt[Int] _

  def create = (sequence: Seq[Int]) => {
    def iter = new CloseableIter[Int] {
      private var i = 0
      def hasNext = i < sequence.size
      def next = {
        val n = sequence(i)
        i += 1
        n
      }
      def close = ()
    }
    iterGenToPlayEnumerator.adapt(iter _)
  }

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] {
      private val i = sequence.toIterator
      def hasNext = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.hasNext
      }
      def next = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.next
      }
    }
    Enumerator.enumerate(iter)
  }

  def implementsClose = true

}
