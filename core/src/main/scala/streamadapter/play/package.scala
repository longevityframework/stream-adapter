package streamadapter

import _root_.play.api.libs.iteratee.Enumerator
import _root_.play.api.libs.iteratee.Input
import _root_.play.api.libs.iteratee.Iteratee
import _root_.play.api.libs.iteratee.Step.Cont
import _root_.play.api.libs.iteratee.Step.Done
import _root_.play.api.libs.iteratee.Step.Error
import java.io.Closeable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking

/** TODO */
package object play {

  type EffectiveEnumerator[F[_], A] = Enumerator[A]

  /** produces a publisher adapter from iterator generator to akka source */
  implicit def iterGenToPlayEnumerator(implicit context: ExecutionContext) = {
    new PublisherAdapter[IterGen, Enumerator] {
      def adapt[A](iterGen: IterGen[A]): Enumerator[A] = {
        new Enumerator[A] {
          def apply[B](iteratee: Iteratee[A, B]): Future[Iteratee[A, B]] = {
            val iterator = iterGen()
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
                      applyInternal(k(Input.El(iterator.next)))
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

  // TODO
  /** produces a publisher adapter from akka source to iterator generator */
  implicit def playEnumeratorToIterGen = {
    new PublisherAdapter[Enumerator, IterGen] {
      def adapt[A](enumerator: Enumerator[A]): IterGen[A] = { () =>
        new Iterator[A] with Closeable {
          def hasNext = ???
          def next    = ???
          def close   = ???
        }
      }
    }
  }

}
