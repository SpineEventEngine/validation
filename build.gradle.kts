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

import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.Flogger
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.applyGitHubPackages
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
    apply(from = "$rootDir/version.gradle.kts")

    val mcJavaVersion: String by extra
    val protoDataVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$mcJavaVersion")
        // The below dependency is obtained from https://plugins.gradle.org/m2/.
        classpath("io.spine:proto-data:$protoDataVersion")
    }
}

plugins {
    `java-library`
    idea

    val protobuf = io.spine.internal.dependency.Protobuf.GradlePlugin
    val errorProne = io.spine.internal.dependency.ErrorProne.GradlePlugin
    val dokka = io.spine.internal.dependency.Dokka

    id(protobuf.id)
    id(errorProne.id)
    kotlin("jvm")
    id(dokka.pluginId) version(dokka.version)
    `force-jacoco`
}

spinePublishing {
    modules = setOf(
        "configuration",
        "java",
        "model",
        "runtime"
    )
    destinations = with(PublishingRepos) {
        setOf(
            cloudRepo,
            gitHub("validation"),
            cloudArtifactRegistry
        )
    }
    artifactPrefix = "spine-validation-"
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
    val validationVersion: String by extra
    version = validationVersion
}

subprojects {
    apply {
        plugin("net.ltgt.errorprone")
        plugin("java-library")
        plugin("kotlin")
        plugin("org.jetbrains.dokka")
        plugin("com.google.protobuf")
        plugin("io.spine.mc-java")
        plugin("pmd")
        plugin("maven-publish")
    }

    // Apply custom Kotlin script plugins.
    apply {
        plugin("pmd-settings")
    }

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)

    val javaVersion = JavaVersion.VERSION_11.toString()

    kotlin {
        explicitApi()
        jvmToolchain {
            (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    tasks {
        withType<JavaCompile> {
            configureJavac()
            configureErrorProne()
        }
        registerTestTasks()
    }

    dependencies {
        ErrorProne.apply {
            errorprone(core)
            errorproneJavac(javacPlugin)
        }
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    val spineBaseVersion: String by extra
    val spineServerVersion: String by extra
    val spineTimeVersion: String by extra

    configurations.forceVersions()
    configurations {
        all {
            resolutionStrategy {
                force(
                    Flogger.lib,
                    Flogger.Runtime.systemBackend,
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion",
                    "io.spine.tools:spine-testlib:$spineBaseVersion",
                    "io.spine:spine-server:$spineServerVersion",
                )
            }
        }
    }
    configurations.excludeProtobufLite()

    tasks.test {
        useJUnitPlatform()
        configureLogging()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xinline-classes",
                "-Xjvm-default=all"
            )
        }
    }

    val dokkaJavadoc by tasks.getting(DokkaTask::class)

    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}

JacocoConfig.applyTo(project)
LicenseReporter.mergeAllReports(project)
PomGenerator.applyTo(project)
