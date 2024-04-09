ThisBuild / organization := "com.xebia.data"
ThisBuild / scalaVersion := "2.13.13"
ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.13")

lazy val spot = project
    .in(file("./spot"))
    .settings(
        name := "spot",
        libraryDependencies ++= Seq(
            "org.apache.spark" %% "spark-core"        % "3.5.1",

            "io.opentelemetry"  % "opentelemetry-api" % "1.37.0",
            "io.opentelemetry"  % "opentelemetry-sdk" % "1.37.0" % Runtime,

            "io.opentelemetry"  % "opentelemetry-sdk-extension-autoconfigure" % "1.34.0" % Optional,
        )
    )
