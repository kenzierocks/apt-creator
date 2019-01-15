import com.techshroom.inciseblue.InciseBlueExtension
import com.techshroom.inciseblue.commonLib
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("jvm") version "1.3.11"
}

evaluationDependsOn(":processor")

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test>().named("test").configure {
    val jarTask = project(":processor").tasks.getByName<Jar>("jar")
    dependsOn(jarTask)
    val classpath = jarTask.outputs.files.asSequence() +
            project(":processor").configurations.getByName("runtimeClasspath").files
    this.systemProperty("apt.creator.classpath",
            classpath.joinToString(File.pathSeparator) { it.absolutePath })
}

configure<InciseBlueExtension> {
    util.enableJUnit5()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    commonLib("org.junit.jupiter", "junit-jupiter", "5.3.2") {
        implementation(lib("api"))
        implementation(lib("params"))
        implementation(lib("engine"))
    }
    implementation("org.junit-pioneer", "junit-pioneer", "0.3.0")

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
}