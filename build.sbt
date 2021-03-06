import sbt.Keys.libraryDependencies

val Scala212 = "2.12.8"

lazy val baseSettings = Seq(
  homepage := Some(url("http://github.com/kiris/s2bot")),
  licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  organization := "com.github.kiris",
  scalaVersion := Scala212,
  crossScalaVersions := Scala212 :: Nil,
  version := (version in ThisBuild).value,
  scalacOptions := Seq(
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-Xfatal-warnings"
  ),
  libraryDependencies := Seq(
    "org.typelevel" %% "cats-core" % "2.0.0-M1",
    "org.scalactic" %% "scalactic" % "3.0.1",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test"

  ),
  parallelExecution in ThisBuild := false,
  publishArtifact in packageDoc := false,

  publishTo := Some(Resolver.file("file", file("repo")))
  // scalafmtOnCompile := true,
  // scalafmtVersion := "1.4.0"

)

lazy val modules: Seq[ProjectReference] = Seq(
  core,
  cronJobExtension,
  brainExtension,
  brainPlayJsonExtension,
  redisBrainExtension,
  choicePlugin,
  youbiPlugin,
  newChannelsPlugin,
  newEmojisPlugin,
  hotTopicsPlugin,
  ameshPlugin,
  googleImageSearchPlugin,
  deleteMessagePlugin,
  welcomeChannelPlugin,
  userDictionaryPlugin
)

lazy val all = (project in file("."))
    .settings(baseSettings)
    .settings(
      name := "s2bot-all"
    )
    .aggregate(modules: _*)
    .dependsOn(modules.map(_ % "test->test;compile->compile"): _*)

lazy val core = (project in file("core"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-core",
      resolvers ++= Seq(
        "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
        "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
      ),
      libraryDependencies ++= Seq(
        "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.6",
        "com.typesafe.akka" %% "akka-actor" % "2.4.14",
        "com.typesafe.akka" %% "akka-http-core" % "10.0.11"
      )
    )

lazy val cronJobExtension = (project in file("extensions/cron-job"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-cron-job-extension",
      resolvers ++= Seq(
        "cronish" at "https://kiris.github.io/cronish/repo/",
        "scalender" at "https://kiris.github.io/scalender/repo/",
      ),
      libraryDependencies ++= Seq(
        "com.github.philcali" %% "cronish" % "0.1.5"
      )
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )


lazy val brainExtension = (project in file("extensions/brain"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-brain-extension",
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val brainPlayJsonExtension = (project in file("extensions/brain-play-json"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-brain-play-json-extension",
      libraryDependencies ++= Seq(
        "com.typesafe.play" %% "play-json" % "2.6.7"
      )
    )
    .dependsOn(
      brainExtension % "test->test;compile->compile"
    )

lazy val redisBrainExtension = (project in file("extensions/redis-brain"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-redis-brain-extension",
      libraryDependencies ++= Seq(
        "com.github.etaty" %% "rediscala" % "1.8.0"
      )
    )
    .dependsOn(
      brainExtension % "test->test;compile->compile"
    )


lazy val choicePlugin = (project in file("plugins/choice"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-choice-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val youbiPlugin = (project in file("plugins/youbi"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-youbi-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val newChannelsPlugin = (project in file("plugins/new-channels"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-new-channels-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val newEmojisPlugin = (project in file("plugins/new-emojis"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-new-emojis-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile",
      brainExtension % "test->test;compile->compile",
      brainPlayJsonExtension  % "test->test;compile->compile",
      cronJobExtension % "test->test;compile->compile"
    )

lazy val hotTopicsPlugin = (project in file("plugins/hot-topics"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-hot-topics-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val ameshPlugin = (project in file("plugins/amesh"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-amesh-plugin",
      libraryDependencies ++= Seq(
        "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
        "com.sksamuel.scrimage" %% "scrimage-io-extra" % "2.1.8",
        "com.sksamuel.scrimage" %% "scrimage-filters" % "2.1.8",
        "net.databinder.dispatch" %% "dispatch-core" % "0.13.3"
      )
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val googleImageSearchPlugin = (project in file("plugins/google-image-search"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-google-image-search-plugin",
      libraryDependencies ++= Seq(
        "net.databinder.dispatch" %% "dispatch-core" % "0.13.3",
        "com.github.etaty" %% "rediscala" % "1.8.0"
      )
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val deleteMessagePlugin = (project in file("plugins/delete-message"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-delete-message-plugin"
    )
    .dependsOn(
      core % "test->test;compile->compile"
    )

lazy val welcomeChannelPlugin = (project in file("plugins/welcome-channel"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-welcome-channel-plugin",
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(
      core % "test->test;compile->compile",
      brainExtension % "test->test;compile->compile"
    )

lazy val userDictionaryPlugin = (project in file("plugins/user-dictionary"))
    .settings(baseSettings)
    .settings(
      name := "s2bot-user-dictionary-plugin",
      libraryDependencies ++= Seq(
      )
    )
    .dependsOn(
      core % "test->test;compile->compile",
      brainExtension % "test->test;compile->compile"
    )