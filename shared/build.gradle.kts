import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val generatedChallengeCommonMainDirectory = layout.buildDirectory.dir("generated/challenge/commonMain/kotlin")
val generatedChallengeCommonTestDirectory = layout.buildDirectory.dir("generated/challenge/commonTest/kotlin")

val generateChallengeMaterial = tasks.register("generateChallengeMaterial", GenerateChallengeMaterialTask::class) {
    commonMainOutputDirectory.set(generatedChallengeCommonMainDirectory)
    commonTestOutputDirectory.set(generatedChallengeCommonTestDirectory)
    localPropertiesFile.set(rootProject.layout.projectDirectory.file("local.properties"))
    challengeFlag.set(providers.environmentVariable("CHALLENGE_FLAG"))
    challengeTrigger.set(providers.environmentVariable("CHALLENGE_TRIGGER"))
}

tasks.configureEach {
    if (name.startsWith("compile") || name.startsWith("link") || name.startsWith("test")) {
        dependsOn(generateChallengeMaterial)
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
       namespace = "kr.foundcake.calculator.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generatedChallengeCommonMainDirectory)
        }
        commonTest {
            kotlin.srcDir(generatedChallengeCommonTestDirectory)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

abstract class GenerateChallengeMaterialTask : DefaultTask() {
    @get:OutputDirectory
    abstract val commonMainOutputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val commonTestOutputDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val challengeFlag: Property<String>

    @get:Input
    @get:Optional
    abstract val challengeTrigger: Property<String>

    @get:InputFile
    @get:Optional
    abstract val localPropertiesFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val localProperties = readLocalProperties()
        val configuredFlag = challengeFlag.orNull ?: localProperties.getProperty(CHALLENGE_FLAG_PROPERTY)
        val configuredTrigger = challengeTrigger.orNull
            ?: localProperties.getProperty(CHALLENGE_TRIGGER_PROPERTY)
            ?: DEFAULT_TRIGGER
        val exact = compact(configuredTrigger)
        val bytes = configuredFlag?.encodeToByteArray()?.map { it.toInt() and 0xFF } ?: DEFAULT_FLAG_BYTES

        validateFlag(bytes)
        resetOutputDirectories()
        writeGeneratedMaterial(bytes, exact)
    }

    private fun readLocalProperties(): Properties {
        val file = localPropertiesFile.orNull?.asFile
        return Properties().apply {
            if (file != null && file.isFile) {
                file.inputStream().use { load(it) }
            }
        }
    }

    private fun resetOutputDirectories() {
        commonMainOutputDirectory.get().asFile.deleteRecursively()
        commonTestOutputDirectory.get().asFile.deleteRecursively()
    }

    private fun writeGeneratedMaterial(bytes: List<Int>, exact: String) {
        val encrypted = encode(bytes, exact)
        val split = (encrypted.size + 1) / 2

        writeKotlin(
            commonMainOutputDirectory.get().asFile.resolve(GENERATED_MAIN_FILE),
            """
package kr.foundcake.calculator.challenge

internal object GeneratedChallengeMaterial {
    internal val exactTargetCodes = ${formatArray(exact.map { it.code })}
    internal val encryptedPartA = ${formatArray(encrypted.take(split))}
    internal val encryptedPartB = ${formatArray(encrypted.drop(split))}
    internal const val plainCheck = ${checksum(bytes)}
}
            """.trimIndent(),
        )

        writeKotlin(
            commonTestOutputDirectory.get().asFile.resolve(GENERATED_TEST_FILE),
            """
package kr.foundcake.calculator.challenge

internal object GeneratedChallengeTestMaterial {
    internal val expectedFlagBytes = ${formatArray(bytes)}
}
            """.trimIndent(),
        )
    }

    private fun compact(raw: String): String {
        val allowed = "0123456789+-*/(). \t\n\r".toSet()
        val cleaned = raw.filter { it in allowed && !it.isWhitespace() }
        require(cleaned.isNotEmpty()) { "CHALLENGE_TRIGGER is empty after normalization" }

        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flushNumber() {
            if (buffer.isEmpty()) return

            val token = buffer.toString()
            require(token != "." && token.count { it == '.' } <= 1) {
                "Invalid numeric token in CHALLENGE_TRIGGER: $token"
            }

            tokens += token
            buffer.clear()
        }

        cleaned.forEach { char ->
            when {
                char.isDigit() || char == '.' -> buffer.append(char)
                char in "+-*/()" -> {
                    flushNumber()
                    tokens += char.toString()
                }

                else -> error("Invalid CHALLENGE_TRIGGER character after filtering: $char")
            }
        }

        flushNumber()
        require(tokens.isNotEmpty()) { "CHALLENGE_TRIGGER produced no tokens" }
        return tokens.joinToString(separator = "")
    }

    private fun validateFlag(bytes: List<Int>) {
        require(bytes.size >= 8) { "CHALLENGE_FLAG is too short" }
        require(bytes.last() == FLAG_TERMINATOR_CODE) {
            "CHALLENGE_FLAG must end with the app-side wrapper terminator"
        }
    }

    private fun checksum(bytes: List<Int>): Int {
        var acc = 0x7A452F1B
        bytes.forEachIndexed { index, byte ->
            acc = ((acc xor (byte + index * 257)) * 16777619) + index
        }
        return acc
    }

    private fun encode(bytes: List<Int>, canonical: String): List<Int> {
        val key = deriveKey(canonical, bytes.size)
        return bytes.mapIndexed { index, byte -> byte xor key[index] xor mask(index) }
    }

    private fun deriveKey(canonical: String, size: Int): List<Int> {
        val seed = (canonical + "|" + salt()).encodeToByteArray().map { it.toInt() and 0xFF }
        val foldedSeed = seed.foldIndexed(0x5A) { index, acc, byte -> mix(index, acc, byte) }
        var acc = foldedSeed

        return List(size) { index ->
            val left = seed[index % seed.size]
            val right = seed[(index * 7) % seed.size]
            acc = (acc * 131 + left + index * 17) and 0xFF
            (acc xor ((index * 31 + right) and 0xFF)) and 0xFF
        }
    }

    private fun mix(index: Int, value: Int, next: Int): Int {
        val mixers = listOf<(Int, Int, Int) -> Int>(
            { i, v, n -> (v xor ((i * 19 + n) and 0xFF)) and 0xFF },
            { i, v, n -> (v + (i * 7) + n) and 0xFF },
            { i, v, n -> (v xor (n * 3 + i)) and 0xFF },
        )
        return mixers[index % mixers.size](index, value, next)
    }

    private fun salt(): String {
        return SALT_FRAGMENTS.flatten().joinToString(separator = "") { it.toChar().toString() }
    }

    private fun mask(index: Int): Int = (index * 29 + 17) and 0xFF

    private fun formatArray(values: List<Int>): String {
        return values.chunked(11).joinToString(
            prefix = "intArrayOf(\n",
            postfix = "\n    )",
            separator = ",\n",
        ) { chunk -> "        " + chunk.joinToString(separator = ", ") }
    }

    private fun writeKotlin(file: File, content: String) {
        file.parentFile.mkdirs()
        file.writeText(content + "\n")
    }

    private companion object {
        const val CHALLENGE_FLAG_PROPERTY = "CHALLENGE_FLAG"
        const val CHALLENGE_TRIGGER_PROPERTY = "CHALLENGE_TRIGGER"
        const val DEFAULT_TRIGGER = "0012 + 03*004 - 05 /006 + 007"
        const val FLAG_TERMINATOR_CODE = 125

        const val GENERATED_MAIN_FILE =
            "kr/foundcake/calculator/challenge/GeneratedChallengeMaterial.kt"
        const val GENERATED_TEST_FILE =
            "kr/foundcake/calculator/challenge/GeneratedChallengeTestMaterial.kt"

        val DEFAULT_FLAG_BYTES = listOf(
            67, 84, 70, 123, 99, 97, 110, 111, 110, 105, 99, 97, 108, 105, 122, 101,
            100, 95, 112, 97, 116, 104, 95, 117, 110, 108, 111, 99, 107, 115, 95, 115,
            104, 97, 114, 101, 100, 95, 116, 114, 117, 116, 104, 125,
        )

        val SALT_FRAGMENTS = listOf(
            listOf(115, 57, 64),
            listOf(75, 33, 50),
            listOf(109, 80, 35, 105, 111, 115),
        )
    }
}
