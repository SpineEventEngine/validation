/*
 * Copyright 2025, TeamDev. All rights reserved.
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
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.CoreJava
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
    jacoco
    idea
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
    applyGeneratedDirectories("$projectDir/generated")

    val javaVersion = BuildSettings.javaVersion
    configureJava(javaVersion)
    configureKotlin(javaVersion)

    configureTaskDependencies()
    dependTestOnJvmRuntime()
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
            errorproneJavac(javacPlugin)
        }
        api(JSpecify.annotations)
    }
}

/**
 * Sets dependencies on `:jvm-runtime-bundle:shadowJar` for Java-related modules,
 * unless it's ":jvm-runtime-bundle" itself.
 *
 * The dependencies are set for the tasks:
 *   1. `test`
 *   2. `launchProtoData`
 *   3. `launchTestProtoData`
 *   4. `pmdMain`.
 */
fun Module.dependTestOnJvmRuntime() {
    val javaBundleModule = ":jvm-runtime"
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
                dependencySubstitution {
                    // Substitute the legacy artifact coordinates with the new `ToolBase.lib` alias.
                    substitute(module("io.spine.tools:spine-tool-base")).using(module(ToolBase.lib))
                }

                Grpc.forceArtifacts(project, this@all, this@resolutionStrategy)
                Ksp.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataFormat.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataType.forceArtifacts(project, this@all, this@resolutionStrategy)

                force(
                    Jackson.bom,
                    Jackson.annotations,
                    JUnit.bom,
                    Kotlin.bom,
                    Kotlin.Compiler.embeddable,
                    Reflect.lib,
                    Base.annotations,
                    Base.lib,
                    Protobuf.compiler,
                    Time.lib,
                    TestLib.lib,
                    ToolBase.gradlePluginApi,
                    ToolBase.jvmTools,
                    ToolBase.lib,
                    ToolBase.intellijPlatform,
                    ToolBase.intellijPlatformJava,
                    ToolBase.psiJava,
                    ToolBase.protobufSetupPlugins,
                    Logging.libJvm,
                    Logging.testLib,
                    Logging.grpcContext,
                    CoreJava.server,
                    CoreJvm.serverTestLib,
                    Validation.runtime,
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

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.setFreeCompilerArgs()
    }
}

/**
 * Adds directories with the generated source code to source sets of the project and
 * to IntelliJ IDEA module settings.
 *
 * @param generatedDir
 *          the name of the root directory with the generated code.
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
