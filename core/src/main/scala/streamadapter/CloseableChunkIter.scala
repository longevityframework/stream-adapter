package streamadapter

/** a closeable iterator of chunks. has method `close: Unit`, along with the standard `Iterator`
 * methods, so that enumerator can be closed early when the iteratee is complete.
 */
trait CloseableChunkIter[+A] extends CloseableIter[Seq[A]]

/** contains factory methods for producing closeable chunk iters */
object CloseableChunkIter {

  /** creates a closeable chunk iter out of an iterator of chunks */
  def apply[A](iterator: Iterator[Seq[A]]) = new CloseableChunkIter[A] {
    def hasNext = iterator.hasNext
    def next = iterator.next
    def close = ()
  }

  /** creates a closeable chunk iter by grouping a sequence
   * @tparam A the type of the elements in the sequence
   * @param n the size of the groups
   * @param sequence the sequence to group
   */
  def grouped[A](n: Int, sequence: Seq[A]) = apply(sequence.grouped(n))

}
