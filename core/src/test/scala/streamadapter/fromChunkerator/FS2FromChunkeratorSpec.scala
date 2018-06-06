package streamadapter.fromChunkerator

import _root_.fs2.Stream
import cats.effect.IO
import streamadapter.fs2.chunkeratorToFS2Stream

object FS2FromChunkeratorSpec {

  type S[A] = Stream[IO, A]

}

import FS2FromChunkeratorSpec.S

class FS2FromChunkeratorSpec extends FromChunkeratorSpec[S] {

  def adapterName = "chunkeratorToFS2Stream"

  def adapt = chunkeratorToFS2Stream.adapt

  def toIterator: S[Int] => Iterator[Int] = _.compile.toVector.unsafeRunSync().toIterator

  def takeThirtyThreeOpt: Option[S[Int] => S[Int]] = Some(_.take(33))

}
