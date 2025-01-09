package org.mule.js

import org.mule.common.Utils

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportTopLevel


@JSExportTopLevel("JSAPI")
object JSAPI {

  @JSExport
  def createName() = {
    Utils.name("Mariano")
  }

  @JSExport
  def test() = {
    Utils.test()
  }

}
