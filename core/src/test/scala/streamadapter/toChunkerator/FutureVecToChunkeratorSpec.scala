package streamadapter.toChunkerator

import streamadapter.futurevec.chunkeratorToFutureVec
import streamadapter.futurevec.futureVecToChunkerator
import streamadapter.futurevec.FutureVec

class FutureVecToChunkeratorSpec extends ToChunkeratorSpec[FutureVec] {

  implicit val ec = streamadapter.fixedPoolExecutionContext(20)

  def adapterName = "futureVecToChunkerator"

  def create = chunkeratorToFutureVec.adapt[Int] _

  def adapt = futureVecToChunkerator.adapt[Int] _

  def implementsClose = false
  
}
