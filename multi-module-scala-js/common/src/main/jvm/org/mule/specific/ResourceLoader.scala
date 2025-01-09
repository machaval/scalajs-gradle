package org.mule.specific

class ResourceLoader {

  def getResource(name: String): String = {
    new String(classOf[ResourceLoader].getClassLoader.getResourceAsStream(name).readAllBytes())
  }

}
