package streamadapter

/** TODO */
trait PublisherAdapter[P1[_], P2[_]] {

  /** TODO */
  def adapt[A](p1: P1[A]): P2[A]

}
