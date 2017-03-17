package streamadapter.toChunkerator

import _root_.cats.Eval
import streamadapter.Chunkerator
import streamadapter.CloseableChunkIter
import streamadapter.cats.EvalEnumerator
import streamadapter.cats.chunkeratorToCatsEnumerator
import streamadapter.cats.catsEnumeratorToChunkerator

class CatsToChunkeratorSpec extends ToChunkeratorSpec[EvalEnumerator] {

  def adapterName = "catsEnumeratorToChunkerator"

  def adapt = catsEnumeratorToChunkerator[Eval].adapt[Int] _

  def create = (seq: Seq[Int]) => chunkeratorToCatsEnumerator[Eval].adapt(Chunkerator.grouped(10, seq))

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new CloseableChunkIter[Int] {
      private val chunks = sequence.grouped(10)
      def hasNext = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        chunks.hasNext
      }
      def next = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        chunks.next
      }
      def close = ()
    }
    chunkeratorToCatsEnumerator[Eval].adapt(iter _)
  }

  def implementsClose = true

}
