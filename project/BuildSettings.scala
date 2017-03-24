import sbt._
import Keys._
import com.typesafe.sbt.pgp.PgpKeys._

object BuildSettings {

  val commonSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "org.longevityframework",
    version := "0.1-SNAPSHOT",
    scalaVersion := Dependencies.scalaVersionString,
    crossScalaVersions := Seq("2.11.8", Dependencies.scalaVersionString),
    cancelable in Global := true)

  val publishSettings = commonSettings ++ Seq(
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    licenses := Seq("Apache License, Version 2.0" ->
                    url("http://www.apache.org/licenses/LICENSE-2.0"))
  )

  val buildSettings = publishSettings ++ Seq(
    fork := true,

    // compile
    scalacOptions ++= nonConsoleScalacOptions ++ otherScalacOptions,

    // console
    scalacOptions in (Compile, console) ~= (_ filterNot (nonConsoleScalacOptions.contains(_))),
    scalacOptions in (Test, console) ~= (_ filterNot (nonConsoleScalacOptions.contains(_))),

    // scaladoc
    scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits", "-encoding", "UTF-8", "-diagrams"),
    scalacOptions in (Compile, doc) ++= {
      val projectName = (name in (Compile, doc)).value
      val projectVersion = (version in (Compile, doc)).value
      Seq("-doc-title", s"$projectName $projectVersion API")
    }
  )

  val noPublishSettings = commonSettings ++ Seq(
    packagedArtifacts := Map.empty,
    publishLocal := (),
    publishSigned := (),
    publish := ())

  def streamadapterHomepage = Some(url("https://github.com/longevityframework/streamadapter"))

  def streamadapterPomExtra = (
    <scm>
      <url>git@github.com:longevityframework/stream-adapter.git</url>
      <connection>scm:git:git@github.com:longevityframework/stream-adapter.git</connection>
    </scm>
    <developers>
      <developer>
        <id>sullivan-</id>
        <name>John Sullivan</name>
        <url>https://github.com/sullivan-</url>
      </developer>
    </developers>)

  private def githubUrl = "https://github.com/longevityframework/stream-adapter"

  private def nonConsoleScalacOptions = Seq(
    "-Xfatal-warnings")

  private def otherScalacOptions = Seq(
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked")

  private def gitHash = sys.process.Process("git rev-parse HEAD").lines_!.head

}
