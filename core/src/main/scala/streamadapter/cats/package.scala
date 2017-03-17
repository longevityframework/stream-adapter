package streamadapter

import _root_.cats.Eval
import _root_.cats.Id
import _root_.cats.Monad
import _root_.cats.Bimonad
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import io.iteratee.internal.Step

/** TODO */
package object cats {

  /** TODO */
  type EvalEnumerator[E] = Enumerator[Eval, E]

  /** TODO */
  type IdEnumerator[E] = Enumerator[Id, E]

  /** produces a publisher adapter from chunkerator to a cats-style `io.iteratee` enumerator */
  implicit def chunkeratorToCatsEnumerator[F[_]](implicit F: Monad[F]) =
    new ChunkeratorToCatsEnumerator(F).adapter

  /** contains an adapter wrapped with the type we are adapting to. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  class ChunkeratorToCatsEnumerator[F[_]](F: Monad[F]) {
    type CatsEnumerator[E] = Enumerator[F, E]
    val adapter = new PublisherAdapter[Chunkerator, CatsEnumerator] {
      def adapt[E](chunkerator: Chunkerator[E]): Enumerator[F, E] = {
        new Enumerator[F, E] {
          final def apply[A](step: Step[F, E, A]): F[Step[F, E, A]] = {
            val iterator = chunkerator()
            def applyInternal[A](step: Step[F, E, A]): F[Step[F, E, A]] = {
              if (!step.isDone && iterator.hasNext) {
                val es = iterator.next
                F.flatMap(step.feed(es))(s => applyInternal[A](s))
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

  /** produces a publisher adapter from a cats-style `io.iteratee` enumerator to chunkerator
   * TODO params
   */
  implicit def catsEnumeratorToChunkerator[F[_]](implicit F: Bimonad[F]) =
    new CatsEnumeratorToChunkerator()(F).adapter

  /** contains an adapter wrapped with the type we are adapting from. exposing the `CatsEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   * TODO params
   */
  // QUESTION: it seems that i really need Bimonad here and not just Monad. because i need to
  // call F.extract on the iteratee to get it to "run". does that make sense to you?
  class CatsEnumeratorToChunkerator[F[_]](implicit F: Bimonad[F]) {
    type CatsEnumerator[E] = Enumerator[F, E]
    val adapter = new PublisherAdapter[CatsEnumerator, Chunkerator] {
      def adapt[E](enumerator: Enumerator[F, E]): Chunkerator[E] = { () =>

        // QUESTION: is there some way that i can do this without the promises? i have a version
        // that works, but performs terribly, that only defines a CloseableIter, and uses
        // methods in the Iteratee companion object to drive it, here:
        //
        // https://github.com/longevityframework/stream-adapter/blob/exp/cats-to-iter-f-extract/core/src/main/scala/streamadapter/cats/package.scala#L63
        //
        // is there some way i can rework this alternate version so it performs?

        // TODO can i do this without promises?
        import scala.concurrent.Await
        import scala.concurrent.Promise

        // this promise is completed when the producer takes an action. it either results in the next
        // value (Some(a)), or it signals completion with None
        var produced = Promise[Option[Seq[E]]]()

        // this promise is completed when the consumer consumes the next signal from the producer.
        // it either results in a signal to keep going (Some(())), or a signal to close (None)
        var consumed = Promise[Option[Unit]]()

        // at the start, the producer hasn't produced anything yet, and the consumer is waiting
        consumed.success(Some(()))

        // the consumer
        val iterator = new CloseableChunkIter[E] {
          def hasNext = {
            val oa = Await.result(produced.future, streamadapter.timeout)
            oa.nonEmpty
          }
          def next = {
            val oa = Await.result(produced.future, streamadapter.timeout)
            produced = Promise()
            consumed.success(Some(()))
            oa.get
          }
          def close = {
            consumed = Promise()
            consumed.success(None)
          }
        }

        val iteratee: Iteratee[F, Vector[E], Unit] = Iteratee.foreach[F, Vector[E]](
          { e =>
            val ou = Await.result(consumed.future, streamadapter.timeout)
            consumed = Promise()
            produced.success(Some(e))
          }).map({ a =>
            val ou = Await.result(consumed.future, streamadapter.timeout)
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
          F.extract(iteratee(enumerator.grouped(10)).run)
        ))
        iterator
      }
    }
  }

}
