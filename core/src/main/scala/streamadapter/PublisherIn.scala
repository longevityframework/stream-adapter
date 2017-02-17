package streamadapter

import org.reactivestreams.Publisher

/** TODO */
trait PublisherIn[P[_]] {

  /** TODO */
  def convert[A](s: P[A]): Publisher[A]

}
