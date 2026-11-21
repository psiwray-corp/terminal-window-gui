import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
}

val lwjglVersion = libs.versions.lwjgl.get()
val lwjglNativesClassifier = rootProject.extra["lwjglNativesClassifier"] as String

// Resolvable only: native artifacts stay on the example runtime classpath and
// never reach the library publication.
val natives =
    configurations.create("natives") {
        description = "LWJGL natives for the build host"
        isCanBeConsumed = false
        isCanBeResolved = true
    }

dependencies {
    implementation(project(":library"))
    implementation(libs.terminal.window)
    implementation(kotlin("stdlib"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    natives("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNativesClassifier")
    natives("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNativesClassifier")
}

configurations.named("runtimeClasspath") {
    extendsFrom(natives)
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

val runtimeJvmArgs = rootProject.extra["runtimeJvmArgs"] as List<String>

tasks.withType<Test>().configureEach {
    jvmArgs(runtimeJvmArgs)
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(runtimeJvmArgs)
}

/** Registers one runnable example and its headless smoke variant. */
fun registerExample(
    name: String,
    mainClass: String,
): TaskProvider<JavaExec> {
    val run =
        tasks.register<JavaExec>(name) {
            group = "application"
            description = "Runs the $name example."
            this.mainClass.set(mainClass)
            classpath = sourceSets["main"].runtimeClasspath
            workingDir = rootProject.projectDir
        }
    tasks.register<JavaExec>("smoke${name.replaceFirstChar { it.uppercase() }}") {
        group = "verification"
        description = "Runs the $name example in smoke mode."
        this.mainClass.set(mainClass)
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = rootProject.projectDir
        args("--smoke")
    }
    tasks.register<JavaExec>("screenshot${name.replaceFirstChar { it.uppercase() }}") {
        group = "verification"
        description = "Captures docs/assets/$name.png from the $name example."
        this.mainClass.set(mainClass)
        classpath = sourceSets["main"].runtimeClasspath
        workingDir = rootProject.projectDir
        args("--smoke", "--screenshot=docs/assets/$name.png")
    }
    return run
}

registerExample("gallery", "com.psiwray.organization.terminal_window_gui.examples.gallery.GalleryMainKt")
registerExample("todo", "com.psiwray.organization.terminal_window_gui.examples.todo.TodoMainKt")
registerExample("dashboard", "com.psiwray.organization.terminal_window_gui.examples.dashboard.DashboardMainKt")
registerExample("custom", "com.psiwray.organization.terminal_window_gui.examples.custom.CustomDemoKt")

tasks.register("smoke") {
    group = "verification"
    description = "Runs every example in smoke mode."
    dependsOn(
        tasks.named("smokeGallery"),
        tasks.named("smokeTodo"),
        tasks.named("smokeDashboard"),
        tasks.named("smokeCustom"),
    )
}

tasks.register("screenshots") {
    group = "verification"
    description = "Captures the gallery, TODO, dashboard, and custom-widget screenshots."
    dependsOn(
        tasks.named("screenshotGallery"),
        tasks.named("screenshotTodo"),
        tasks.named("screenshotDashboard"),
        tasks.named("screenshotCustom"),
    )
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
