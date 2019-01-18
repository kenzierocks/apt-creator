package net.octyl.aptcreator

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                annotationProcessor files($classpath)
            }
        """.trimIndent())
        return buildFile
    }

    private inline fun singleCompileRound(testProjectDir: Path, files: (Path) -> Unit) {
        testProjectDir.newBuildFile()

        val srcDir = testProjectDir.resolve("src/main/java")
        Files.createDirectories(srcDir)
        files(srcDir)

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("-Si", "compileJava")
                .forwardOutput()
                .build()!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")!!.outcome)
    }

    @Test
    @DisplayName("doesn't break compile when applied without Kotlin")
    fun noBrokenCompile(
            @TempDirectory.TempDir testProjectDir: Path
    ) {
        singleCompileRound(testProjectDir) { srcDir ->
            srcDir.resolve("Main.java").write("""
                @net.octyl.aptcreator.GenerateCreator
                class Main {
                }
            """.trimIndent())
        }

        assertTrue(Files.exists(testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )))
    }

    @Test
    @DisplayName("does not copy annotations by default")
    fun noDefaultAnnotationCopy(
            @TempDirectory.TempDir testProjectDir: Path
    ) {
        singleCompileRound(testProjectDir) { srcDir ->
            srcDir.resolve("Main.java").write("""
                @net.octyl.aptcreator.GenerateCreator
                @javax.inject.Singleton
                class Main {
                }
            """.trimIndent())
        }

        val mainCreatorJava = testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )
        assertTrue("Generated MainCreator file") { Files.exists(mainCreatorJava) }

        val content = Files.newBufferedReader(mainCreatorJava, StandardCharsets.UTF_8)
                .use(Reader::readText)
        assertFalse("Annotation should not be copied. File content: $content") {
            content.contains("javax.inject.Singleton")
        }
    }

    @Test
    @DisplayName("can enable annotation copying")
    fun canCopyAnnotations(
            @TempDirectory.TempDir testProjectDir: Path
    ) {
        singleCompileRound(testProjectDir) { srcDir ->
            srcDir.resolve("Main.java").write("""
                @net.octyl.aptcreator.GenerateCreator
                @net.octyl.aptcreator.GenerateCreator.CopyAnnotations
                @javax.inject.Singleton
                class Main {
                }
            """.trimIndent())
        }

        val mainCreatorJava = testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )
        assertTrue("Generated MainCreator file") { Files.exists(mainCreatorJava) }

        val content = Files.newBufferedReader(mainCreatorJava, StandardCharsets.UTF_8)
                .use(Reader::readText)
        assertTrue("Did not copy annotations. File content: $content") {
            content.contains("javax.inject.Singleton") &&
                    content.contains("@Singleton\npublic final class")
        }
        listOf("GenerateCreator", "CopyAnnotations").forEach {
            assertFalse("$it should not be copied. File content: $content") {
                content.contains("$it[^ ]")
            }
        }
    }

    @Test
    @DisplayName("can enable annotation copying and exclude some")
    fun canCopyAnnotationsAndExclude(
            @TempDirectory.TempDir testProjectDir: Path
    ) {
        singleCompileRound(testProjectDir) { srcDir ->
            srcDir.resolve("Main.java").write("""
                import net.octyl.aptcreator.GenerateCreator;

                @GenerateCreator
                @GenerateCreator.CopyAnnotations(exclude = SuppressWarnings.class)
                @javax.inject.Singleton
                @SuppressWarnings("deprecation")
                class Main {
                }
            """.trimIndent())
        }

        val mainCreatorJava = testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )
        assertTrue("Generated MainCreator file") { Files.exists(mainCreatorJava) }

        val content = Files.newBufferedReader(mainCreatorJava, StandardCharsets.UTF_8)
                .use(Reader::readText)
        assertTrue("Did not copy annotations. File content: $content") {
            content.contains("javax.inject.Singleton") &&
                    content.contains("@Singleton\npublic final class")
        }
        assertFalse("Should not have copied SuppressWarnings. File content: $content") {
            content.contains("SuppressWarnings")
        }
    }

    @Test
    @DisplayName("is an incremental annotation processor")
    fun isIncrementalAnnotationProcessor(
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
                .build()!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")!!.outcome)
        assertTrue(Files.exists(testProjectDir.resolve(
                "build/classes/java/main/MainCreator.java"
        )))

        // add a new file, should be incremental still:
        srcDir.resolve("Helper.java").write("""
            class Helper {
            }
        """.trimIndent())

        val result2 = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("-Si", "compileJava")
                .build()!!

        assertEquals(TaskOutcome.SUCCESS, result2.task(":compileJava")!!.outcome)
        assertTrue("Incremental compilation of 1 classes completed" in result2.output)
    }
}