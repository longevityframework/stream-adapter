package streamadapter.toChunkerator

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
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

  def create = chunkeratorToAkkaSource.adapt[Int] _

  def implementsClose = true

}
