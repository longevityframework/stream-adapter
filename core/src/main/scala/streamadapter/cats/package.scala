package streamadapter

import _root_.cats.Eval
import _root_.cats.Monad
import io.iteratee.Enumerator
import io.iteratee.internal.Step
import java.io.Closeable

/** TODO */
package object cats {

  /** TODO */
  type EvalEnumerator[A] = Enumerator[Eval, A]

  /** produces a publisher adapter from iterator generator to a cats-style `io.iteratee` enumerator */
  implicit def iterGenToCatsEnumerator[F[_]](implicit F: Monad[F]) =
    new IterGenToCatsEnumerator(F).adapter

  /** contains an adapter wrapped with the type we are adapting to. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  class IterGenToCatsEnumerator[F[_]](F: Monad[F]) {
    type CatsEnumerator[A] = Enumerator[F, A]
    val adapter = new PublisherAdapter[IterGen, CatsEnumerator] {
      def adaptPublisher[E](iterGen: IterGen[E]): Enumerator[F, E] = {
        new Enumerator[F, E] {
          final def apply[A](step: Step[F, E, A]): F[Step[F, E, A]] = {
            val iterator = iterGen()
            def applyInternal[A](step: Step[F, E, A]): F[Step[F, E, A]] = {
              if (!step.isDone && iterator.hasNext) {
                // TODO: implement chunking here
                F.flatMap(step.feedEl(iterator.next))(s => applyInternal[A](s))
              } else {
                iterator.close
                F.pure(step)
              }
            }
            applyInternal(step)
          }
        }
      }
    }
  }

  /** produces a publisher adapter from a cats-style `io.iteratee` enumerator to iterator generator */
  implicit def catsEnumeratorToIterGen[F[_]](implicit F: Monad[F]) =
    new CatsEnumeratorToIterGen(F).adapter

  /** contains an adapter wrapped with the type we are adapting from. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  class CatsEnumeratorToIterGen[F[_]](F: Monad[F]) {
    type CatsEnumerator[A] = Enumerator[F, A]
    // TODO
    val adapter = new PublisherAdapter[CatsEnumerator, IterGen] {
      def adaptPublisher[A](enumerator: Enumerator[F, A]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          def hasNext = ???
          def next    = ???
          def close   = ???
        }
      }
    }
  }

}
