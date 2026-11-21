package com.psiwray.organization.terminal_window_gui

import com.psiwray.organization.terminal_window_gui.input.GuiEvent
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Freezes the source-level public surface.
 *
 * The inventory is read from the compiled test classpath. Kotlin visibility is
 * used so `internal` bytecode-public classes do not enter the inventory. The
 * source check covers typealiases and proves that public top-level declarations
 * carry explicit visibility; `explicitApi()` covers public members.
 */
class ApiSurfaceTest {
    @Test
    fun exactPublicTypeInventory() {
        val expected =
            javaClass
                .getResourceAsStream("/public-api.txt")!!
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toSet()
        assertEquals(expected, publicKotlinTypes(), "public type inventory changed")
    }

    @Test
    fun publicDeclarationsUseExplicitVisibilityOrCompilerGate() {
        val sources = sourceFiles()
        val topLevelDeclaration =
            Regex(
                "^(?:class|interface|object|enum class|data class|data object|sealed class|" +
                    "sealed interface|abstract class|open class|annotation class|value class|" +
                    "fun|val|var|typealias)\\b",
            )
        for (file in sources) {
            for ((lineNumber, line) in Files.readAllLines(file).withIndex()) {
                if (!topLevelDeclaration.containsMatchIn(line)) {
                    continue
                }
                assertTrue(
                    line.startsWith("public ") || line.startsWith("internal ") || line.startsWith("private "),
                    "${file.name}:${lineNumber + 1} has no explicit visibility: $line",
                )
            }
        }
        val build = Path.of("build.gradle.kts")
        assertTrue(build.toFile().isFile, "test working directory must be :library")
        assertTrue(
            Files.readString(build).contains("explicitApi()"),
            "explicitApi() is required for member-level explicit visibility",
        )
        val aliases = setOf("Field", "Tabs")
        for (alias in aliases) {
            assertTrue(
                sources.any { file ->
                    Files.readAllLines(file).any { it.trimStart().startsWith("public typealias $alias =") }
                },
                "missing public typealias $alias",
            )
        }
    }

    @Test
    fun noInternalOrLwjglTypesInPublicSignatures() {
        val offenders = ArrayList<String>()
        for (type in publicClasses()) {
            offenders += inspectType(type, "type ${type.qualifiedName}")
            for (supertype in type.supertypes) {
                offenders += inspectKType(supertype, "supertype of ${type.qualifiedName}")
            }
            for (typeParameter in type.typeParameters) {
                for (bound in typeParameter.upperBounds) {
                    offenders += inspectKType(bound, "type parameter of ${type.qualifiedName}")
                }
            }
            for (constructor in type.constructors.filter { it.visibility == KVisibility.PUBLIC }) {
                for (parameter in constructor.valueParameters) {
                    offenders += inspectKType(parameter.type, "${type.qualifiedName} constructor")
                }
            }
            for (function in type.declaredMemberFunctions.filter { it.visibility == KVisibility.PUBLIC }) {
                offenders += inspectKType(function.returnType, "${type.qualifiedName}.${function.name}")
                for (parameter in function.valueParameters) {
                    offenders += inspectKType(parameter.type, "${type.qualifiedName}.${function.name}")
                }
            }
            for (property in type.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }) {
                offenders += inspectKType(property.returnType, "${type.qualifiedName}.${property.name}")
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "forbidden internal/LWJGL types in public signatures:\n${offenders.joinToString("\n")}",
        )
    }

    @Test
    fun noPublicInlineFunctions() {
        val inline =
            publicClasses().flatMap { type ->
                type.declaredMemberFunctions
                    .filter { it.visibility == KVisibility.PUBLIC && it.isInline }
                    .map { "${type.qualifiedName}.${it.name}" }
            }
        assertTrue(inline.isEmpty(), "public inline functions: ${inline.joinToString()}")
    }

    @Test
    fun sealedEventHierarchyIsClosed() {
        assertEquals(
            setOf(
                "GuiCharEvent",
                "GuiFocusEvent",
                "GuiKeyEvent",
                "GuiPointerButtonEvent",
                "GuiPointerMoveEvent",
                "GuiResizeEvent",
                "GuiScrollEvent",
            ),
            GuiEvent::class.sealedSubclasses.map { it.simpleName }.toSet(),
        )
        assertTrue(GuiEvent::class.isSealed)
    }

    @Test
    fun noPublicMutableSizeFields() {
        val forbidden = setOf("preferredSize", "minimumSize", "maximumSize")
        val fields =
            publicClasses().flatMap { type ->
                type.java.declaredFields
                    .filter { field ->
                        field.name in forbidden &&
                            java.lang.reflect.Modifier
                                .isPublic(field.modifiers)
                    }.map { "${type.qualifiedName}.${it.name}" }
            }
        assertTrue(fields.isEmpty(), "public mutable size fields: ${fields.joinToString()}")
    }

    private fun publicKotlinTypes(): Set<String> = publicClasses().mapNotNull { it.qualifiedName }.toSet()

    private fun publicClasses(): List<KClass<*>> {
        val root = classRoot()
        val paths = Files.walk(root)
        val names =
            try {
                paths
                    .filter { it.name.endsWith(".class") }
                    .map {
                        root
                            .relativize(it)
                            .toString()
                            .removeSuffix(".class")
                            .replace('\\', '.')
                            .replace('/', '.')
                    }.toList()
            } finally {
                paths.close()
            }
        return names
            .filter { it.substringAfterLast('.').isNotEmpty() }
            .filter { !it.contains(".internal.") }
            .filter { !it.substringAfterLast('.').startsWith("WhenMappings") }
            .mapNotNull { name ->
                val clazz = Class.forName(name, false, javaClass.classLoader)
                if (clazz.isSynthetic || clazz.isAnonymousClass || clazz.isLocalClass) {
                    null
                } else {
                    clazz.kotlin
                }
            }.filter { type ->
                type.visibility == KVisibility.PUBLIC &&
                    !type.isCompanion &&
                    !isFileFacade(type)
            }.sortedBy { it.qualifiedName }
    }

    private fun classRoot(): Path =
        Path.of(
            GuiSurface::class.java.protectionDomain.codeSource.location
                .toURI(),
        )

    private fun isFileFacade(type: KClass<*>): Boolean {
        val metadata = type.java.getAnnotation(Metadata::class.java) ?: return false
        return metadata.kind == 2
    }

    private fun inspectType(
        type: KClass<*>,
        where: String,
    ): List<String> {
        val offenders = ArrayList<String>()
        val name = type.qualifiedName ?: type.java.name
        if (name.contains(".internal.") || name.startsWith("org.lwjgl.")) {
            offenders += "$where -> $name"
        }
        return offenders
    }

    private fun inspectKType(
        type: KType,
        where: String,
    ): List<String> {
        val offenders = ArrayList<String>()
        val classifier = type.classifier
        if (classifier is KClass<*>) {
            offenders += inspectType(classifier, where)
        }
        for (argument in type.arguments) {
            argument.type?.let { nested ->
                offenders += inspectKType(nested, where)
            }
        }
        return offenders
    }

    private fun sourceFiles(): List<Path> {
        val root = Path.of("src/main/kotlin")
        assertTrue(root.toFile().isDirectory, "missing main source tree at $root")
        return Files.walk(root).use { paths ->
            paths.filter { it.name.endsWith(".kt") }.toList()
        }
    }
}
