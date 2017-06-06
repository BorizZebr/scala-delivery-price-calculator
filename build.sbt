import sbt.Keys._

lazy val `price-calculator`: Project = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, DebianPlugin, SystemdPlugin)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    organization := "com.zebrosoft",
    name := "scala-delivery-price-calculator",
    version := "1.0",
    scalaVersion := "2.11.8",
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    fork in run := true,
    maintainer in Linux := "Boriz Zebr <borizzebr@gmail.com>",
    packageSummary in Linux := "Blah blah price service",
    packageDescription := "Blah blah blah price service",
    parallelExecution in IntegrationTest := false,
    libraryDependencies ++= dependencies,
    unmanagedClasspath in Runtime += baseDirectory.value / "src" / "main" / "data",
    scriptClasspath := Seq("../conf/", "../data/") ++ scriptClasspath.value,
    mappings in Universal <++= sourceDirectory map { src =>
      Seq(
        src / "main" / "resources" / "prod.conf" -> "conf/application.conf",
        src / "main" / "data" / "packages.csv" -> "data/packages.csv")
    },
    javaOptions in Universal ++= Seq(
      // -J params will be added as jvm parameters
      "-J-Xmx64m",
      "-J-Xms64m"
    )
  )

lazy val dependencies: Seq[ModuleID] = {
  val akkaV       = "2.4.8"
  val scalaTestV  = "2.2.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "org.scalatest"     %% "scalatest" % scalaTestV % "test"
  )
}