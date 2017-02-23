package streamadapter

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.futureseq.iterGenToFutureSeq

object FutureSeqFromIterGenSpec {

  type FutureSeq[A] = Future[Seq[A]]

}

class FutureSeqFromIterGenSpec extends FromIterGenSpec[FutureSeqFromIterGenSpec.FutureSeq] {

  def adapterName = "iterGenToFutureSeq"

  def adaptPublisher = iterGenToFutureSeq.adaptPublisher[Int] _

  def toIterator: Future[Seq[Int]] => Iterator[Int] = (fs) => Await.result(fs, Duration.Inf).toIterator

  // no way to take three here, unless we change toFutureSeq to take eg an until: A => Boolean
  def takeThreeOpt = None
  
}
