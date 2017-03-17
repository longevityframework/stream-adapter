package streamadapter.toChunkerator

import streamadapter.Chunkerator
import streamadapter.CloseableChunkIter
import streamadapter.fs2.fs2StreamToChunkerator
import streamadapter.fs2.chunkeratorToFS2Stream
import streamadapter.fs2.FS2Stream

class FS2ToChunkeratorSpec extends ToChunkeratorSpec[FS2Stream] {

  implicit val S = _root_.fs2.Strategy.fromFixedDaemonPool(8, threadName = "worker")

  def adapterName = "fs2StreamToChunkerator"

  def adapt = fs2StreamToChunkerator.adapt[Int] _

  def create = (seq: Seq[Int]) => chunkeratorToFS2Stream.adapt(Chunkerator.grouped(10, seq))

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
    chunkeratorToFS2Stream.adapt(iter _)
  }

  def implementsClose = true

}
