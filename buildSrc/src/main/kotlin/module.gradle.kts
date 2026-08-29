/*
 * Copyright 2026, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.dependency.boms.BomsPlugin
import io.spine.dependency.build.Dokka
import io.spine.dependency.build.ErrorProne
import io.spine.dependency.build.JSpecify
import io.spine.dependency.build.Ksp
import io.spine.dependency.kotlinx.AtomicFu
import io.spine.dependency.kotlinx.Coroutines
import io.spine.dependency.lib.Caffeine
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.JacksonV2
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.gradle.github.pages.updateGitHubPages
import io.spine.gradle.javac.configureErrorProne
import io.spine.gradle.javac.configureJavac
import io.spine.gradle.javadoc.JavadocConfig
import io.spine.gradle.kotlin.applyJvmToolchain
import io.spine.gradle.kotlin.setFreeCompilerArgs
import io.spine.gradle.publish.IncrementGuard
import io.spine.gradle.report.license.LicenseReporter
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    java
    `java-library`
    kotlin("jvm")
    id("module-testing")
    id("com.google.protobuf")
    id("net.ltgt.errorprone")
    id("detekt-code-analysis")
    pmd
    id("dokka-setup")
    `maven-publish`
    id("org.jetbrains.kotlinx.kover")
    id("project-report")
    id("pmd-settings")
}
apply<BomsPlugin>()
apply<IncrementGuard>()
LicenseReporter.generateReportIn(project)
JavadocConfig.applyTo(project)

project.run {
    addDependencies()
    forceConfigurations()

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin(javaVersion)

    configureTaskDependencies()
    configureProtoc()
    setupDocPublishing()
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
        }
        api(JSpecify.annotations)
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
                Grpc.forceArtifacts(project, this@all, this@resolutionStrategy)
                Ksp.forceArtifacts(project, this@all, this@resolutionStrategy)

                JacksonV2.Core.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.DataType.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.Module.forceArtifacts(project, this@all, this@resolutionStrategy)
                JacksonV2.Junior.forceArtifacts(project, this@all, this@resolutionStrategy)

                force(
                    Caffeine.lib,
                    // The refreshed compiler pins the current Time while
                    // floor artifacts still request the previous one.
                    Time.lib,
                    Time.javaExtensions,
                    // `Coroutines.forceArtifacts` (where present) covers the
                    // modules list but not the BOM itself.
                    Coroutines.bom,
                    AtomicFu.lib,
                    Protobuf.javaLib,
                    Jackson.annotations,
                    JUnit.bom,
                    Kotlin.bom,
                    Kotlin.scriptRuntime,
                    KotlinPoet.lib,
                    Base.annotations,
                    Base.lib,
                    Base.format,
                    Base.environment,
                    Time.lib,
                    Logging.lib,
                    Validation.runtime,
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

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.setFreeCompilerArgs()
    }
}

fun Module.configureProtoc() {
    protobuf {
        protoc { artifact = Protobuf.compiler }
    }
}

/**
 * Configures documentation publishing for this subproject.
 */
fun Module.setupDocPublishing() {
    updateGitHubPages {
        rootFolder.set(rootDir)
    }

    tasks.named("publish") {
        dependsOn("${project.path}:updateGitHubPages")
    }
}
