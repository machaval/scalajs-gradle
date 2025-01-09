package org.machaval.tools.scalajs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.ScalaSourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.machaval.tools.buildutil.Configurations
import org.machaval.tools.buildutil.DependencyRequirement
import org.machaval.tools.buildutil.Gradle
import org.machaval.tools.buildutil.ScalaLibrary

import java.io.File
import scala.collection.JavaConverters._

class ScalaJSPlugin extends Plugin[Project] {

  override def apply(project: Project): Unit = {
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])
    val scalaJS: Configuration = project.getConfigurations.create(ScalaJSDependencies.scalaJsConfigurationName)
    scalaJS.setVisible(false)
    scalaJS.setCanBeConsumed(false)
    scalaJS.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

    val linkMain: LinkTask = project.getTasks.create("link", classOf[LinkTask])

    project.afterEvaluate((project: Project) => {
      val properties = project.getProperties
      val target = Option(properties.get(ScalaJSPlugin.TARGET_PROPERTY_NAME)).getOrElse(ScalaJSPlugin.JVM_TARGET)
      addSourceSet(project, target)
      if (ScalaJSPlugin.JS_TARGET.equals(target)) {
        project.getLogger.log(LogLevel.INFO, "Running JS Build.....")
        val dependencies = project.getDependencies
        project.getConfigurations()
          .getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
          .getDependencies()
          .all((dependency: Dependency) => {
            if (dependency.getGroup != null
              && dependency.getName != null
              && !dependency.getName.endsWith(ScalaJSPlugin.SCALA_JS_PREFIX(project)) //Already transformed
              && !isScalaJSLibrary(dependency.getName)) {
              val newArtifactId = toJSName(project, dependency.getName)
              project.getLogger.log(LogLevel.ERROR, "Name: -> " + dependency.getName + " is updated to: -> " + newArtifactId)
              val newDependency: Dependency = dependencies.create(String.format("%s:%s:%s", dependency.getGroup, newArtifactId, dependency.getVersion))
              dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, newDependency)
              project.getConfigurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).getDependencies.remove(dependency)
            } else {
              project.getLogger.log(LogLevel.INFO, "Dependency -> " + dependency.toString + " is not updated")
            }
          })

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

  private def addSourceSet(project: Project, target: Any): Unit = {

    val sourceSets = project.getProperties().get("sourceSets").asInstanceOf[SourceSetContainer];
    if (sourceSets == null) {
      project.getLogger().warn("No SourceSetContainer found, skipping source set configuration.");
    } else {
      // Determine the folder to add based on the 'target' property
      var folderToAdd: String = "src/main/jvm";
      if (ScalaJSPlugin.JS_TARGET.equals(target)) {
        folderToAdd = "src/main/js";
      }
      // Verify the folder exists before adding it
      val folder = new File(project.getProjectDir(), folderToAdd);
      if (folder.exists() && folder.isDirectory()) {
        // Add the folder to the main source set
        val mainSourceSet: SourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        mainSourceSet.getExtensions().getByType(classOf[ScalaSourceDirectorySet]).srcDir(folderToAdd)
        project.getLogger().lifecycle("Added {} to the main scala source set.", folderToAdd);
      }
    }
  }

  private def isScalaJSLibrary(artifactName: String) = {
    artifactName.equals("scala-library") || artifactName.startsWith("scalajs-library")
  }

  private def toJSName(project: Project, artifactName: String) = {
    val i = artifactName.lastIndexOf('_')
    val jarBaseName = if (i > 0) {
      artifactName.substring(0, i) + ScalaJSPlugin.SCALA_JS_PREFIX(project)
    } else {
      artifactName + ScalaJSPlugin.SCALA_JS_PREFIX(project)
    }
    jarBaseName
  }
}

object ScalaJSPlugin {
  val TARGET_PROPERTY_NAME: String = "target"
  val JS_TARGET: String = "js"
  val JVM_TARGET: String = "jvm"

  def SCALA_JS_PREFIX(project: Project): String = {
    var scalaVersion = "2.12"
    val value = project.getProperties.get("scalaVersion")
    if (value != null) {
      val strVersion = value.toString
      scalaVersion = strVersion.substring(0, strVersion.lastIndexOf("."));
    }
    project.getLogger.lifecycle("Scala Version: {}", scalaVersion)
    s"_sjs1_${scalaVersion}"
  }
}
