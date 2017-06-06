logLevel := Level.Warn

resolvers ++= Seq(Classpaths.sbtPluginReleases, Classpaths.sbtPluginSnapshots)

addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.0-M5")