package streamadapter.toChunkerator

import streamadapter.fs2.fs2StreamToChunkerator
import streamadapter.fs2.chunkeratorToFS2Stream
import streamadapter.fs2.FS2Stream

class FS2ToChunkeratorSpec extends ToChunkeratorSpec[FS2Stream] {

  implicit val S = _root_.fs2.Strategy.fromFixedDaemonPool(8, threadName = "worker")

  def adapterName = "fs2StreamToChunkerator"

  def adapt = fs2StreamToChunkerator.adapt[Int] _

  def create = chunkeratorToFS2Stream.adapt[Int] _

  def implementsClose = true

}
