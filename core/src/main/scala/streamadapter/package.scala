import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** TODO */
package object streamadapter {

  /** a closeable iterator. has method `close: Unit`, along with the standard `Iterator` methods, so
   * that enumerator can be closed early when the iteratee is complete.
   */
  trait CloseableIter[+A] extends Iterator[A] {

    /** closes this stream and releases any system resources associated with it */
    def close: Unit
  }

  /** a [[CloseableIter closeable iterator]] generator. whenever the function is called, it
   * produces an iterator for the same underlying resource. this is necessary the enumerators to be
   * reusable, i.e., produce the same stream of elements when used with two iteratees, and not
   * produce an empty stream when used a second time. note, however, that the iterators returned by
   * this generating function do not have to produce the exact same results. allowance is made for
   * the fact that the underlying resource may be a changeable resource. for instance, we could be
   * iterating over the contents of a file, and some other process might modify the file in the
   * meantime.
   *
   * at this point, IterGen serves as our common interchange format for publishers - we will try
   * to get to and from conversions for this type, from each stream type we handle. we can then
   * convert between any two publisher types using this one as a mediator.
   *
   * TODO tparams
   */
  type IterGen[+A] = () => CloseableIter[A]

  /** TODO */
  def adapt0[P1[_], P2[_], A](
    p1: P1[A])(
    implicit publisherAdapter: PublisherAdapter[P1, P2]): P2[A] =
    publisherAdapter.adapt(p1)

  private[streamadapter] def trampoline[A](f: => Future[A])(implicit context: ExecutionContext): Future[A] =
    Future(()).flatMap { _ => f }

}
