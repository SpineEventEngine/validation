/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.dependency.Validation
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.idea
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `java-library`
    kotlin("jvm")
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    pmd
    id("dokka-for-java")
    `maven-publish`
    jacoco
    idea
    id("project-report")
    id("pmd-settings")
}

apply<IncrementGuard>()
LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)

object BuildSettings {
    private const val JAVA_VERSION = 11

    val javaVersion = JavaLanguageVersion.of(JAVA_VERSION)
}

project.run {
    addDependencies()
    forceConfigurations()
    applyGeneratedDirectories("$projectDir/generated")

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin(javaVersion)

    configureTests()
    configureTaskDependencies()
    dependTestOnJavaRuntime()
    configureTaskDependencies()
}

/**
 * A subproject of Validation.
 */
typealias Module = Project

/**
 * Adds dependencies common to all subprojects.
 */
fun Module.addDependencies() {
    dependencies {
        ErrorProne.apply {
            errorprone(core)
            errorproneJavac(javacPlugin)
        }
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }
}

/**
 * Sets dependencies on `:java-runtime-bundle:shadowJar` for Java-related modules,
 * unless it's ":java-runtime-bundle" itself.
 *
 * The dependencies are set for the tasks:
 *   1. `test`
 *   2. `launchProtoData`
 *   3. `launchTestProtoData`
 *   4. `pmdMain`.
 */
fun Module.dependTestOnJavaRuntime() {
    val javaBundleModule = ":java-runtime"
    if (!name.startsWith(":java") || name == javaBundleModule) {
        return
    }

    afterEvaluate {
        val test: Task by tasks.getting
        val javaBundleJar = project(javaBundleModule).tasks.findByName("shadowJar")

        fun String.dependOn(task: Task) = tasks.findByName(this)?.dependsOn(task)

        javaBundleJar?.let {
            test.dependsOn(it)
            "launchProtoData".dependOn(it)
            "launchTestProtoData".dependOn(it)
            "pmdMain".dependOn(it)
        }
    }
}

/**
 * Forces versions of dependencies and excludes Protobuf Light.
 */
fun Module.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()

        all {
            resolutionStrategy {
                /* Use default version of Validation, not those coming with Spine because
                   it would use `validationVersion` extension property of the project. */
                val validationVersion = Validation.version
                force(
                    Flogger.lib,
                    Flogger.Runtime.systemBackend,
                    JUnit.runner,

                    Spine.base,
                    Spine.time,
                    Spine.testlib,
                    Spine.toolBase,
                    Spine.server,
                    "io.spine.validation:spine-validation-java-runtime:$validationVersion",

                    Jackson.core,
                    Jackson.moduleKotlin,
                    Jackson.databind,
                    Jackson.bom,
                    Jackson.annotations,
                    Jackson.dataformatYaml,
                    Jackson.dataformatXml,

                    Dokka.BasePlugin.lib
                )
            }

            // Exclude all transitive dependencies onto the recently moved artifact.
            exclude("io.spine", "spine-validate")
        }
    }
}

/**
 * Configures Java in this subproject.
 */
fun Module.configureJava(javaVersion: JavaLanguageVersion) {
    java {
        toolchain.languageVersion.set(javaVersion)
    }
    tasks {
        withType<JavaCompile>().configureEach {
            configureJavac()
            configureErrorProne()
            // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
            options.isIncremental = false
        }
        withType<Jar>().configureEach {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

/**
 * Configures Kotlin in this subproject.
 */
fun Module.configureKotlin(javaVersion: JavaLanguageVersion) {
    kotlin {
        explicitApi()
        applyJvmToolchain(javaVersion.asInt())
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion.toString()

        setFreeCompilerArgs()
        // https://stackoverflow.com/questions/38298695/gradle-disable-all-incremental-compilation-and-parallel-builds
        incremental = false
    }
}

/**
 * Configures test tasks.
 */
fun Project.configureTests() {
    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 *
 * @param generatedDir
 *          the name of the root directory with the generated code
 */
fun Module.applyGeneratedDirectories(generatedDir: String) {
    val generatedMain = "$generatedDir/main"
    val generatedJava = "$generatedMain/java"
    val generatedKotlin = "$generatedMain/kotlin"
    val generatedGrpc = "$generatedMain/grpc"
    val generatedSpine = "$generatedMain/spine"

    val generatedTest = "$generatedDir/test"
    val generatedTestJava = "$generatedTest/java"
    val generatedTestKotlin = "$generatedTest/kotlin"
    val generatedTestGrpc = "$generatedTest/grpc"
    val generatedTestSpine = "$generatedTest/spine"

    idea {
        module {
            generatedSourceDirs.addAll(files(
                generatedJava,
                generatedTestJava,
                generatedKotlin,
                generatedTestKotlin,
                generatedGrpc,
                generatedTestGrpc,
                generatedSpine,
                generatedTestSpine,
            ))
            testSources.from(
                generatedTestJava,
                generatedTestKotlin,
                generatedTestGrpc,
                generatedTestSpine,
            )
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
}
