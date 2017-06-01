import sbt._
import Keys._

object Dependencies {

  val scalaVersionString = "2.12.2"

  val akkaStreamDep     : ModuleID = "com.typesafe.akka"      %% "akka-stream"      % "2.5.2"
  val catsDep           : ModuleID = "org.typelevel"          %% "cats"             % "0.9.0"
  val catsIterateeDep   : ModuleID = "io.iteratee"            %% "iteratee-core"    % "0.12.0"
  val fs2CoreDep        : ModuleID = "co.fs2"                 %% "fs2-core"         % "0.9.6"
  val nScalaTimeDep     : ModuleID = "com.github.nscala-time" %% "nscala-time"      % "2.16.0"
  val playIterateeDep   : ModuleID = "com.typesafe.play"      %% "play-iteratees"   % "2.6.1"
  val reactiveStreamsDep: ModuleID = "org.reactivestreams"    %  "reactive-streams" % "1.0.0"
  val specs2Dep         : ModuleID = "org.specs2"             %% "specs2-core"      % "3.8.9"
  val typesafeConfigDep : ModuleID = "com.typesafe"           %  "config"           % "1.3.1"

}
