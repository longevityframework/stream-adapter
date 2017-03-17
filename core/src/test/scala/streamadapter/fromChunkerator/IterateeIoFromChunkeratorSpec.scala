package streamadapter.fromChunkerator

import cats.Eval
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import scala.collection.mutable.ArrayBuffer
import streamadapter.iterateeio.EvalEnumerator
import streamadapter.iterateeio.chunkeratorToIterateeIoEnumerator

class IterateeIoFromChunkeratorSpec extends FromChunkeratorSpec[EvalEnumerator] {

  def adapterName = "chunkeratorToIterateeIoEnumerator"

  def adapt = chunkeratorToIterateeIoEnumerator[Eval].adapt[Int]

  def toIterator: EvalEnumerator[Int] => Iterator[Int] = { (enumerator) =>
    val builder = ArrayBuffer.newBuilder[Int]
    val it = Iteratee.foreach[Eval, Int] { builder += _ }
    it(enumerator).run.value
    builder.result.toIterator
  }

  def takeThirtyThreeOpt: Option[Enumerator[Eval, Int] => Enumerator[Eval, Int]] = Some(_.take(33))

}
