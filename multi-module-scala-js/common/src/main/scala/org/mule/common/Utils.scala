package org.mule.common

import org.mule.specific.ResourceLoader
import org.parboiled2.Base64Parsing

object Utils {
  def name(name: String) = {
    name.toLowerCase
  }

  def test() = {
    Base64Parsing.customBlockDecoder.toString()
  }

  def resource(r: String): String = {
    new ResourceLoader().getResource(r)
  }
}
