package usage

object Usage extends App {

  import akka.actor.ActorSystem
  import akka.stream.ActorMaterializer
  import akka.stream.scaladsl.Source
  implicit val actorSystem = ActorSystem("streamadapter")
  implicit val materializer = ActorMaterializer()
  val akkaSource = Source(0.until(10))

  {
    import akka.stream.scaladsl.Keep
    import akka.stream.scaladsl.Sink
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    println("akkaSource = " + Await.result(akkaSource.toMat(Sink.seq[Int])(Keep.right).run, Duration.Inf))
  }

  val fs2Stream: fs2.Stream[cats.effect.IO, Int] = {
    import streamadapter._
    import streamadapter.akka._
    import streamadapter.fs2._
    adapt[AkkaSource, FS2Stream, Int](akkaSource)
  }

  println("fs2Stream = " + fs2Stream.compile.toVector.unsafeRunSync)

  val iterateeIoEnumerator: io.iteratee.Enumerator[cats.Eval, Int] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import streamadapter._
    import streamadapter.fs2._
    import streamadapter.iterateeio._
    adapt[FS2Stream, EvalEnumerator, Int](fs2Stream)
  }

  println("iterateeIoEnumerator = " + io.iteratee.Iteratee.consume[cats.Eval, Int].apply(iterateeIoEnumerator).run.value)

  val playEnumerator: play.api.libs.iteratee.Enumerator[Int] = {
    import streamadapter._
    import streamadapter.iterateeio._
    import streamadapter.play._
    import scala.concurrent.ExecutionContext.Implicits.global
    adapt[EvalEnumerator, PlayEnumerator, Int](iterateeIoEnumerator)
  }

  {
    import play.api.libs.iteratee.Iteratee
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    val f = playEnumerator.run(Iteratee.fold[Int, Seq[Int]](Seq())(_ :+ _))
    println("playEnumerator = " + Await.result(f, Duration.Inf))
  }

  actorSystem.terminate

}
