plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
}

// One host classification for :examples. Native artifacts are runtime-only
// and never published by :library.
val hostOs = System.getProperty("os.name").lowercase()
val hostArch = System.getProperty("os.arch").lowercase()
val isX64 = hostArch == "x86_64" || hostArch == "amd64"
val isArm64 = hostArch == "aarch64" || hostArch == "arm64"
val isMac = hostOs.contains("mac") || hostOs.contains("darwin")
val lwjglNativesClassifier =
    when {
        hostOs.contains("linux") && isX64 -> "natives-linux"
        hostOs.contains("linux") && isArm64 -> "natives-linux-arm64"
        hostOs.contains("windows") && isX64 -> "natives-windows"
        hostOs.contains("windows") && isArm64 -> "natives-windows-arm64"
        isMac && isX64 -> "natives-macos"
        isMac && isArm64 -> "natives-macos-arm64"
        else -> error("Unsupported host for LWJGL natives: $hostOs/$hostArch")
    }
extra["lwjglNativesClassifier"] = lwjglNativesClassifier

val runtimeJvmArgs =
    buildList {
        add("-Djava.awt.headless=true")
        add("--enable-native-access=ALL-UNNAMED")
        if (isMac) {
            add("-XstartOnFirstThread")
        }
    }
extra["runtimeJvmArgs"] = runtimeJvmArgs

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "library/*.gradle.kts", "examples/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(mapOf("ktlint_standard_package-name" to "disabled"))
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs the minimum GL integration suite."
    dependsOn(":library:integrationTest")
}

tasks.register("minimumAcceptanceTest") {
    group = "verification"
    description = "Runs the unit suite and the minimum GL integration suite."
    dependsOn(":library:minimumAcceptanceTest")
}

tasks.register("fullTest") {
    group = "verification"
    description = "Runs every unit and GL test, including long-running tests."
    dependsOn(":library:fullUnitTest", ":library:fullIntegrationTest")
}
