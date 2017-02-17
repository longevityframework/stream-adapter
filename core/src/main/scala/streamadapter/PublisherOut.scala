package streamadapter

import org.reactivestreams.Publisher

/** TODO */
trait PublisherOut[P[_]] {

  /** TODO */
  def convert[A](s: Publisher[A]): P[A]

}
