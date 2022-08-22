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
import io.spine.internal.dependency.Jackson
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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)
    apply(from = "$rootDir/version.gradle.kts")

    val protoDataVersion: String by extra

    dependencies {
        // The below dependency is obtained from https://plugins.gradle.org/m2/.
        classpath("io.spine:protodata:$protoDataVersion")
    }
}

plugins {
    `java-library`
    idea

    val protobuf = io.spine.internal.dependency.Protobuf.GradlePlugin
    val errorProne = io.spine.internal.dependency.ErrorProne.GradlePlugin

    id(protobuf.id)
    id(errorProne.id)
    kotlin("jvm")
    `force-jacoco`
}

spinePublishing {
    modules = setOf(
        ":proto:configuration",
        ":proto:context",
        "java",
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
    apply {
        plugin("net.ltgt.errorprone")
        plugin("java-library")
        plugin("kotlin")
        plugin("com.google.protobuf")
        plugin("pmd")
        plugin("maven-publish")
        plugin("dokka-for-java")
    }

    // Apply custom Kotlin script plugins.
    apply {
        plugin("pmd-settings")
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

    configurations {
        forceVersions()
        excludeProtobufLite()

        val spineBaseVersion: String by extra
        val spineServerVersion: String by extra
        val spineTimeVersion: String by extra
        val spineToolBaseVersion: String by extra
        val publishedValidationVersion: String by extra

        all {
            resolutionStrategy {
                force(
                    Flogger.lib,
                    Flogger.Runtime.systemBackend,
                    "io.spine:spine-base:$spineBaseVersion",
                    "io.spine:spine-time:$spineTimeVersion",
                    "io.spine.tools:spine-testlib:$spineBaseVersion",
                    "io.spine.tools:spine-tool-base:$spineToolBaseVersion",
                    "io.spine:spine-server:$spineServerVersion",
                    Jackson.core,
                    Jackson.moduleKotlin,
                    Jackson.databind,
                    Jackson.bom,
                    Jackson.annotations,
                    Jackson.dataformatYaml,
                    Jackson.dataformatXml
                )

                // Some direct project dependencies still have
                // an "old" `io.spine.validation:runtime:2.0.0-SNAPSHOT.12`
                // as their transient dependency. That's why we need to substitute it
                // with the last published version of current `spine-validation-runtime` artifact.
                dependencySubstitution {
                    substitute(module("io.spine.validation:runtime:2.0.0-SNAPSHOT.12"))
                        .using(module("io.spine.validation:" +
                                "spine-validation-runtime:$publishedValidationVersion"))
                }
            }
        }
    }

    val javaVersion = JavaVersion.VERSION_11

    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        tasks {
            withType<JavaCompile>().configureEach {
                configureJavac()
                configureErrorProne()
            }
            withType<org.gradle.jvm.tasks.Jar>().configureEach {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
    }

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

    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform {
                includeEngines("junit-jupiter")
            }
            configureLogging()
        }
    }

    LicenseReporter.generateReportIn(project)
    JavadocConfig.applyTo(project)
}

JacocoConfig.applyTo(project)
LicenseReporter.mergeAllReports(project)
PomGenerator.applyTo(project)
