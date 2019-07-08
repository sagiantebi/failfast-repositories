package com.sagiantebi.failfastrepos

import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

class FailFastRepositoriesPluginTest {

    //// Classic unit tests

    @Test
    fun `plugin applies to project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.sagiantebi.failfast.repos")
        //just make sure the plugin applies for now
    }

    //// Functional tests below. quite a bit of boiler plate.

    val testProjectDir = TemporaryFolder()

    var settingsFile: File? = null
    var buildFile: File? = null

    internal val pluginClassPath by lazy {
        var pluginClassPath: List<File> = Collections.emptyList()

        this.javaClass.classLoader.resources("plugin-classpath.txt").findFirst().ifPresent {
            pluginClassPath = it.readText().split("\n").stream().map { u -> File(u) }.collect(Collectors.toList())
        }

        this.javaClass.classLoader.resources("plugin-classpath.txt").findFirst().let {
            pluginClassPath.map { it.absolutePath.replace("\\", "\\\\") }.map { "'$it'" }.joinToString(separator = ",")
        }
    }

    @Throws(IOException::class)
    @BeforeEach
    internal fun setup() {
        testProjectDir.create()
        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")
    }

    @AfterEach
    internal fun destroy() {
        testProjectDir.delete()
    }

    @Test
    fun `when creating a new project with proper extension and none working failfast the 2 regular repos are added`() {
        testInsideNewProject(
            """
            buildscript {
                dependencies {
                    classpath files($pluginClassPath)
                }
            }
            apply plugin: "com.sagiantebi.failfast.repos"
            
            failfastRepositories {
                failFast(ofRepository("http://a.b.c.d.e.f.g.h"))
                fallback(Arrays.asList(ofRepository("https://maven.google.com"), ofRepository("https://jcenter.bintray.com"))) 
            }
            
            afterEvaluate {
                println("repo size is \$\{repositories.size()}")
            }
            
        """.trimIndent().replace("\\$", "$").replace("\\{", "{")
            , 2
        )
    }

    @Test
    fun `when creating a new project with proper extension and working failfast the failfast repo is added`() {
        testInsideNewProject(
            """
            buildscript {
                dependencies {
                    classpath files($pluginClassPath)
                }
            }
            apply plugin: "com.sagiantebi.failfast.repos"
            
            failfastRepositories {
                failFast(ofRepository("https://repo.maven.apache.org/maven2/"))
                fallback(Arrays.asList(ofRepository("https://maven.google.com"), ofRepository("https://jcenter.bintray.com"))) 
            }
            
            afterEvaluate {
                println("repo size is \$\{repositories.size()}")
            }
            
        """.trimIndent().replace("\\$", "$").replace("\\{", "{")
            , 1
        )
    }


    private fun testInsideNewProject(buildScript: String, expectedRepoSize: Int) {
        settingsFile?.let { writeFile(it, "rootProject.name = 'hello-world'") }

        buildFile?.let { writeFile(it, buildScript) }

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments("--debug")
            .build()


        assertNotNull(result)
        assertTrue(result.output.contains("repo size is ${expectedRepoSize}"), "output is ${result.output}")
    }

    @Throws(IOException::class)
    private fun writeFile(destination: File, content: String) {
        var output: BufferedWriter? = null
        try {
            output = BufferedWriter(FileWriter(destination))
            output.write(content)
        } finally {
            output?.close()
        }
    }

}