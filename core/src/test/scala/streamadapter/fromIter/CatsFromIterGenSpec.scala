package streamadapter

import _root_.cats.Eval
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import scala.collection.mutable.ArrayBuffer
import streamadapter.cats.iterGenToCatsEnumerator

object CatsFromIterGenSpec {

  type CatsEnumerator[A] = Enumerator[Eval, A]

}

import CatsFromIterGenSpec.CatsEnumerator

class CatsFromIterGenSpec extends FromIterGenSpec[CatsEnumerator] {

  def adapterName = "iterGenToCatsEnumerator"

  def adaptPublisher = iterGenToCatsEnumerator[Eval].adaptPublisher[Int]

  def toIterator: CatsEnumerator[Int] => Iterator[Int] = { (enumerator) =>
    val builder = ArrayBuffer.newBuilder[Int]
    val it = Iteratee.foreach[Eval, Int] { builder += _ }
    it(enumerator).run.value
    builder.result.toIterator
  }

  def takeThreeOpt: Option[Enumerator[Eval, Int] => Enumerator[Eval, Int]] = Some(_.take(3))

}
