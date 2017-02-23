package streamadapter.fromIter

import _root_.fs2.Stream
import _root_.fs2.Task
import streamadapter.fs2.iterGenToFS2Stream

object FS2FromIterGenSpec {

  type S[A] = Stream[Task, A]

}

import FS2FromIterGenSpec.S

class FS2FromIterGenSpec extends FromIterGenSpec[S] {

  def adapterName = "iterGenToFS2Stream"

  def adapt = iterGenToFS2Stream.adapt

  def toIterator: S[Int] => Iterator[Int] = _.runLog.unsafeRun().toIterator

  def takeThreeOpt: Option[S[Int] => S[Int]] = Some(_.take(3))

}
