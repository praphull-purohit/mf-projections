name := """mf-projections"""
organization := "com.praphull"
description := "Mutual Fund Projections"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,

  "com.praphull" %% "scala-finance" % "0.0.1"
)