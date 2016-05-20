package com.github.gtache.tasks

import com.github.gtache.Utils
import com.github.gtache.testing.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.scalajs.core.tools.jsdep.ResolvedJSDependency
import org.scalajs.core.tools.logging.Level
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import org.scalajs.jsenv.ComJSEnv
import org.scalajs.jsenv.ConsoleJSConsole$
import org.scalajs.testadapter.ScalaJSFramework
import sbt.testing.*
import scala.collection.JavaConverters
import scala.collection.Seq
import scala.collection.mutable.ArrayBuffer

/**
 * A task used to run tests for various frameworks
 */
public class TestJSTask extends DefaultTask {
    final String description = "Runs tests"

    private static final String LOG_LEVEL = 'testLogLevel'

    /**
     * The action of the task : Instantiates a framework, a runner, and executes all tests found, with the fingerprints
     * given by the framework
     */
    @TaskAction
    def run() {
        final Seq<ResolvedJSDependency> dependencySeq = Utils.getMinimalDependencySeq(project)
        final def libEnv = (ComJSEnv) Utils.resolveEnv(project).loadLibs(dependencySeq)

        final List<TestFramework> customTestFrameworks = Utils.resolveTestFrameworks(project)
        final ArrayBuffer<TestFramework> allFrameworks = new ArrayBuffer<>()
        customTestFrameworks.each {
            allFrameworks.$plus$eq(it)
        }
        final Seq<TestFramework> defaultFrameworks = TestFrameworks.defaultFrameworks()
        for (int i = 0; i < defaultFrameworks.length(); ++i) {
            allFrameworks.$plus$eq(defaultFrameworks.apply(i))
        }
        final List<ScalaJSFramework> frameworks = JavaConverters.asJavaIterableConverter(new FrameworkDetector(libEnv).instantiatedScalaJSFrameworks(
                allFrameworks.toSeq(),
                new ScalaConsoleLogger(Utils.resolveLogLevel(project, LOG_LEVEL, Level.Info$.MODULE$)),
                ConsoleJSConsole$.MODULE$
        )).asJava().toList()

        final URL[] urls = project.sourceSets.test.runtimeClasspath.collect { it.toURI().toURL() } as URL[]
        final URLClassLoader classL = new URLClassLoader(urls)

        frameworks.each { ScalaJSFramework framework ->
            project.logger.info("Framework found : " + framework.name())
        }

        Set<String> explicitlySpecified = new HashSet<>()
        scala.collection.immutable.Set<String> excluded = new scala.collection.immutable.HashSet<String>()
        if (project.hasProperty('test-only')) {
            explicitlySpecified = ((String) project.property('test-only')).split(File.pathSeparator).toList().toSet()
                    .collect { Utils.toRegex(it) }
        } else if (project.hasProperty('test-quick')) {
            explicitlySpecified = ((String) project.property('test-quick')).split(File.pathSeparator).toList().toSet()
                    .collect { Utils.toRegex(it) }
            excluded = ScalaJSTestResult$.MODULE$.successfulClassnames()
        } else if (project.hasProperty('retest')) {
            excluded = ScalaJSTestResult$.MODULE$.successfulClassnames()
        }
        scala.collection.immutable.Set<String> explicitlySpecifiedScala = JavaConverters.asScalaSetConverter(explicitlySpecified).asScala().toSet()

        Logger[] simpleLoggerArray = new SimpleLogger() as Logger[]
        frameworks.each { ScalaJSFramework framework ->
            final Runner runner = framework.runner(new String[0], new String[0], null)
            final Fingerprint[] fingerprints = framework.fingerprints()
            final Task[] tasks = runner.tasks(ClassScanner.scan(classL, fingerprints, explicitlySpecifiedScala, excluded))
            project.logger.info("Executing " + framework.name())
            if (tasks.length == 0) {
                project.logger.info("No tasks found")
            } else {
                final ScalaJSTestStatus testStatus = new ScalaJSTestStatus(framework)
                final EventHandler eventHandler = new ScalaJSEventHandler(testStatus)
                ScalaJSTestResult$.MODULE$.statuses_$eq(ScalaJSTestResult$.MODULE$.statuses().$plus(testStatus) as scala.collection.immutable.Set<ScalaJSTestStatus>)
                testStatus.runner_$eq(runner)
                tasks.each { Task t ->
                    t.execute(eventHandler, simpleLoggerArray)
                }
                project.logger.lifecycle('\n')
                runner.done()
                testStatus.finished_$eq(true)
            }
        }

        project.logger.lifecycle(ScalaJSTestResult$.MODULE$.toString())

    }
}