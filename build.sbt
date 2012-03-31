name := "sbt-android-plugin"

organization := "org.scala-sbt"

version := "0.6.2-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "com.google.android.tools" % "ddmlib" % "r10"
)

sbtPlugin := true
