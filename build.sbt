organization := "com.github.kiris"
name := "s2bot"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.8", "2.12.4")

homepage := Some(url("http://github.com/kiris/s2bot"))
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))


resolvers ++= Seq(
  "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "cronish" at "https://kiris.github.io/cronish/repo/",
  "scalender" at "https://kiris.github.io/scalender/repo/"
)

libraryDependencies ++= Seq(
  "com.github.gilbertw1" %% "slack-scala-client" % "0.2.2",

  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-http-core" % "10.0.11",

  "com.github.philcali" %% "cronish" % "0.1.5",
  "com.github.etaty" %% "rediscala" % "1.8.0",

  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

publishTo := Some(Resolver.file("file", file("repo")))

//publishMavenStyle := true
//publishArtifact in Test := false
//publishTo := Some(
//  if (isSnapshot.value)
//    Opts.resolver.sonatypeSnapshots
//  else
//    Opts.resolver.sonatypeStaging
//)
//
//sonatypeProfileName := "com.github.kiris"
//
//scmInfo := Some(
//  ScmInfo(
//    url("https://github.com/kiris/s2bot"),
//    "scm:git:git@github.com:kiris/s2bot.git"
//  )
//)
//
//developers := List(
//  Developer(
//    id = "kiris",
//    name = "Yoshiaki Iwanaga",
//    email = "kiris60@gmail.com",
//    url = url("http://kiris.github.com")
//  )
//)
//
//import ReleaseTransformations._
//releaseProcess := Seq[ReleaseStep](
//  checkSnapshotDependencies,
//  inquireVersions,
//  runClean,
//  runTest,
//  setReleaseVersion,
//  commitReleaseVersion,
//  tagRelease,
//  releaseStepCommand("publishSigned"),
//  setNextVersion,
//  commitNextVersion,
//  releaseStepCommand("sonatypeRelease"),
//  pushChanges
//)
