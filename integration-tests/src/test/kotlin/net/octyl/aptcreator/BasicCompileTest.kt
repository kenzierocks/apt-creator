package net.octyl.aptcreator

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(TempDirectory::class)
class BasicCompileTest {

    private fun Path.write(text: String) {
        Files.write(this, text.split('\n'), StandardCharsets.UTF_8)
    }

    private fun Path.newBuildFile(): Path {
        val buildFile = resolve("build.gradle")!!
        // pull the classpath provided to us:
        val classpath = APT_CREATOR_CLASSPATH.joinToString("\", \"", "\"", "\"")
        buildFile.write("""
            plugins {
                id "java"
            }

            dependencies {
                implementation files($classpath)
            }
        """.trimIndent())
        return buildFile
    }

    @Test
    @DisplayName("doesn't break compile when applied without Kotlin")
    fun noBrokenCompile(
            @TempDirectory.TempDir testProjectDir: Path
    ) {
        testProjectDir.newBuildFile()

        val srcDir = testProjectDir.resolve("src/main/java")
        Files.createDirectories(srcDir)
        srcDir.resolve("Main.java").write("""
            @net.octyl.aptcreator.GenerateCreator
            class Main {
            }
        """.trimIndent())

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("-Si", "compileJava")
                .forwardOutput()
                .build()!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")!!.outcome)
        assertTrue(Files.exists(testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )))
    }
}