package streamadapter.toIter

import java.io.Closeable
import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import streamadapter.akka.AkkaSource
import streamadapter.akka.akkaSourceToIterGen
import streamadapter.akka.iterGenToAkkaSource

object AkkaToIterGenSpec {

  implicit val materializer = ActorMaterializer()(ActorSystem("streamadapter"))

}

import AkkaToIterGenSpec.materializer

class AkkaToIterGenSpec extends ToIterGenSpec[AkkaSource] {

  def adapterName = "akkaSourceToIterGen"

  def adapt = akkaSourceToIterGen.adapt[Int] _

  def create = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] with Closeable {
      var i = 0
      def hasNext = i < sequence.size
      def next = {
        val n = sequence(i)
        i += 1
        n
      }
      def close = ()
    }
    iterGenToAkkaSource.adapt(iter _)
  }

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] with Closeable {
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
    iterGenToAkkaSource.adapt(iter _)
  }

  def implementsClose = true

}
