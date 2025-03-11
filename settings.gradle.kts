pluginManagement {
  repositories {
    apply(from = "${rootProject.projectDir}/gradle/utils/token-utils.gradle")
    val fetchToken = (extra["fetchCodeArtifactTokenMethod"] as groovy.lang.Closure<*>).call() as String
    maven {
      name = "codeartifact"
      url =
              uri("https://getyourguide-130607246975.d.codeartifact.eu-central-1.amazonaws.com/maven/private/")
      credentials {
        username = "aws"
        password = fetchToken
      }
    }
  }
}

plugins {
    id("com.getyourguide.libs.gradle.develocity.configuration") version "4.14.2"
}

rootProject.name = "paparazzi"