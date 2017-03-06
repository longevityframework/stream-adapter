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
  // QUESTION: it seems that i really need Bimonad here and not just Monad. because i need to
  // call F.extract on the iteratee to get it to "run". does that make sense to you?
  class CatsEnumeratorToIterGen[F[_]](implicit F: Bimonad[F]) {
    type CatsEnumerator[E] = Enumerator[F, E]
    val adapter = new PublisherAdapter[CatsEnumerator, IterGen] {
      def adapt[E](enumerator: Enumerator[F, E]): IterGen[E] = { () =>

        // QUESTION: is there some way that i can do this without the promises? i have a version
        // that works, but performs terribly, that only defines an Iterator with Closeable, and uses
        // methods in the Iteratee companion object to drive it, here:
        //
        // https://github.com/longevityframework/stream-adapter/blob/exp/cats-to-iter-f-extract/core/src/main/scala/streamadapter/cats/package.scala#L63
        //
        // is there some way i can rework this alternate version so it performs?

        // TODO can i do this without promises?
        import scala.concurrent.Await
        import scala.concurrent.Promise
        import scala.concurrent.duration.Duration

        // TODO add some chunking here for performance

        // this promise is completed when the producer takes an action. it either results in the next
        // value (Some(a)), or it signals completion with None
        var produced = Promise[Option[E]]()

        // this promise is completed when the consumer consumes the next signal from the producer.
        // it either results in a signal to keep going (Some(())), or a signal to close (None)
        var consumed = Promise[Option[Unit]]()

        // at the start, the producer hasn't produced anything yet, and the consumer is waiting
        consumed.success(Some(()))

        // the consumer
        val iterator = new Iterator[E] with Closeable {
          def hasNext = {
            val oa = Await.result(produced.future, Duration.Inf)
            oa.nonEmpty
          }
          def next = {
            val oa = Await.result(produced.future, Duration.Inf)
            produced = Promise()
            consumed.success(Some(()))
            oa.get
          }
          def close = {
            consumed = Promise()
            consumed.success(None)
          }
        }

        val iteratee: Iteratee[F, E, Unit] = Iteratee.fold[F, E, Unit](())(
          { case (a, e) =>
            val ou = Await.result(consumed.future, Duration.Inf)
            consumed = Promise()
            produced.success(Some(e))
            a
          }).map({ a =>
            val ou = Await.result(consumed.future, Duration.Inf)
            consumed = Promise()
            produced.success(None)
            a
          })

        // TODO get rid of future

        // QUESTION: i would really prefer to not use a future here, but it seems necessary that i
        // spawn off a second thread in this scenario. I really want to try to limit the
        // dependencies needed to make this converter work to just cats and iteratee.io, but it
        // seems neither provides a Task. is there a better option that brings in maybe a tiny
        // dependency? i am thinking that i would prefer to use Future over bringing in any extra
        // dependency. what do you think?
        import scala.concurrent.Future
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.blocking
        Future(blocking(
          F.extract(iteratee(enumerator).run)
        ))
        iterator
      }
    }
  }

}
