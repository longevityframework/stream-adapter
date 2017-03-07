package streamadapter.fromIter

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Keep
import _root_.akka.stream.scaladsl.Sink
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import streamadapter.akka.AkkaSource
import streamadapter.akka.iterGenToAkkaSource

object AkkaFromIterGenSpec {

  implicit val materializer = ActorMaterializer()(ActorSystem("unblocking"))

}

import AkkaFromIterGenSpec.materializer

class AkkaFromIterGenSpec extends FromIterGenSpec[AkkaSource] {

  def adapterName = "iterGenToAkkaSource"

  def adapt = iterGenToAkkaSource.adapt[Int]

  def toIterator: AkkaSource[Int] => Iterator[Int] = (as) => {
    val f = as.toMat(Sink.seq[Int])(Keep.right).run().map(_.toIterator)
    Await.result(f, Duration.Inf)
  }

  def takeThreeOpt: Option[AkkaSource[Int] => AkkaSource[Int]] = Some(_.take(3))

}
