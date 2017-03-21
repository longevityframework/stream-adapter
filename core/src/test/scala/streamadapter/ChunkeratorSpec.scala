package streamadapter

import java.lang.InterruptedException
import org.specs2.Specification

/** @tparam P the type of the publisher to convert to */
abstract class ChunkeratorSpec extends Specification {

  def adapterName: String

  def elements = 0 until 5000

  class CloseTrackerChunkerator extends Chunkerator[Int] {
    var index = 0
    var viewCounts = Map[Int, Int]()
    var closedIndexes: Seq[Int] = Seq()
    class CloseTracker extends CloseableChunkIter[Int] {
      val i = elements.grouped(10)
      def hasNext = {
        viewCounts += index -> (viewCounts.getOrElse(index, 0) + 1)
        i.hasNext
      }
      def next = {
        viewCounts += index -> (viewCounts.getOrElse(index, 0) + 1)
        index += 1
        i.next
      }
      def close = closedIndexes :+= index
    }
    def apply = new CloseTracker
  }

  class BlockingChunkerator extends CloseTrackerChunkerator {
    class BlockingIter extends CloseTracker {
      private def sleep = try Thread.sleep(1000) catch { case t: InterruptedException => }
      override def hasNext = {
        sleep
        super.hasNext
      }
      override def next = {
        sleep
        super.next
      }
      override def close = {
        sleep
        super.close
      }
    }
    override def apply = new BlockingIter
  }

  class ThrowingChunkeratorException extends RuntimeException

  class ThrowingChunkerator extends Chunkerator[Int] {
    class TI extends CloseableChunkIter[Int] {
      val i = elements.grouped(10)
      var index = 0
      def hasNext = {
        if (index == elements.size / 10 - 2) throw new ThrowingChunkeratorException
        i.hasNext
      }
      def next = {
        index += 1
        i.next
      }
      def close = ()
    }
    def apply = new TI
  }

  def chunkerator = Chunkerator.grouped(10, elements)

  def trackingChunkerator = new CloseTrackerChunkerator

  def blockingChunkerator = new BlockingChunkerator

  def throwingChunkerator = new ThrowingChunkerator

}
