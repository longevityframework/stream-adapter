package streamadapter.toIter

import _root_.cats.Eval
import streamadapter.CloseableIter
import streamadapter.cats.EvalEnumerator
import streamadapter.cats.iterGenToCatsEnumerator
import streamadapter.cats.catsEnumeratorToIterGen

// TODO this file is misnamed
class CatsToIterGenSpec extends ToIterGenSpec[EvalEnumerator] {

  def adapterName = "catsEnumeratorToIterGen"

  def adapt = catsEnumeratorToIterGen[Eval].adapt[Int] _

  def create = (sequence: Seq[Int]) => {
    def iter = new CloseableIter[Int] {
      private var i = 0
      def hasNext = {
        i < sequence.size
      }
      def next = {
        val n = sequence(i)
        i += 1
        n
      }
      def close = ()
    }
    iterGenToCatsEnumerator[Eval].adapt(iter _)
  }

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new CloseableIter[Int] {
      private val i = sequence.toIterator
      def hasNext = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.hasNext
      }
      def next = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.next
      }
      def close = ()
    }
    iterGenToCatsEnumerator[Eval].adapt(iter _)
  }

  def implementsClose = true

}
