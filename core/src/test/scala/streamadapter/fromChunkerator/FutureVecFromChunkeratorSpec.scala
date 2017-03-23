package streamadapter.fromChunkerator

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.futurevec.chunkeratorToFutureVec

object FutureVecFromChunkeratorSpec {

  type FutureVec[A] = Future[Vector[A]]

}

class FutureVecFromChunkeratorSpec extends FromChunkeratorSpec[FutureVecFromChunkeratorSpec.FutureVec] {

  def adapterName = "chunkeratorToFutureVec"

  def adapt = chunkeratorToFutureVec.adapt[Int] _

  def toIterator = (fs) => Await.result(fs, streamadapter.timeout).toIterator

  // no way to take three here, unless we change toFutureVec to take eg an until: A => Boolean
  def takeThirtyThreeOpt = None
  
}
