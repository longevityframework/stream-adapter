package streamadapter

import _root_.cats.Eval
import _root_.cats.Id
import _root_.cats.Monad
import _root_.cats.Bimonad
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import io.iteratee.internal.Step
import java.io.Closeable

/** TODO */
package object cats {

  /** TODO */
  type EvalEnumerator[E] = Enumerator[Eval, E]

  /** TODO */
  type IdEnumerator[E] = Enumerator[Id, E]

  /** produces a publisher adapter from iterator generator to a cats-style `io.iteratee` enumerator */
  implicit def iterGenToCatsEnumerator[F[_]](implicit F: Monad[F]) =
    new IterGenToCatsEnumerator(F).adapter

  /** contains an adapter wrapped with the type we are adapting to. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  class IterGenToCatsEnumerator[F[_]](F: Monad[F]) {
    type CatsEnumerator[E] = Enumerator[F, E]
    val adapter = new PublisherAdapter[IterGen, CatsEnumerator] {
      def adapt[E](iterGen: IterGen[E]): Enumerator[F, E] = {
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

  /** produces a publisher adapter from a cats-style `io.iteratee` enumerator to iterator generator
   * TODO params
   */
  implicit def catsEnumeratorToIterGen[F[_]](implicit F: Bimonad[F]) =
    new CatsEnumeratorToIterGen()(F).adapter

  /** contains an adapter wrapped with the type we are adapting from. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   * TODO params
   */
  class CatsEnumeratorToIterGen[F[_]](implicit F: Bimonad[F]) {
    type CatsEnumerator[E] = Enumerator[F, E]
    val adapter = new PublisherAdapter[CatsEnumerator, IterGen] {
      def adapt[E](enumerator: Enumerator[F, E]): IterGen[E] = { () =>
        var enum = enumerator
        new Iterator[E] with Closeable {
          def hasNext = {
            println("hasNext")
            !F.extract(Iteratee.isEnd[F, E](F)(enum).run)
          }
          def next = {
            println("next")
            val e = F.extract(Iteratee.head[F, E](F)(enum).run).get
            enum = enum.drop(1)
            e
          }
          def close = {
            Iteratee.done[F, E, Unit](())(F)(enum).run
          }
        }
      }
    }
  }

}
