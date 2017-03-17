package streamadapter.toChunkerator

import cats.Eval
import streamadapter.Chunkerator
import streamadapter.CloseableChunkIter
import streamadapter.iterateeio.EvalEnumerator
import streamadapter.iterateeio.chunkeratorToIterateeIoEnumerator
import streamadapter.iterateeio.iterateeIoEnumeratorToChunkerator

class IterateeIoToChunkeratorSpec extends ToChunkeratorSpec[EvalEnumerator] {

  def adapterName = "iterateeIoEnumeratorToChunkerator"

  def adapt = iterateeIoEnumeratorToChunkerator[Eval].adapt[Int] _

  def create = (seq: Seq[Int]) => chunkeratorToIterateeIoEnumerator[Eval].adapt(Chunkerator.grouped(10, seq))

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
    chunkeratorToIterateeIoEnumerator[Eval].adapt(iter _)
  }

  def implementsClose = true

}
