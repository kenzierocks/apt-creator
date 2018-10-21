import com.techshroom.inciseblue.InciseBlueExtension
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.researchgate.release") version "2.7.0" apply false
    id("com.techshroom.incise-blue") version "0.1.3"
    idea
}

inciseBlue.ide()

subprojects {
    apply(plugin = "net.researchgate.release")
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
}
