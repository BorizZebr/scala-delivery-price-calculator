name := "scala-delivery-price-calculator"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaV       = "2.4.8"
  val scalaTestV  = "2.2.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "org.scalatest"     %% "scalatest" % scalaTestV % "test"
  )
}

enablePlugins(JavaAppPackaging)

unmanagedClasspath in Runtime += baseDirectory.value / "src" / "main" / "data"

mappings in Universal <++= sourceDirectory map { src =>
  Seq(
    src / "main" / "resources" / "prod.conf" -> "conf/application.conf",
    src / "main" / "data" / "packages.csv" -> "data/packages.csv")
}

scriptClasspath := Seq("../conf/", "../data/") ++ scriptClasspath.value