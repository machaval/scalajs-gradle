# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code.

This plugin also supports testing plain Scala code (no ScalaJS) using sbt-compatible testing frameworks.

Supports ScalaJS 1; default version: 1.9.0.

Plugin requires Gradle 7.5.0.

Plugin is written in Scala 2.


## Applying to a Gradle project ##

Plugin is [published](https://plugins.gradle.org/plugin/io.github.machaval.scalajs)
on the Gradle Plugin Portal. To apply it to a Gradle project:

```groovy
plugins {
  id 'io.github.machaval.scalajs' version '2.0.2'
}
```

```groovy
dependencies {
  scalajs "org.scala-js:scalajs-linker_2.12:$scalaJsVersion"
}
```


### Compiler ####
And depending on the `target` property value it uses the right compiler. 
If target value is `-Ptarget=js`  then the plugin will inject into the scala compiler the scalaJs compiler. So it will generate the sjs files in the target folder. If the `target=jvm` then 
it will not inject the scalajs compiler.


### Dependencies ###
Also if `js` is the target it will adopt the implementation dependencies and change it to be the one for scalajs.


### Source set ###
 
In order to support multitarget modules the plugin allows to change the source set to have specific clases for each target. 
If target is `js` then the `src/main/js` target will be added to the source set. If target is `jvm` then the target is `src/main/jvm` is added



