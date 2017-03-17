package streamadapter.toChunkerator

import java.lang.InterruptedException
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.futurevec.futureVecToChunkerator
import streamadapter.futurevec.FutureVec

class FutureVecToChunkeratorSpec extends ToChunkeratorSpec[FutureVec] {

  def adapterName = "futureVecToChunkerator"

  def create = (seq: Seq[Int]) => Future.successful(seq.toVector)

  def createBlocking = (seq: Seq[Int]) => Future {
    blocking {
      try Thread.sleep(3000) catch { case t: InterruptedException => }
      seq.toVector
    }
  }

  def adapt = futureVecToChunkerator.adapt[Int] _

  def implementsClose = false
  
}
