package com.github.gtache.testing

import java.lang.annotation.Annotation
import java.net.{URL, URLClassLoader}
import java.nio.file.Paths

import sbt.testing.{AnnotatedFingerprint, Fingerprint, SubclassFingerprint, TaskDef}

import scala.collection.mutable.ArrayBuffer

object ClassScanner {

  /**
    * Finds all classes contained in an URLClassLoader which match to a fingerprint
    *
    * @param classL       The URLClassLoader
    * @param fingerprints The fingerprints to
    * @return The TaskDefs found by the scan
    */
  def scan(classL: URLClassLoader, fingerprints: Array[Fingerprint], explicitelySpecified: Array[String] = Array.empty): Array[TaskDef] = {

    def checkSuperclasses(c: Class[_], sF: SubclassFingerprint): Boolean = {

      def checkName(c: Class[_], fName: String): Boolean = {
        if (c.getName == fName || c.getSimpleName == fName || c.getCanonicalName == fName) {
        }
        c.getName == fName || c.getSimpleName == fName || c.getCanonicalName == fName
      }


      def checkRec(c: Class[_], fName: String): Boolean = {
        if (checkName(c, fName)) {
          true
        } else {
          var sC = c.getSuperclass
          while (sC != null) {
            if (checkName(sC, fName)) {
              return true
            } else {
              sC = sC.getSuperclass
            }
          }
          c.getInterfaces.foreach(interf => {
            if (checkRec(interf, fName)) {
              return true
            }
          })
          false
        }
      }

      val fName = sF.superclassName()
      checkRec(c, fName)
    }

    val classes = parseClasses(classL)
    val buffer = ArrayBuffer[TaskDef]()
    classes.foreach(c => {
      if (explicitelySpecified.contains(c.getCanonicalName)) {
        buffer += new TaskDef(c.getCanonicalName, null, false, Array.empty)
      } else {
        fingerprints.foreach {
          case aF: AnnotatedFingerprint => {
            try {
              if (c.isAnnotationPresent(Class.forName(aF.annotationName(), false, classL).asInstanceOf[Class[_ <: Annotation]])) {
                buffer += new TaskDef(c.getName, aF, false, Array.empty)
              }
            } catch {
              case e: ClassNotFoundException => {
                Console.err.println("Class not found for annotation : " + aF.annotationName())
              }
            }
          }
          case sF: SubclassFingerprint => {
            if (checkSuperclasses(c, sF)) {
              if (!sF.requireNoArgConstructor || c.isInterface || (sF.requireNoArgConstructor && checkZeroArgsConstructor(c))) {
                buffer += new TaskDef(c.getName, sF, false, Array.empty)
              }
            }
          }
          case _ => throw new IllegalArgumentException("Unsupported Fingerprint type")
        }
      }
    })
    buffer.toArray
  }

  /**
    * Checks if the given class has a constructor with zero arguments
    *
    * @param c The class
    * @return true or false
    */
  def checkZeroArgsConstructor(c: Class[_]): Boolean = {
    println(c.getName)
    c.getDeclaredConstructors.foreach(cons => {
      if (cons.getParameterCount == 0) {
        return true
      }
    })
    false
  }

  /**
    * Finds all classes in a URLClassLoader
    *
    * @param classL The URLClassLoader
    * @return the classes
    */
  def parseClasses(classL: URLClassLoader): Array[Class[_]] = {
    def parseClasses(url: URL, idx: Int): Array[Class[_]] = {
      val f = Paths.get(url.toURI).toFile
      val packageName = {
        if (url != classL.getURLs()(idx)) {
          classL.getURLs()(idx).toURI.relativize(url.toURI).toString.replace('/', '.')
        } else {
          ""
        }
      }
      if (f.isDirectory) {
        val buffer = ArrayBuffer.empty[Class[_]]
        f.listFiles().foreach(file => {
          if (!file.isDirectory && file.getName.endsWith(".class")) {
            val name = file.getName
            buffer += classL.loadClass(packageName + name.substring(0, name.indexOf('.')))
          } else if (file.isDirectory) {
            parseClasses(file.toURI.toURL, idx).foreach(c => {
              buffer += c
            })
          }
        })
        buffer.toArray
      } else {
        if (f.getName.endsWith(".class")) {
          val name = f.getName
          Array(classL.loadClass(packageName + name.substring(0, name.indexOf('.'))))
        } else {
          Array.empty[Class[_]]
        }
      }
    }

    val buffer = ArrayBuffer.empty[Class[_]]
    classL.getURLs.zipWithIndex.foreach(url => {
      val f = Paths.get(url._1.toURI).toFile
      if (!f.isDirectory && f.getName.endsWith(".class")) {
        val name = f.getName
        buffer += classL.loadClass(name.substring(0, name.indexOf('.')))
      } else if (f.isDirectory) {
        parseClasses(url._1, url._2).foreach(c => {
          buffer += c
        })
      }
    })
    buffer.toArray
  }
}


