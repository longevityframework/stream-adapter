package streamadapter.toChunkerator

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import streamadapter.CloseableChunkIter
import streamadapter.Chunkerator
import streamadapter.akka.AkkaSource
import streamadapter.akka.akkaSourceToChunkerator
import streamadapter.akka.chunkeratorToAkkaSource

object AkkaToChunkeratorSpec {

  implicit val actorSystem = ActorSystem("streamadapter",
    classLoader = Some(classOf[Chunkerator[_]].getClassLoader))

  implicit val materializer = ActorMaterializer()

}

import AkkaToChunkeratorSpec.materializer

class AkkaToChunkeratorSpec extends ToChunkeratorSpec[AkkaSource] {

  def adapterName = "akkaSourceToChunkerator"

  def adapt = akkaSourceToChunkerator.adapt[Int] _

  def create = (seq: Seq[Int]) => chunkeratorToAkkaSource.adapt(Chunkerator.grouped(10, seq))

  def createBlocking = (sequence: Seq[Int]) => {
    def iter = new CloseableChunkIter[Int] {
      private val i = sequence.grouped(10)
      def hasNext = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        i.hasNext
      }
      def next = {
        try Thread.sleep(3000) catch { case t: InterruptedException => }
        i.next
      }
      def close = ()
    }
    chunkeratorToAkkaSource.adapt(iter _)
  }

  def implementsClose = true

}
