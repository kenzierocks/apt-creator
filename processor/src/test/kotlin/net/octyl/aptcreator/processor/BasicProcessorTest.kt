package net.octyl.aptcreator.processor

import com.google.common.io.Resources
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import kotlin.test.Test

class BasicProcessorTest {

    private fun provideBasicCompileTest(vararg testSource: String, config: (Compiler.() -> Compiler)? = null): Compilation {
        return Compiler.javac()
                .withProcessors(AptCreatorProcessor())
                .apply { config?.invoke(this) }
                .compile(testSource.map { JavaFileObjects.forResource("sources/$it") })
    }

    private fun CompilationSubject.generatedFileEquivalent(resourceName: String) {
        // We need to vary Generated annotation based on current JVM
        val fileContent = Resources.getResource("targets/$resourceName")
                .openStream().use {
                    it.reader().readLines()
                }.let {
                    when {
                        System.getProperty("java.version").startsWith("1.") ->
                            it
                        else ->
                            replaceGeneratedImport(it)
                    }
                }

        val resourceFqn = resourceName.removeSuffix(".java").replace('/', '.')
        generatedSourceFile(resourceFqn)
                .hasSourceEquivalentTo(JavaFileObjects.forSourceLines(resourceFqn, fileContent))
    }

    @Test
    fun `generates with no parameters`() {
        val result = provideBasicCompileTest("NoParameters.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("NoParametersCreator.java")
    }

    @Test
    fun `generates with one parameter`() {
        val result = provideBasicCompileTest("OneParameter.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("OneParameterCreator.java")
    }

    @Test
    fun `generates with one nullable parameter`() {
        val result = provideBasicCompileTest("OneNullableParameter.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("OneNullableParameterCreator.java")
    }

    @Test
    fun `generates with one provided parameter`() {
        val result = provideBasicCompileTest("ProvidedParameter.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("ProvidedParameterCreator.java")
    }

    @Test
    fun `generates with one nullable provided parameter`() {
        val result = provideBasicCompileTest("NullableProvidedParameter.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("NullableProvidedParameterCreator.java")
    }

    @Test
    fun `supports renaming the entire creator class`() {
        val result = provideBasicCompileTest("NameOverride.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("NotTheClassName.java")
    }

    @Test
    fun `supports nested static classes`() {
        val result = provideBasicCompileTest("nesting/NestingSuccess.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("nesting/NestingSuccess_LevelOne_LevelTwoCreator.java")
    }

    @Test
    fun `doesn't support nested non-static classes`() {
        val result = provideBasicCompileTest("nesting/NestingFailByNonStatic.java")
        assertThat(result).failed()
        assertThat(result).hadErrorContaining("an enclosing instance")
    }

    @Test
    fun `supports multiple constructors`() {
        val result = provideBasicCompileTest("MultiConstructor.java")
        assertThat(result).succeededWithoutWarnings()
        assertThat(result)
                .generatedFileEquivalent("MultiConstructorCreator.java")
    }

    @Test
    fun `doesn't support multiple constructors with conflicting signatures`() {
        val result = provideBasicCompileTest("FailMultiConstructor.java")
        assertThat(result).failed()
        assertThat(result).hadErrorContaining("create() is already defined")
    }
}
