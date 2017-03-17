package streamadapter

/** a closeable iterator of chunks. has method `close: Unit`, along with the standard `Iterator`
 * methods, so that enumerator can be closed early when the iteratee is complete.
 */
trait CloseableChunkIter[+A] extends CloseableIter[Seq[A]]

object CloseableChunkIter {

  /** TODO */
  def apply[A](iterator: Iterator[Seq[A]]) = new CloseableChunkIter[A] {
    def hasNext = iterator.hasNext
    def next = iterator.next
    def close = ()
  }

  /** TODO */
  def grouped[A](n: Int, sequence: Seq[A]) = apply(sequence.grouped(n))

}
