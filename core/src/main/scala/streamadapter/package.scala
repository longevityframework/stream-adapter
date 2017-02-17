
/** TODO */
package object streamadapter {

  /** TODO */
  def adaptPublisher[P1[_] : PublisherIn, P2[_] : PublisherOut, A](s: P1[A]): P2[A] =
    implicitly[PublisherOut[P2]].convert(implicitly[PublisherIn[P1]].convert(s))

}
