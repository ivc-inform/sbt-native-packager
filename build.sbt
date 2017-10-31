
sbtPlugin := true

name := "sbt-native-packager"
organization := "com.typesafe.sbt"

scalaVersion in Global := "2.12.4"

// crossBuildingSettings
//crossSbtVersions := Vector("0.13.16", "1.0.0")

scalacOptions in Compile ++= Seq("-deprecation")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// put jdeb on the classpath for scripted tests
classpathTypes += "maven-plugin"
libraryDependencies ++= Seq(
    "org.apache.commons" % "commons-compress" % "1.15",
    // for jdkpackager
    "org.apache.ant" % "ant" % "1.10.1",
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.scala-sbt" %% "io" % "1.1.0",
    // these dependencies have to be explicitly added by the user
    // FIXME temporary remove the 'provided' scope. SBT 1.0.0-M6 changed the resolving somehow
    "com.spotify" % "docker-client" % "8.9.1" /* % "provided" */ ,
    "org.vafer" % "jdeb" % "1.5" /*% "provided"*/ artifacts Artifact("jdeb", "jar", "jar"),
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)

// configure github page
enablePlugins(SphinxPlugin, SiteScaladocPlugin)

GhpagesPlugin.ghpagesProjectSettings

git.remoteRepo := "git@github.com:sbt/sbt-native-packager.git"

// scripted test settings
ScriptedPlugin.projectSettings
scriptedLaunchOpts += "-Dproject.version=" + version.value

// Release configuration
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    releaseStepCommandAndRemaining("^ test"),
    releaseStepCommandAndRemaining("^ scripted universal/* debian/* rpm/* docker/* ash/* jar/* bash/* jdkpackager/*"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("^ publishSigned"),
    setNextVersion,
    commitNextVersion,
    pushChanges,
    releaseStepTask(GhpagesPlugin.autoImport.ghpagesPushSite)
)

GhpagesPlugin.autoImport.ghpagesNoJekyll := true
GhpagesPlugin.autoImport.ghpagesBranch := name.value

// bintray config
//bintrayOrganization := Some("sbt")
//bintrayRepository := "sbt-plugin-releases"

// ci commands
addCommandAlias("validateFormatting", "; scalafmt::test ; test:scalafmt::test ; sbt:scalafmt::test")
addCommandAlias("validate", "; clean ; update ; test")

// List all scripted test separately to schedule them in different travis-ci jobs.
// Travis-CI has hard timeouts for jobs, so we run them in smaller jobs as the scripted
// tests take quite some time to run.
// Ultimatley we should run only those tests that are necessary for a change
addCommandAlias("validateUniversal", "scripted universal/*")
addCommandAlias("validateJar", "scripted jar/*")
addCommandAlias("validateBash", "scripted bash/*")
addCommandAlias("validateAsh", "scripted ash/*")
addCommandAlias("validateRpm", "scripted rpm/*")
addCommandAlias("validateDebian", "scripted debian/*")
addCommandAlias("validateDocker", "scripted docker/*")
addCommandAlias("validateDockerUnit", "scripted docker/staging docker/entrypoint docker/ports docker/volumes")
addCommandAlias("validateJdkPackager", "scripted jdkpackager/*")
// travis ci's jdk8 version doesn't support nested association elements.
// error: Caused by: class com.sun.javafx.tools.ant.Info doesn't support the nested "association" element.
addCommandAlias(
    "validateJdkPackagerTravis",
    "scripted jdkpackager/test-package-minimal jdkpackager/test-package-mappings"
)

// TODO check the cygwin scripted tests and run them on appveyor
addCommandAlias("validateWindows", "; test-only * -- -n windows;scripted universal/dist universal/stage windows/*")
