import org.gradle.api.plugins.jvm.JvmTestSuite
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc)
    `java-library`
    `maven-publish`
    `signing`
}

base {
    archivesName = "terminal-window-gui"
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(25)
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        allWarningsAsErrors.set(true)
        extraWarnings.set(true)
    }
}

val lwjglVersion = libs.versions.lwjgl.get()
val lwjglNativesClassifier = rootProject.extra["lwjglNativesClassifier"] as String

// Resolvable only: this must never extend runtimeElements or the published POM.
val natives =
    configurations.create("natives") {
        description = "LWJGL natives for the build host"
        isCanBeConsumed = false
        isCanBeResolved = true
    }

dependencies {
    api(libs.terminal.window)
    implementation(kotlin("stdlib"))

    natives("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNativesClassifier")

    testImplementation(libs.kotlin.test)
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

configurations.named("runtimeClasspath") {
    extendsFrom(natives)
}

configurations.named("testRuntimeOnly") {
    extendsFrom(natives)
}

val updateGoldens = providers.gradleProperty("updateGoldens").getOrElse("false")

tasks.test {
    useJUnitPlatform {
        excludeTags("long")
    }
    systemProperty("updateGoldens", updateGoldens)
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
                implementation(libs.kotlin.test)
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.platform.launcher)
            }

            targets {
                all {
                    testTask.configure {
                        useJUnitPlatform {
                            includeTags("gl")
                            excludeTags("long")
                        }
                        maxParallelForks = 1
                        shouldRunAfter(tasks.test)
                    }
                }
            }
        }
    }
}

configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(natives)
}

// The GL suite exercises internal resource types as part of the library.
kotlin.target.compilations.getByName("integrationTest").associateWith(
    kotlin.target.compilations.getByName("main"),
)

// The default suites are the minimum acceptance run. Long tests are tagged
// `long` and only execute in `fullUnitTest` / `fullIntegrationTest` / `fullTest`.
val fullUnitTest =
    tasks.register<Test>("fullUnitTest") {
        group = "verification"
        description = "Runs every unit test, including @Tag(\"long\") tests."
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform()
        systemProperty("updateGoldens", updateGoldens)
    }

val fullIntegrationTest =
    tasks.register<Test>("fullIntegrationTest") {
        group = "verification"
        description = "Runs every GL integration test, including @Tag(\"long\") tests."
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        useJUnitPlatform {
            includeTags("gl")
        }
        maxParallelForks = 1
        shouldRunAfter(tasks.test)
    }

val minimumAcceptanceTest =
    tasks.register("minimumAcceptanceTest") {
        group = "verification"
        description = "Runs the unit suite and the minimum GL acceptance suite."
        dependsOn(tasks.test, tasks.named("integrationTest"))
    }

val fullTest =
    tasks.register("fullTest") {
        group = "verification"
        description = "Runs every unit and GL test, including long-running tests."
        dependsOn(fullUnitTest, fullIntegrationTest)
    }

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(mapOf("ktlint_standard_package-name" to "disabled"))
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dokka {
    dokkaPublications.configureEach {
        failOnWarning.set(true)
    }
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(setOf(VisibilityModifier.Public))
        reportUndocumented.set(true)
    }
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.psiwray.organization.terminal_window_gui")
    }
}

val javadocJar =
    tasks.register<Jar>("javadocJar") {
        description = "A Javadoc JAR containing Dokka Javadoc"
        from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "terminal-window-gui"
            artifact(javadocJar)
            pom {
                name.set("Terminal Window GUI")
                description.set("Cell-grid GUI components rendered through terminal-window.")
                url.set("https://github.com/psiwray-corp/terminal-window-gui")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                developers {
                    developer {
                        id.set("psiwray")
                        name.set("Luca Cavallari")
                        email.set("development@psiwray.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:psiwray-corp/terminal-window-gui.git")
                    url.set("https://github.com/psiwray-corp/terminal-window-gui")
                }
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

// Release signing is local-only. The key stays in the maintainer's GnuPG
// keyring; gpg-agent prompts for the passphrase. No signing secrets belong in
// GitHub Actions or Forgejo.
signing {
    useGpgCmd()
    sign(tasks["jar"], tasks["sourcesJar"], tasks["javadocJar"])
    if (providers.gradleProperty("release").isPresent) {
        sign(publishing.publications["mavenJava"])
    }
    isRequired = providers.gradleProperty("release").isPresent
}

tasks.register("sign") {
    group = "signing"
    description = "Signs the standalone JAR, sources JAR, and Javadoc JAR."
    dependsOn("signJar", "signSourcesJar", "signJavadocJar")
}
