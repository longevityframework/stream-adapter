package streamadapter.toChunkerator

import _root_.play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.Chunkerator
import streamadapter.play.chunkeratorToPlayEnumerator
import streamadapter.play.playEnumeratorToChunkerator

class PlayToChunkeratorSpec extends ToChunkeratorSpec[Enumerator] {

  def adapterName = "playEnumeratorToChunkerator"

  def adapt = playEnumeratorToChunkerator.adapt[Int] _

  def create = (seq: Seq[Int]) => chunkeratorToPlayEnumerator.adapt(Chunkerator.grouped(10, seq.toVector))

  def createBlocking = (seq: Seq[Int]) => {
    def iter = new Iterator[Int] {
      private val i = seq.toIterator
      def hasNext = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        i.hasNext
      }
      def next = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        i.next
      }
    }
    Enumerator.enumerate(iter)
  }

  def implementsClose = true

}
