package streamadapter

/** adapts streams from one streaming library into another streaming library
 * @tparam P1 the type of stream to adapt from
 * @tparam P2 the type of stream to adapt into
 */
trait StreamAdapter[P1[_], P2[_]] {

  /** adapts a stream of elements from one streaming library into a stream from another streaming library
   * @tparam A the type of the elements of the stream
   * @param p1 the stream to adapt
   * @return the adapted stream
   */
  def adapt[A](p1: P1[A]): P2[A]

}
