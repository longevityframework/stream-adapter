package streamadapter.fromChunkerator

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Keep
import _root_.akka.stream.scaladsl.Sink
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.akka.AkkaSource
import streamadapter.akka.chunkeratorToAkkaSource

object AkkaFromChunkeratorSpec {

  implicit val materializer = ActorMaterializer()(ActorSystem("unblocking"))

}

import AkkaFromChunkeratorSpec.materializer

class AkkaFromChunkeratorSpec extends FromChunkeratorSpec[AkkaSource] {

  def adapterName = "chunkeratorToAkkaSource"

  def adapt = chunkeratorToAkkaSource.adapt[Int]

  def toIterator: AkkaSource[Int] => Iterator[Int] = (as) => {
    val f = as.toMat(Sink.seq[Int])(Keep.right).run().map(_.toIterator)
    Await.result(f, streamadapter.timeout)
  }

  def takeThirtyThreeOpt: Option[AkkaSource[Int] => AkkaSource[Int]] = Some(_.take(33))

}
