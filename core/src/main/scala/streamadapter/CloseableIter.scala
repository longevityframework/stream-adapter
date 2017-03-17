package streamadapter

/** a closeable iterator. has method `close: Unit`, along with the standard `Iterator` methods, so
 * that enumerator can be closed early when the iteratee is complete.
 */
trait CloseableIter[+A] extends Iterator[A] {

  /** closes this stream and releases any system resources associated with it */
  def close: Unit

}
