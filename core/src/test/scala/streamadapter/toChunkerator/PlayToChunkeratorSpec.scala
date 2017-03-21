package streamadapter.toChunkerator

import _root_.play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.Chunkerator
import streamadapter.play.chunkeratorToPlayEnumerator
import streamadapter.play.playEnumeratorToChunkerator

class PlayToChunkeratorSpec extends ToChunkeratorSpec[Enumerator] {

  def adapterName = "playEnumeratorToChunkerator"

  def adapt = playEnumeratorToChunkerator.adapt[Int] _

  def create = (c: Chunkerator[Int]) => chunkeratorToPlayEnumerator.adapt(c)

  def implementsClose = true

}
