package streamadapter.toIter

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Source
import streamadapter.akka.AkkaSource
import streamadapter.akka.akkaSourceToIterGen

object AkkaFromIterGenSpec {

  implicit val materializer = ActorMaterializer()(ActorSystem("unblocking"))

}

import AkkaFromIterGenSpec.materializer

class AkkaFromIterGenSpec extends ToIterGenSpec[AkkaSource] {

  def adapterName = "akkaSourceToIterGen"

  def adapt = akkaSourceToIterGen.adapt[Int] _

  def create = (sequence: Seq[Int]) => {
    def iter = new Iterator[Int] {
      var i = 0
      def hasNext = i < sequence.size
      def next = {
        val n = sequence(i)
        i += 1
        n
      }
    }
    Source.fromIterator(iter _)
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
    Source.fromIterator(iter _)
  }

  def implementsClose = true

}
