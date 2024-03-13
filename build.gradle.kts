plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10" // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.getyourguide"
version = "1.2.2023.3"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2023.3")
    plugins.set(listOf("Kotlin", "java", "gradle"))

    tasks {
        buildSearchableOptions {
            enabled = false
        }
    }
}

tasks {
    runIde {
        // Absolute path to installed target Android Studio to use as
        // IDE Development Instance (the "Contents" directory is macOS specific):
        ideDir.set(file("/Applications/Android Studio.app/Contents"))
    }
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
