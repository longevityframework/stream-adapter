package streamadapter.toIter

import java.lang.InterruptedException
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.futureseq.futureSeqToIterGen
import streamadapter.futureseq.FutureSeq

class FutureSeqToIterGenSpec extends ToIterGenSpec[FutureSeq] {

  def adapterName = "futureSeqToIterGen"

  def create = (seq: Seq[Int]) => Future(seq)

  def createBlocking = (seq: Seq[Int]) => Future {
    try Thread.sleep(1000) catch { case t: InterruptedException => }
    seq
  }

  def adapt = futureSeqToIterGen.adapt[Int] _

  def implementsClose = false
  
}
