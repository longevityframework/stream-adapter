package streamadapter

import _root_.play.api.libs.iteratee.Enumerator
import _root_.play.api.libs.iteratee.Input
import _root_.play.api.libs.iteratee.Iteratee
import _root_.play.api.libs.iteratee.Step
import _root_.play.api.libs.iteratee.Step.Cont
import _root_.play.api.libs.iteratee.Step.Done
import _root_.play.api.libs.iteratee.Step.Error
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.blocking

/** TODO */
package object play {

  /** an alias for `play.api.libs.iteratee.Enumerator` */
  type PlayEnumerator[A] = Enumerator[A]

  /** produces a publisher adapter from chunkerator to akka source */
  implicit def chunkeratorToPlayEnumerator(implicit context: ExecutionContext) = {
    new PublisherAdapter[Chunkerator, Enumerator] {
      def adapt[A](chunkerator: Chunkerator[A]): Enumerator[A] = {
        new Enumerator[A] {
          def apply[B](iteratee: Iteratee[A, B]): Future[Iteratee[A, B]] = {
            // TODO chunking
            val iterator = chunkerator.toIterator
            def applyInternal[B](iteratee: Iteratee[A, B]): Future[Iteratee[A, B]] =
              iteratee.fold[Iteratee[A, B]] {
                _ match {
                  case Done(result, remaining) =>
                    Future {
                      blocking {
                        iterator.close
                        iteratee
                      }
                    }
                  case Cont(k: (Input[A] => Iteratee[A, B])) =>
                    if (iterator.hasNext) {
                      // force a trampoline here to prevent stack overflows
                      trampoline(applyInternal(k(Input.El(iterator.next))))
                    } else {
                      Future.successful(k(Input.EOF))
                    }
                  case Error(message, input) =>
                    Future {
                      blocking {
                        iterator.close
                        iteratee
                      }
                    }
                }
              }
            applyInternal(iteratee)
          }
        }
      }
    }
  }

  /** produces a publisher adapter from akka source to chunkerator */
  implicit def playEnumeratorToChunkerator(implicit context: ExecutionContext) = {
    new PublisherAdapter[Enumerator, Chunkerator] {
      def adapt[A](enumerator: Enumerator[A]): Chunkerator[A] = { () =>

        // this promise is completed when the producer takes an action. it either results in the next
        // value (Some(a)), or it signals completion with None
        var produced = Promise[Option[Vector[A]]]()

        // this promise is completed when the consumer consumes the next signal from the producer.
        // it either results in a signal to keep going (Some(())), or a signal to close (None)
        var consumed = Promise[Option[Unit]]()

        // at the start, the producer hasn't produced anything yet, and the consumer is waiting
        consumed.success(Some(()))

        // the consumer
        val iterator = new CloseableChunkIter[A] {
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

        // the producer
        val iteratee = new Iteratee[A, Unit] {
          def done = Done[Unit, A]((), Input.EOF).it
          def fold[B](folder: Step[A, Unit] => Future[B])(implicit ec: ExecutionContext): Future[B] = {
            folder {
              val ou = Await.result(consumed.future, streamadapter.timeout)
              Step.Cont {
                case Input.EOF =>
                  consumed = Promise()
                  produced.success(None)
                  done
                case Input.Empty =>
                  if (ou.isEmpty) done else this
                case Input.El(a) =>
                  val ou = Await.result(consumed.future, streamadapter.timeout)
                  consumed = Promise()
                  // TODO chunking
                  produced.success(Some(Vector(a)))
                  if (ou.isEmpty) done else this
              }
            }
          }

          def folder(step: Step[A, Unit]): Future[Unit] = step match {
            case Step.Done(a, _) => Future.successful(a)
            case Step.Cont(k) => k(Input.EOF).fold({
              case Step.Done(a, _) => Future.successful(a)
              case _ => throw new Exception("Erroneous or diverging iteratee")
            })
            case _ => throw new Exception("Erroneous iteratee")
          }
        }

        // force a trampoline here to return immediately
        trampoline(enumerator(iteratee))
        iterator
      }
    }
  }

}
