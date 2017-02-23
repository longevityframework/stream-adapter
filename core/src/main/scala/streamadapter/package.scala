import java.io.Closeable

/** TODO */
package object streamadapter {

  /** a closeable iterator. has method `close: Unit`, along with the standard `Iterator` methods, so
   * that enumerator can be closed early when the iteratee is complete.
   *
   * TODO tparams
   */
  type CloseableIter[+A] = Iterator[A] with Closeable

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
  type EffectiveIterGen[F[_], +A] = IterGen[A]

  /** TODO */
  def adaptPublisher[F1[_], P1[F[_], _], F2[_], P2[F[_], _], A](
    p1: P1[F1, A])(
    implicit publisherAdapter: PublisherAdapter[F1, P1, F2, P2]): P2[F2, A] =
    publisherAdapter.adaptPublisher(p1)

  /** TODO */
  type NoEffect[A] = Unit

}
