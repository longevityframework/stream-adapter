package streamadapter

import cats.instances.future._
import cats.Eval
import cats.Id
import cats.Monad
import cats.Bimonad
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.iteratee.Enumerator
import io.iteratee.Iteratee
import io.iteratee.internal.Step

/** contains [[StreamAdapter stream adapters]] for `iteratee.io` enumerators */
package object iterateeio {

  /** an iteratee.io enumerator bound to an `Eval` effect */
  type EvalEnumerator[E] = Enumerator[Eval, E]

  /** an iteratee.io enumerator bound to an `Id` effect */
  type IdEnumerator[E] = Enumerator[Id, E]

  /** produces a publisher adapter from chunkerator to an iteratee.io enumerator */
  implicit def chunkeratorToIterateeIoEnumerator[F[_]](implicit F: Monad[F]) =
    new ChunkeratorToIterateeIoEnumerator(F).adapter

  /** contains an adapter wrapped with the type we are adapting to. exposing the `IterateeIoEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  class ChunkeratorToIterateeIoEnumerator[F[_]](F: Monad[F]) {
    type IterateeIoEnumerator[E] = Enumerator[F, E]
    val adapter = new StreamAdapter[Chunkerator, IterateeIoEnumerator] {
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

  /** produces a publisher adapter from an iteratee.io enumerator to chunkerator */
  implicit def iterateeIoEnumeratorToChunkerator[F[_]](implicit F: Bimonad[F]) =
    new IterateeIoEnumeratorToChunkerator()(F).adapter

  /** contains an adapter wrapped with the type we are adapting from. exposing the `IterateeIoEnumerator` type
   * within this class in a non-anonymous way helps resolve some problems the compiler has equating
   * the type returned by the adapter with the expected type.
   */
  // QUESTION: it seems that i really need Bimonad here and not just Monad. because i need to
  // call F.extract on the iteratee to get it to "run". does that make sense to you?
  class IterateeIoEnumeratorToChunkerator[F[_]](implicit F: Bimonad[F]) {
    type IterateeIoEnumerator[E] = Enumerator[F, E]
    val adapter = new StreamAdapter[IterateeIoEnumerator, Chunkerator] {
      def adapt[E](enumerator: Enumerator[F, E]): Chunkerator[E] = { () =>

        // QUESTION: is there some way that i can do this without the promises? i have a version
        // that works, but performs terribly, that only defines a CloseableIter, and uses
        // methods in the Iteratee companion object to drive it, here:
        //
        // https://github.com/longevityframework/stream-adapter/blob/exp/cats-to-iter-f-extract/core/src/main/scala/streamadapter/cats/package.scala#L63
        //
        // is there some way i can rework this alternate version so it performs?

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
        class FCCI {
          def hasNext: Future[Boolean] = produced.future.map(_.nonEmpty)
          def next: Future[Seq[E]] = produced.future.map { es =>
            produced = Promise()
            consumed.success(Some(()))
            es.get
          }
          def close: Future[Unit] = Future.successful(())
        }

        // the producer
        val iteratee: Iteratee[Future, Vector[E], Unit] = Iteratee.foreach[Future, Vector[E]](
          { es =>
            val ou = Await.result(consumed.future, streamadapter.timeout)
            consumed = Promise()
            produced.success(Some(es))
          }).map({ a =>
            val ou = Await.result(consumed.future, streamadapter.timeout)
            consumed = Promise()
            produced.success(None)
            a
          })

        // QUESTION: i would really prefer to not use a future here, but it seems necessary that i
        // spawn off a second thread in this scenario. I really want to try to limit the
        // dependencies needed to make this converter work to just cats and iteratee.io, but it
        // seems neither provides a Task. is there a better option that brings in maybe a tiny
        // dependency? i am thinking that i would prefer to use Future over bringing in any extra
        // dependency. what do you think?
        import scala.concurrent.blocking
        val i2 = iteratee.mapI[F](
          new cats.arrow.FunctionK[Future, F] {
            final def apply[A](fa: Future[A]): F[A] = F.pure(Await.result(fa, streamadapter.timeout))
          }
        )
        Future(blocking(
          F.extract(i2(enumerator.grouped(10)).run)
        ))

        val iterator = new CloseableChunkIter[E] {
          val fcci = new FCCI
          def hasNext = Await.result(fcci.hasNext, streamadapter.timeout)
          def next = Await.result(fcci.next, streamadapter.timeout)
          def close = Await.result(fcci.close, streamadapter.timeout)
        }

        iterator
      }
    }
  }

}
