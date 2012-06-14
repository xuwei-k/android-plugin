import sbt._
import Keys._
import AndroidKeys._
import AndroidHelpers._

import java.io.{File => JFile}

object AndroidInstall {

  private def installTask(emulator: Boolean) = (dbPath, packageApkPath, streams) map { (dp, p, s) =>
    adbTask(dp.absolutePath, emulator, s, "install", "-r ", p.absolutePath)
  }

  private def uninstallTask(emulator: Boolean) = (dbPath, manifestPackage, streams) map { (dp, m, s) =>
    adbTask(dp.absolutePath, emulator, s, "uninstall", m)
  }

  private def aaptPackageTask: Project.Initialize[Task[File]] =
  (aaptPath, manifestPath, mainResPath, mainAssetsPath, jarPath, resourcesApkPath, extractApkLibDependencies, streams) map {
    (apPath, manPath, rPath, assetPath, jPath, resApkPath, apklibs, s) =>

    val libraryResPathArgs = for (
      lib <- apklibs;
      d <- lib.resDir.toSeq;
      arg <- Seq("-S", d.absolutePath)
    ) yield arg

    val aapt = Seq(apPath.absolutePath, "package", "--auto-add-overlay", "-f",
        "-M", manPath.head.absolutePath,
        "-S", rPath.absolutePath,
        "-A", assetPath.absolutePath,
        "-I", jPath.absolutePath,
        "-F", resApkPath.absolutePath) ++
        libraryResPathArgs
    s.log.debug("packaging: "+aapt.mkString(" "))
    if (aapt.run(false).exitValue != 0) sys.error("error packaging resources")
    resApkPath
  }

  private def dxTask: Project.Initialize[Task[File]] =
    (dxPath, dxInputs, dxOpts, classDirectory, classesDexPath, streams) map {
    (dxPath, dxInputs, dxOpts, classDirectory, classesDexPath, streams) =>

      val uptodate = classesDexPath.exists &&
        !(dxInputs +++ (classDirectory ** "*.class") get).exists (_.lastModified > classesDexPath.lastModified)

      if (!uptodate) {
        //val noLocals = if (proguardOptimizations.isEmpty) "" else "--no-locals"
        val noLocals =  "--no-locals"
        val dxCmd = (Seq(dxPath.absolutePath,
                        dxMemoryParameter(dxOpts._1),
                        "--dex", noLocals,
                        "--num-threads="+java.lang.Runtime.getRuntime.availableProcessors,
                        "--output="+classesDexPath.absolutePath) ++
                        dxInputs.get.map(_.absolutePath)).filter(_.length > 0)
        streams.log.debug(dxCmd.mkString(" "))
        streams.log.info("Dexing "+classesDexPath)
        streams.log.debug(dxCmd !!)
      } else streams.log.debug("dex file uptodate, skipping")
      classesDexPath
    }

  private def packageTask(debug: Boolean):Project.Initialize[Task[File]] = (packageConfig, streams) map { (c, s) =>
    val builder = new ApkBuilder(c, debug)
    builder.build.fold(sys.error(_), s.log.info(_))
    s.log.debug(builder.outputStream.toString)
    c.packageApkPath
  }

  lazy val installerTasks = Seq (
    installEmulator <<= installTask(emulator = true) dependsOn packageDebug,
    installDevice <<= installTask(emulator = false) dependsOn packageDebug
  )

  lazy val settings: Seq[Setting[_]] = inConfig(Android) (installerTasks ++ Seq (
    uninstallEmulator <<= uninstallTask(emulator = true),
    uninstallDevice <<= uninstallTask(emulator = false),

    makeAssetPath <<= directory(mainAssetsPath),

    aaptPackage <<= aaptPackageTask,
    aaptPackage <<= aaptPackage dependsOn (makeAssetPath, dx),
    dx <<= dxTask,
    scalaLibraryFilter <<= (streams).map{s =>
      {base:File =>
        new SimpleFileFilter(f =>
          IO.relativize(base,f).map{ path =>
            if(path.startsWith("scala/actors") || path.startsWith("scala/concurrent/forkjoin")){
              s.log.info("delete " + path)
              false
            }else true
          }.getOrElse(false)
        )
      }
    },
    scalaMinJar <<= (scalaInstance,scalaLibraryFilter) map { (scalaInstance,scalaLibraryFilter) =>
      val dir = IO.createTemporaryDirectory
      val files = IO.unzip(scalaInstance.libraryJar,dir)
      val filter = scalaLibraryFilter(dir)
      val newFiles = files.filter(filter.accept)
      val miniScala = dir / "scala-library-min.jar"
      IO.zip( newFiles.map{f => f -> IO.relativize(dir,f).get } , miniScala )
      miniScala
    },
    dxInputs <<= (proguardInJars, classDirectory, scalaMinJar) map {
      ( proguardInJars, classDirectory, scalaMinJar) =>
        (classDirectory +++ proguardInJars +++ scalaMinJar ) get
    },

    cleanApk <<= (packageApkPath) map (IO.delete(_)),
    packageConfig <<=
      (toolsPath, packageApkPath, resourcesApkPath, classesDexPath,
       nativeLibrariesPath, managedNativePath, dxInputs, resourceDirectory) map
      (ApkConfig(_, _, _, _, _, _, _, _)),

    packageDebug <<= packageTask(true),
    packageRelease <<= packageTask(false),
    autoScalaLibrary in GlobalScope := false
  ) ++ Seq(packageDebug, packageRelease).map {
    t => t <<= t dependsOn (cleanApk, aaptPackage, copyNativeLibraries)
  })
}
