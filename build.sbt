organization := "com.github.kiris"
name := "s2bot"

scalaVersion := "2.12.3"
crossScalaVersions := Seq("2.11.3", "2.11.8")

homepage := Some(url("http://github.com/kiris/s2bot"))
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))


resolvers ++= Seq(
  "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.github.gilbertw1" %% "slack-scala-client" % "0.2.2",

  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-http-core" % "10.0.0",

  "com.enragedginger" %% "akka-quartz-scheduler" % "1.6.1-akka-2.5.x",
  "net.debasishg" %% "redisclient" % "3.4",

  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

publishMavenStyle := true
publishArtifact in Test := false
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

sonatypeProfileName := "com.github.kiris"

scmInfo := Some(
  ScmInfo(
    url("https://github.com/kiris/s2bot"),
    "scm:git:git@github.com:kiris/s2bot.git"
  )
)

developers := List(
  Developer(
    id = "kiris",
    name = "Yoshiaki Iwanaga",
    email = "kiris60@gmail.com",
    url = url("http://kiris.github.com")
  )
)

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeRelease"),
  pushChanges
)
