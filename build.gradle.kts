/*
 * Copyright 2022, TeamDev. All rights reserved.
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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.internal.dependency.Dokka
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Jackson
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.publish.IncrementGuard
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
    dependencies {
        classpath(io.spine.internal.dependency.Spine.ProtoData.pluginLib)
        classpath(io.spine.internal.dependency.Spine.McJava.pluginLib)
    }
}

plugins {
    `java-library`
    idea
    id(protobufPlugin)
    id(errorPronePlugin)
    kotlin("jvm")
    `force-jacoco`
    `detekt-code-analysis`
    id(gradleDoctor.pluginId) version gradleDoctor.version
}

spinePublishing {
    modules = setOf(
        ":proto:configuration",
        ":proto:context",
        "java",
        "java-bundle",
        "java-runtime",
        "java-runtime-bundle",
        "model"
    )
    destinations = with(PublishingRepos) {
        setOf(
            cloudRepo,
            gitHub("validation"),
            cloudArtifactRegistry
        )
    }
    artifactPrefix = "spine-validation-"

    dokkaJar {
        enabled = true
    }
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")

    repositories {
        applyStandard()
        applyGitHubPackages("ProtoData", project)
    }

    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
    }

    group = "io.spine.validation"
    version = extra["validationVersion"]!!
}

subprojects {
    applyPlugins()
    addDependencies()
    forceConfigurations()

    applyGeneratedDirectories("$projectDir/generated")

    val javaVersion = JavaVersion.VERSION_11
    configureJava(javaVersion)
    configureKotlin(javaVersion)

    configureTests()
    configureTaskDependencies()
    dependTestOnJavaRuntime()
}

JacocoConfig.applyTo(project)
LicenseReporter.mergeAllReports(project)
PomGenerator.applyTo(project)

/**
 * A subproject of Validation.
 */
typealias Subproject = Project

/**
 * Applies plugins to a subproject
 */
fun Subproject.applyPlugins() {
    apply {
        plugin(ErrorProne.GradlePlugin.id)
        plugin("java-library")
        plugin("kotlin")
        plugin(Protobuf.GradlePlugin.id)
        plugin("pmd")
        plugin("maven-publish")
        plugin("dokka-for-java")
        plugin("detekt-code-analysis")
    }

    // Apply custom Kotlin script plugins.
    apply {
        plugin("pmd-settings")
    }

    apply<IncrementGuard>()
    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
}

/**
 * Adds dependencies common to all subprojects.
 */
fun Subproject.addDependencies() {
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
 *   2. `launchProtoDataMain`
 *   3. `launchProtoDataTest`
 *   4. `pmdMain`.
 */
fun Subproject.dependTestOnJavaRuntime() {
    val javaBundleModule = ":java-runtime-bundle"
    if (!name.startsWith(":java") || name == javaBundleModule) {
        return
    }

    afterEvaluate {
        val test: Task by tasks.getting
        val javaBundleJar = project(javaBundleModule).tasks.findByName("shadowJar")

        fun String.dependOn(task: Task) = tasks.findByName(this)?.dependsOn(task)

        javaBundleJar?.let {
            test.dependsOn(it)
            "launchProtoDataMain".dependOn(it)
            "launchProtoDataTest".dependOn(it)
            "pmdMain".dependOn(it)
        }
    }
}

/**
 * Forces versions of dependencies and excludes Protobuf Light.
 */
fun Subproject.forceConfigurations() {
    configurations {
        forceVersions()
        excludeProtobufLite()

        all {
            resolutionStrategy {
                val spine = Spine(project)
                force(
                    Flogger.lib,
                    Flogger.Runtime.systemBackend,

                    spine.base,
                    spine.time,
                    spine.testlib,
                    spine.toolBase,
                    spine.server,
                    "io.spine.validation:spine-validation-java-runtime:${Spine.DefaultVersion.validation}",

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
fun Subproject.configureJava(javaVersion: JavaVersion) {
    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        tasks {
            withType<JavaCompile>().configureEach {
                configureJavac()
                configureErrorProne()
            }
            withType<Jar>().configureEach {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }
}

/**
 * Configures Kotlin in this subproject.
 */
fun Subproject.configureKotlin(javaVersion: JavaVersion) {
    kotlin {
        explicitApi()

        tasks {
            withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = javaVersion.toString()
                }
            }
        }
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
fun Subproject.applyGeneratedDirectories(generatedDir: String) {
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

    sourceSets {
        main {
            java.srcDirs(
                generatedJava,
                generatedGrpc,
                generatedSpine,
            )
            kotlin.srcDirs(
                generatedKotlin,
            )
        }
        test {
            java.srcDirs(
                generatedTestJava,
                generatedTestGrpc,
                generatedTestSpine,
            )
            kotlin.srcDirs(
                generatedTestKotlin,
            )
        }
    }

    idea {
        module {
            generatedSourceDirs.addAll(files(
                generatedJava,
                generatedKotlin,
                generatedGrpc,
                generatedSpine,
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
