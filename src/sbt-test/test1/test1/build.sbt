import AndroidKeys._

name := "test1"

scalaVersion := "2.8.2"

platformName in Android := "android-7"

seq(
  {AndroidProject.androidSettings ++ TypedResources.settings ++ AndroidMarketPublish.settings}:_*
)

keyalias in Android := "change-me"

libraryDependencies ++= Seq()

