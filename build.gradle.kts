plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "dev.osome"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo1.maven.org/maven2/")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"

    val targetPlatform: String? = project.findProperty("targetPlatform") as? String

    val nativeTarget = when (targetPlatform ?: "") {
        "macosArm64" -> macosArm64("native")
        "linuxArm64" -> linuxArm64("native")
        "linuxX64" -> linuxX64("native")
        "" -> when {
            hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
            hostOs == "Linux" && isArm64 -> linuxArm64("native")
            hostOs == "Linux" && !isArm64 -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
        else -> throw GradleException("Unsupported target platform: $targetPlatform")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/caBundleSrc"))
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val nativeTest by getting {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.kotlin.test)
            }
        }
    }
}

val generateCaBundleSource by tasks.registering {
    val pemFile = file("src/nativeMain/resources/cacert.pem")
    val outputDir = layout.buildDirectory.dir("generated/caBundleSrc")

    inputs.file(pemFile)
    outputs.dir(outputDir)

    doLast {
        val pemContent = pemFile.readText()
        val outputFile = outputDir.get().file("CaBundle.kt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(buildString {
            appendLine("// AUTO-GENERATED from cacert.pem — do not edit")
            appendLine("// Source: https://curl.se/ca/cacert.pem (Mozilla CA bundle)")
            appendLine("package config")
            appendLine()
            appendLine("internal val EMBEDDED_CA_BUNDLE: String =")
            appendLine("\"\"\"")
            append(pemContent)
            if (!pemContent.endsWith("\n")) appendLine()
            appendLine("\"\"\"")
            appendLine(".trimIndent()")
        })
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateCaBundleSource)
}