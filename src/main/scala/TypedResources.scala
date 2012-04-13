import sbt._
import classpath._
import scala.xml._

import Keys._
import AndroidKeys._

object TypedResources {
  private def generateTypedResourcesTask =
    (typedResource, layoutResources, jarPath, manifestPackage, typedResourceType, streams) map {
    (typedResource, layoutResources, jarPath, manifestPackage, typedResourceType, s) =>
      val Id = """@\+id/(.*)""".r
      val androidJarLoader = ClasspathUtilities.toLoader(jarPath)

      def tryLoading(className: String) = {
        try {
          Some(androidJarLoader.loadClass(className))
        } catch {
          case _ => None
        }
      }

      val layouts = layoutResources.get.map{ layout =>
        val Name = "(.*)\\.[^\\.]+".r
        layout.getName match {
          case Name(name) => Some(name)
          case _ => None
        }
      }
      val reserved = List("extends", "trait", "type", "val", "var", "with")

      val resources = layoutResources.get.flatMap { path =>
        XML.loadFile(path).descendant_or_self flatMap { node =>
          // all nodes
          node.attribute("http://schemas.android.com/apk/res/android", "id") flatMap {
            // with android:id attribute
            _.headOption.map { _.text } flatMap {
              // if it looks like a full classname
              case Id(id) if node.label.contains('.') => Some(id, node.label)
              // otherwise it may be a widget or view
              case Id(id) => {
                List("android.widget.", "android.view.", "android.webkit.").map(pkg =>
                  tryLoading(pkg + node.label)).find(_.isDefined).flatMap(clazz =>
                    Some(id, clazz.get.getName)
                  )
              }
              case _ => None
            }
          }
        }
      }.foldLeft(Map.empty[String, String]) {
        case (m, (k, v)) =>
          m.get(k).foreach { v0 =>
            if (v0 != v) s.log.warn("Resource id '%s' mapped to %s and %s" format (k, v0, v))
          }
          m + (k -> v)
      }.filterNot {
        case (id, _) => reserved.contains(id)
      }

      typedResourceType.write(typedResource,manifestPackage,resources,layouts)

      s.log.info("Wrote %s" format(typedResource))
      Seq(typedResource)
    }

  lazy val settings: Seq[Setting[_]] = inConfig(Android) (Seq (
    typedResourceType <<= (autoScalaLibrary){ autoScala =>
      if(autoScala) TypedResourceType.Scala 
      else TypedResourceType.Java
    },
    typedResource <<= (manifestPackage, managedScalaPath, typedResourceType) map {
      (manifestPackage, managedScalaPath, typedResourceType) =>
       manifestPackage.split('.').foldLeft(managedScalaPath)(_ / _) / (typedResourceType.fileName)
    },
    layoutResources <<= (mainResPath) map { x=>  (x / "layout" ** "*.xml" get) },

    generateTypedResources <<= generateTypedResourcesTask,

    sourceGenerators in Compile <+= generateTypedResources,
    watchSources in Compile <++= (layoutResources) map (ls => ls)
  )) ++ Seq (
    generateTypedResources <<= (generateTypedResources in Android)
  )
}
