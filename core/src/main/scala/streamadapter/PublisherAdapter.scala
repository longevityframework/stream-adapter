package streamadapter

/** TODO */
trait PublisherAdapter[
  F1[_],
  P1[F[_], _],
  F2[_],
  P2[F[_], _]] {

  /** TODO */
  def adaptPublisher[A](p1: P1[F1, A]): P2[F2, A]

}
