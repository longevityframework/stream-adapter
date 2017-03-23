package streamadapter.fromChunkerator

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import _root_.akka.stream.scaladsl.Keep
import _root_.akka.stream.scaladsl.Sink
import org.specs2.specification.AfterAll
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.akka.AkkaSource
import streamadapter.akka.chunkeratorToAkkaSource

class AkkaFromChunkeratorSpec extends FromChunkeratorSpec[AkkaSource] with AfterAll {

  implicit val actorSystem = ActorSystem("streamadapter",
    classLoader = Some(classOf[streamadapter.Chunkerator[_]].getClassLoader))

  implicit val materializer = ActorMaterializer()

  def adapterName = "chunkeratorToAkkaSource"

  def adapt = chunkeratorToAkkaSource.adapt[Int]

  def toIterator: AkkaSource[Int] => Iterator[Int] = (as) => {
    val f = as.toMat(Sink.seq[Int])(Keep.right).run().map(_.toIterator)
    Await.result(f, streamadapter.timeout)
  }

  def takeThirtyThreeOpt: Option[AkkaSource[Int] => AkkaSource[Int]] = Some(_.take(33))

  def afterAll = actorSystem.terminate

}
