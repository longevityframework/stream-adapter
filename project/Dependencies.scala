import sbt._
import Keys._

object Dependencies {

  val scalaVersionString = "2.12.6"

  val akkaStreamDep     : ModuleID = "com.typesafe.akka"      %% "akka-stream"      % "2.5.12"
  val catsDep           : ModuleID = "org.typelevel"          %% "cats-core"        % "1.1.0"
  val catsEffectDep     : ModuleID = "org.typelevel"          %% "cats-effect"      % "0.10.1"
  val catsIterateeDep   : ModuleID = "io.iteratee"            %% "iteratee-core"    % "0.17.0"
  val fs2CoreDep        : ModuleID = "co.fs2"                 %% "fs2-core"         % "0.10.4"
  val nScalaTimeDep     : ModuleID = "com.github.nscala-time" %% "nscala-time"      % "2.20.0"
  val playIterateeDep   : ModuleID = "com.typesafe.play"      %% "play-iteratees"   % "2.6.1"
  val reactiveStreamsDep: ModuleID = "org.reactivestreams"    %  "reactive-streams" % "1.0.0"
  val specs2Dep         : ModuleID = "org.specs2"             %% "specs2-core"      % "4.2.0"
  val typesafeConfigDep : ModuleID = "com.typesafe"           %  "config"           % "1.3.3"

}
