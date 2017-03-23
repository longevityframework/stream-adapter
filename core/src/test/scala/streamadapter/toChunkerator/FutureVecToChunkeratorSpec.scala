package streamadapter.toChunkerator

import org.specs2.specification.AfterAll
import streamadapter.futurevec.chunkeratorToFutureVec
import streamadapter.futurevec.futureVecToChunkerator
import streamadapter.futurevec.FutureVec

class FutureVecToChunkeratorSpec extends ToChunkeratorSpec[FutureVec] with AfterAll {

  implicit val ec = streamadapter.fixedPoolExecutionContext(20)

  def adapterName = "futureVecToChunkerator"

  def create = chunkeratorToFutureVec.adapt[Int] _

  def adapt = futureVecToChunkerator.adapt[Int] _

  def implementsClose = false

  def afterAll = ec.shutdown
  
}
