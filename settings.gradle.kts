pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

val centralUsername = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orElse("").get()
val centralPassword = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orElse("").get()

nmcpSettings {
    centralPortal {
        username.set(centralUsername)
        password.set(centralPassword)
        publishingType.set("USER_MANAGED")
        publicationName.set("terminal-window-gui:${providers.gradleProperty("version").get()}")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "terminal-window-gui"
include(":library", ":examples")
