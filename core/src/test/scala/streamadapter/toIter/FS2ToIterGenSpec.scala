package streamadapter.toIter

import java.io.Closeable
import streamadapter.fs2.fs2StreamToIterGen
import streamadapter.fs2.iterGenToFS2Stream
import streamadapter.fs2.FS2Stream

// TODO this file is misnamed
class FS2ToIterGenSpec extends ToIterGenSpec[FS2Stream] {

  implicit val S = _root_.fs2.Strategy.fromFixedDaemonPool(8, threadName = "worker")

  def adapterName = "fs2StreamToIterGen"

  def adapt = fs2StreamToIterGen.adapt[Int] _

  def create = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] with Closeable {
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
    iterGenToFS2Stream.adapt(iter _)
  }

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] with Closeable {
      private val i = sequence.toIterator
      private var closed = false
      def hasNext = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.hasNext && !closed
      }
      def next = {
        try Thread.sleep(1000) catch { case t: InterruptedException => }
        i.next
      }
      def close = {
        closed = true
      }
    }
    iterGenToFS2Stream.adapt(iter _)
  }

  def implementsClose = true

}
