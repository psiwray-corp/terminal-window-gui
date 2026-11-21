package com.psiwray.organization.terminal_window_gui.testkit

import java.io.File
import kotlin.test.assertEquals

/**
 * Golden-file assertions for harness output.
 *
 * Goldens live under `library/src/test/resources/golden`. Set the Gradle
 * `-PupdateGoldens` property to rewrite them locally; CI never updates them.
 */
object GoldenAssert {
    private val goldenDirectory: File = File("src/test/resources/golden")

    /** Asserts [actual] against the golden named [name]. */
    fun assertGolden(
        name: String,
        actual: String,
    ) {
        val file = File(goldenDirectory, name)
        if (updateRequested()) {
            file.parentFile.mkdirs()
            file.writeText(actual)
            return
        }
        require(file.isFile) { "missing golden ${file.absolutePath}" }
        assertEquals(file.readText(), actual)
    }

    /** Asserts [rows] as newline-terminated structural rows. */
    fun assertRows(
        name: String,
        rows: List<String>,
    ) {
        assertGolden(name, rows.joinToString(separator = "\n", postfix = "\n"))
    }

    /** Asserts [styles] as newline-terminated style rows. */
    fun assertStyles(
        name: String,
        styles: List<String>,
    ) {
        assertGolden(name, styles.joinToString(separator = "\n", postfix = "\n"))
    }

    private fun updateRequested(): Boolean =
        System.getProperty("updateGoldens")?.equals("true", ignoreCase = true) == true ||
            System.getenv("UPDATE_GOLDENS")?.equals("true", ignoreCase = true) == true
}
