package streamadapter.toChunkerator

import _root_.play.api.libs.iteratee.Enumerator
import org.specs2.specification.AfterAll
import streamadapter.Chunkerator
import streamadapter.play.chunkeratorToPlayEnumerator
import streamadapter.play.playEnumeratorToChunkerator

class PlayToChunkeratorSpec extends ToChunkeratorSpec[Enumerator] with AfterAll {

  implicit val ec = streamadapter.fixedPoolExecutionContext(20)

  def adapterName = "playEnumeratorToChunkerator"

  def adapt = playEnumeratorToChunkerator.adapt[Int] _

  def create = (c: Chunkerator[Int]) => chunkeratorToPlayEnumerator.adapt(c)

  def implementsClose = true

  def afterAll = ec.shutdown

}
