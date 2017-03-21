package streamadapter.toChunkerator

import scala.concurrent.ExecutionContext.Implicits.global
import streamadapter.futurevec.chunkeratorToFutureVec
import streamadapter.futurevec.futureVecToChunkerator
import streamadapter.futurevec.FutureVec

class FutureVecToChunkeratorSpec extends ToChunkeratorSpec[FutureVec] {

  def adapterName = "futureVecToChunkerator"

  def create = chunkeratorToFutureVec.adapt[Int] _

  def adapt = futureVecToChunkerator.adapt[Int] _

  def implementsClose = false
  
}
