package com.github.gtache.testing

import org.scalajs.testadapter.ScalaJSFramework
import sbt.testing.{Runner, Task, TaskDef}

/**
  * A class storing informations about a TestFramework (results of test)
  *
  * @param framework The framework which corresponds to this instance
  */
final class ScalaJSTestStatus(framework: ScalaJSFramework) {
  var runner: Runner = null
  var all: List[Task] = List.empty
  var errored: List[TaskDef] = List.empty
  var failed: List[TaskDef] = List.empty
  var succeeded: List[TaskDef] = List.empty
  var skipped: List[TaskDef] = List.empty
  var ignored: List[TaskDef] = List.empty
  var canceled: List[TaskDef] = List.empty
  var pending: List[TaskDef] = List.empty

  /**
    * Tells the runner / framework that the testing is finished
    */
  def testingFinished(): Unit = {
    if (runner != null) {
      runner.done()
    }
  }

  override def toString: String = {
    "ScalaJSTestStatus for " + framework.name + " : " +
      "\nRunner : " + runner +
      "\nAll : " + all.map(t => t.taskDef().fullyQualifiedName()).mkString +
      "\nSuccess : " + succeeded.map(t => t.fullyQualifiedName()).mkString +
      "\nError : " + errored.map(t => t.fullyQualifiedName()).mkString +
      "\nFail : " + failed.map(t => t.fullyQualifiedName()).mkString +
      "\nSkip : " + skipped.map(t => t.fullyQualifiedName()).mkString +
      "\nIgnored : " + ignored.map(t => t.fullyQualifiedName()).mkString +
      "\nCanceled : " + canceled.map(t => t.fullyQualifiedName()).mkString +
      "\nPending : " + pending.map(t => t.fullyQualifiedName()).mkString
  }
}
