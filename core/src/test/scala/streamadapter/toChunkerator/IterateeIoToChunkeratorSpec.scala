package streamadapter.toChunkerator

import cats.Eval
import streamadapter.iterateeio.EvalEnumerator
import streamadapter.iterateeio.chunkeratorToIterateeIoEnumerator
import streamadapter.iterateeio.iterateeIoEnumeratorToChunkerator

class IterateeIoToChunkeratorSpec extends ToChunkeratorSpec[EvalEnumerator] {

  def adapterName = "iterateeIoEnumeratorToChunkerator"

  def adapt = iterateeIoEnumeratorToChunkerator[Eval].adapt[Int] _

  def create = chunkeratorToIterateeIoEnumerator[Eval].adapt[Int] _

  def implementsClose = true

}
