plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.24" // https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
    id("org.jetbrains.intellij.platform") version "2.0.1" // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
}

group = "com.getyourguide"
version = "1.2.2024.2"

val ideaVersion = "2024.2"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(ideaVersion)
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.java", "com.intellij.gradle")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

tasks {
    runIde {
        jvmArgs("-Xmx1g")
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
        sinceBuild.set("241")
        untilBuild.set("242.*")
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
