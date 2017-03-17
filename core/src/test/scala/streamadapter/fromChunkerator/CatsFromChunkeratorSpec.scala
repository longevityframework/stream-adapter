package streamadapter.fromChunkerator

import _root_.cats.Eval
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import scala.collection.mutable.ArrayBuffer
import streamadapter.cats.chunkeratorToCatsEnumerator

object CatsFromChunkeratorSpec {

  type CatsEnumerator[A] = Enumerator[Eval, A]

}

import CatsFromChunkeratorSpec.CatsEnumerator

class CatsFromChunkeratorSpec extends FromChunkeratorSpec[CatsEnumerator] {

  def adapterName = "chunkeratorToCatsEnumerator"

  def adapt = chunkeratorToCatsEnumerator[Eval].adapt[Int]

  def toIterator: CatsEnumerator[Int] => Iterator[Int] = { (enumerator) =>
    val builder = ArrayBuffer.newBuilder[Int]
    val it = Iteratee.foreach[Eval, Int] { builder += _ }
    it(enumerator).run.value
    builder.result.toIterator
  }

  def takeThirtyThreeOpt: Option[Enumerator[Eval, Int] => Enumerator[Eval, Int]] = Some(_.take(33))

}
