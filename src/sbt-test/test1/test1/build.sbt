import AndroidKeys._

name := "test1"

scalaVersion := "2.10.0-M4"

platformName in Android := "android-7"

seq(
  {AndroidProject.androidSettings ++ TypedResources.settings ++ AndroidMarketPublish.settings}:_*
)

keyalias in Android := "change-me"

libraryDependencies ++= Seq()

