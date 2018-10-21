import com.techshroom.inciseblue.InciseBlueExtension
import net.researchgate.release.ReleaseExtension
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.researchgate.release") version "2.7.0"
    id("com.techshroom.incise-blue") version "0.1.3"
    idea
}

inciseBlue.ide()

val arb = tasks.named("afterReleaseBuild")
subprojects {
    apply(plugin = "com.techshroom.incise-blue")
    apply(plugin = "java")

    configure<InciseBlueExtension> {
        ide()
        license()
        maven {
            projectDescription = "Annotation Processor for generating creator classes"
            coords("kenzierocks", "apt-creator")
            artifactName = "${rootProject.name}-${project.name}"
        }
        util {
            setJavaVersion("1.8")
        }
    }
    plugins.withId("maven-publish") {
        arb.configure {
            dependsOn(tasks.named("publish"))
        }
    }
}

tasks.register("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
