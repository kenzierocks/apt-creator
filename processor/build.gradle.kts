import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.techshroom.inciseblue.InciseBlueExtension
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("jvm") version "1.2.71"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

dependencies {
    "implementation"(group = "com.squareup", name = "javapoet", version = "1.11.1")
    "implementation"(group = "com.google.auto", name = "auto-common", version = "0.6")
    "implementation"(group = "javax.inject", name = "javax.inject", version = "1")
    "implementation"(project(":annotations"))
    "implementation"(kotlin("stdlib-jdk8"))

    "testImplementation"(group = "junit", name = "junit", version = "4.12")
    "testImplementation"(group = "com.google.testing.compile", name = "compile-testing", version = "0.15")
    "testImplementation"(kotlin("test-junit"))
    "testImplementation"(files(Jvm.current().toolsJar ?: "No tools.jar for current JVM?"))
}

tasks.withType<ShadowJar>().named("shadowJar").configure {
    minimize()
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    dependencies {
        include {
            it.moduleGroup == "org.jetbrains.kotlin"
        }
    }
    relocate("kotlin", "aptcreator.kotlin")
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
