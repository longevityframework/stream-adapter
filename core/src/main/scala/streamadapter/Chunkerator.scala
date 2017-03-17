package streamadapter

/** a [[CloseableChunkIter closeable chunk iterator]] generator. whenever the function is called, it
 * produces an iterator for the same underlying resource. this is necessary for the enumerators to
 * be reusable, ie, produce the same stream of elements when used with two iteratees, and not
 * produce an empty stream when used a second time. note, however, that the iterators returned by
 * this generating function do not have to produce the exact same results. allowance is made for
 * the fact that the underlying resource may be a changeable resource. for instance, we could be
 * iterating over the contents of a file, and some other process might modify the file in the
 * meantime.
 *
 * at this point, Chunkerator serves as our common interchange format for publishers - we will try
 * to get to and from conversions for this type, from each stream type we handle. we can then
 * convert between any two publisher types using this one as a mediator.
 */
trait Chunkerator[+A] extends Function0[CloseableChunkIter[A]] {
  self =>

  /** produces a closeable iterator over the elements in the chunks */
  def toIterator = new CloseableIter[A] {
    private val chunker = self.apply
    private var chunk = List[A]()
    def hasNext = chunk.nonEmpty || chunker.hasNext
    def next = if (chunk.nonEmpty) {
      val a = chunk.head
      chunk = chunk.tail
      a
    } else {
      chunk = chunker.next.toList
      next
    }
    def close = chunker.close
  }

  /** produces a vector of the elements in the chunks */
  def toVector = {
    val chunker = self.apply
    var buffer = new scala.collection.immutable.VectorBuilder[A]
    while (chunker.hasNext) buffer ++= chunker.next
    buffer.result
  }

}

/** contains factory methods for producing chunkerators */
object Chunkerator {

  /** creates a chunkerator by grouping a sequence
   * @tparam A the type of the elements in the sequence
   * @param n the size of the groups
   * @param sequence the sequence to group
   */
  def grouped[A](n: Int, sequence: Seq[A]) = new Chunkerator[A] {
    def apply = CloseableChunkIter.grouped(n, sequence)
  }
  
}
