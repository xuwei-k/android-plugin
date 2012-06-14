name := "sbt-android-plugin"

organization := "org.scala-sbt"

version := "0.6.2-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "com.google.android.tools" % "ddmlib" % "r10"
)

sbtPlugin := true

ScriptedPlugin.scriptedSettings

ScriptedPlugin.scriptedBufferLog := false

ScriptedPlugin.scriptedLaunchOpts ++= {
  import scala.collection.JavaConverters._
  val args = Seq("-Xmx","-Xms")
  management.ManagementFactory.getRuntimeMXBean().getInputArguments().asScala.filter(a => args.contains(a) || a.startsWith("-XX")).toSeq
}
