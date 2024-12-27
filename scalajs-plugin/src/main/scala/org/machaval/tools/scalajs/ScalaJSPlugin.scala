package org.machaval.tools.scalajs

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.Jar
import org.machaval.tools.buildutil.Configurations
import org.machaval.tools.buildutil.DependencyRequirement
import org.machaval.tools.buildutil.Gradle
import org.machaval.tools.buildutil.ScalaLibrary

import scala.collection.JavaConverters._

class ScalaJSPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit = {
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])
    val scalaJS: Configuration = project.getConfigurations.create(ScalaJSDependencies.configurationName)
    scalaJS.setVisible(false)
    scalaJS.setCanBeConsumed(false)
    scalaJS.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

    val linkMain: LinkTask = project.getTasks.create("linkJs", classOf[LinkTask])
    val jsJar = project.getTasks.register("jarJs", classOf[Jar], new Action[Jar]() {

      override def execute(jar: Jar): Unit = {
        jar.setGroup("build")
        jar.setDescription("Creates a JAR with the contents of build/classes.")
        jar.from(project.file(project.getBuildDir + "/classes"))

        //TODO get the scala js version to put here
        val jarBaseName = project.getName + "_sjs1_2.12"
        jar.getArchiveBaseName.set(jarBaseName)
//        jar.getArchiveVersion.set(project.getVersion.toString)
//        jar.getArchiveClassifier.set("js")
        jar.getDestinationDirectory.set(project.file(project.getBuildDir + "/libs"))
      }
    })


    project.afterEvaluate((project: Project) => {
      val properties = project.getProperties

      val value = properties.get(ScalaJSPlugin.JS_PROPERTY)
      if (java.lang.Boolean.TRUE.toString.equals(value)) {
        project.getLogger.log(LogLevel.INFO, "Enable JS")
        val implementationConfiguration: Configuration = Gradle.getConfiguration(project, Configurations.implementationConfiguration)
        val pluginScalaLibrary: ScalaLibrary = ScalaLibrary.getFromClasspath(Gradle.collectClassPath(getClass.getClassLoader))
        val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(implementationConfiguration)
        val requirements: Seq[DependencyRequirement] = ScalaJSDependencies.dependencyRequirements(
          pluginScalaLibrary,
          projectScalaLibrary,
          implementationConfiguration
        )
        DependencyRequirement.applyToProject(requirements, project)
        projectScalaLibrary.verify(
          ScalaLibrary.getFromClasspath(Gradle.getConfiguration(project, Configurations.implementationClassPath).asScala)
        )
      }
    })
  }
}

object ScalaJSPlugin {
  val JS_PROPERTY: String = "js"
}
