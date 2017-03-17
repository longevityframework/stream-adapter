package streamadapter.fromChunkerator

import _root_.fs2.Stream
import _root_.fs2.Task
import streamadapter.fs2.chunkeratorToFS2Stream

object FS2FromChunkeratorSpec {

  type S[A] = Stream[Task, A]

}

import FS2FromChunkeratorSpec.S

class FS2FromChunkeratorSpec extends FromChunkeratorSpec[S] {

  def adapterName = "chunkeratorToFS2Stream"

  def adapt = chunkeratorToFS2Stream.adapt

  def toIterator: S[Int] => Iterator[Int] = _.runLog.unsafeRun().toIterator

  def takeThirtyThreeOpt: Option[S[Int] => S[Int]] = Some(_.take(33))

}
