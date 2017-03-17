package streamadapter.fromChunkerator

import _root_.play.api.libs.iteratee.Enumeratee
import _root_.play.api.libs.iteratee.Enumerator
import _root_.play.api.libs.iteratee.Iteratee
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.play.chunkeratorToPlayEnumerator

class PlayFromChunkeratorSpec extends FromChunkeratorSpec[Enumerator] {

  def adapterName = "chunkeratorToPlayEnumerator"

  def adapt = chunkeratorToPlayEnumerator.adapt

  def toIterator: Enumerator[Int] => Iterator[Int] = { (enumerator) =>
    val iteratee = Iteratee.fold[Int, Vector[Int]](Vector())(_ :+ _)
    val f = enumerator.run(iteratee)
    Await.result(f, streamadapter.timeout).toIterator
  }

  def takeThirtyThreeOpt = Some(_.through(Enumeratee.take(33)))

}
