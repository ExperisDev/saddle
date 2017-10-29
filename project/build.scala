/**
 * Copyright (c) 2013 Saddle Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

import sbt._
import sbt.Keys._

object SaddleBuild extends sbt.Build {

  lazy val root =
    project(id = "saddle",
            settings = Seq(
              /* 'console' in root acts as if in core. */
              console <<= (console in core in Compile) { identity },
              publishArtifact := false
            ),
            base = file(".")) aggregate(core)

  lazy val core =
    project(id = "saddle-core",
            base = file("saddle-core"),
            settings = Seq(
              name := "saddle-core-fork",
              initialCommands := """
                |import org.joda.time.DateTime
                |import org.saddle._
                |import org.saddle.time._
                |import org.saddle.io._""".stripMargin('|'),
              unmanagedClasspath in(LocalProject("saddle-core"), Test) <++= (fullClasspath in(LocalProject("saddle-test-framework"), Test)),
              libraryDependencies <++= scalaVersion (v => Seq(
                "joda-time" % "joda-time" % "2.1",
                "org.joda" % "joda-convert" % "1.2",
                "org.scala-saddle" % "google-rfc-2445" % "20110304",
                "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.19",
                "org.apache.commons" % "commons-math" % "2.2",
                "it.unimi.dsi" % "fastutil" % "6.5.4",
                "it.unimi.dsi" % "dsiutils" % "2.0.15"
              ) ++ Shared.testDeps(v)),
              testOptions in Test += Tests.Argument("console", "junitxml")
            ))

  lazy val test_framework =
    project(
      id = "saddle-test-framework",
      base = file("saddle-test-framework"),
      settings = Seq(
        libraryDependencies <++= scalaVersion(v => Shared.testDeps(v, "compile"))
      )
    ) dependsOn (core)

  def project(id: String, base: File, settings: Seq[Project.Setting[_]] = Nil) =
    Project(id = id,
            base = base,
            settings = Project.defaultSettings ++ Shared.settings ++ settings)
}

object Shared {
  def testDeps(version: String, conf: String = "test") =
    Seq(
      "org.specs2" %% "specs2-core" % "3.8.6" % conf,
      "org.specs2" %% "specs2-scalacheck" % "3.8.6" % conf
    )


  val settings = Seq(
    organization := "io.github.pityka",
    publishArtifact in Test := false,
    pomExtra := (
      <url>https://github.com/pityka/saddle</url>
      <licenses>
        <license>
          <name>Apache 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:pityka/saddle.git</url>
        <connection>scm:git:git@github.com:pityka/saddle.git</connection>
      </scm>
      <developers>
        <developer>
          <id>adamklein</id>
          <name>Adam Klein</name>
          <url>http://blog.adamdklein.com</url>
        </developer>
        <developer>
          <id>chrislewis</id>
          <name>Chris Lewis</name>
          <email>chris@thegodcode.net</email>
          <url>http://www.thegodcode.net/</url>
          <organizationUrl>https://www.novus.com/</organizationUrl>
          <timezone>-5</timezone>
        </developer>
        <developer>
          <id>pityka</id>
          <name>Istvan Bartha</name>
        </developer>
      </developers>
    ),
    scalaVersion := "2.12.4",
    version := "1.3.4-fork1",
    crossScalaVersions := Seq( "2.11.11"),
    scalacOptions := Seq("-deprecation", "-unchecked") // , "-Xexperimental"),
    // compile <<= (compile in Compile) dependsOn (compile in Test)
  )
}
