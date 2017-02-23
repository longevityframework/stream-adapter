package streamadapter

/** TODO */
// we really have to accomodate
trait PublisherAdapter[P1[_], P2[_]] {

  /** TODO */
  def adaptPublisher[A](p1: P1[A]): P2[A]

}
