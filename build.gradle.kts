/*
 * Copyright 2024, TeamDev. All rights reserved.
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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.KotlinPoet
import io.spine.dependency.lib.Roaster
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Time
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.repo.standardToSpineSdk
import io.spine.gradle.report.coverage.JacocoConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator

buildscript {
    standardSpineSdkRepositories()
    configurations.all {
        resolutionStrategy {
            force(
                // Make sure we have the right Protobuf Runtime.
                io.spine.dependency.lib.Protobuf.javaLib,
                io.spine.dependency.local.Logging.grpcContext,
                "io.spine.validation:spine-validation-java-runtime:2.0.0-SNAPSHOT.360",
            )
        }
    }
    dependencies {
        // Use newer KSP in the classpath to avoid the "too old" warnings.
        classpath(io.spine.dependency.build.Ksp.run { artifact(gradlePlugin) })

        // Make sure we have the right Protobuf Runtime by adding it explicitly.
        classpath(io.spine.dependency.lib.Protobuf.javaLib)

        // For `io.spine.generated-sources` plugin.
        classpath(io.spine.dependency.local.ToolBase.protobufSetupPlugins)

        classpath(io.spine.dependency.kotlinx.DateTime.lib)

        // For `io.spine.artifact-meta` plugin.
        classpath(io.spine.dependency.local.ToolBase.jvmToolPluginDogfooding)
    }
}

plugins {
    idea
    jacoco
    `gradle-doctor`
    id("project-report")
}

spinePublishing {
    modules = setOf(
        "context",
        "java",
        "java-bundle",
        "jvm-runtime",
        "ksp",
    )
    modulesWithCustomPublishing = setOf(
        "java-bundle",
        "gradle-plugin",
    )
    destinations = with(PublishingRepos) {
        setOf(
            gitHub("validation"),
            cloudArtifactRegistry
        )
    }
    artifactPrefix = "validation-"
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    repositories.standardToSpineSdk()
    group = "io.spine.tools"
    version = extra["validationVersion"]!!

    configurations.all {
        exclude(group = "io.spine", module = "spine-flogger-api")
        exclude(group = "io.spine", module = "spine-logging-backend")

        resolutionStrategy {
            @Suppress("DEPRECATION") // `Kotlin.stdLibJdk7` is a transitive dependency.
            force(
                Base.lib,
                Compiler.api,
                Compiler.backend,
                Compiler.gradleApi,
                Compiler.jvm,
                Compiler.params,
                Compiler.pluginLib,
                CoreJvm.client,
                CoreJvm.server,
                Grpc.bom,
                KotlinPoet.lib,
                Logging.lib,
                Roaster.api,
                Roaster.jdt,
                Time.lib,
                Time.javaExtensions,
                ToolBase.lib,
                ToolBase.pluginBase,
                Validation.javaBundle,
                "io.spine.validation:spine-validation-java-runtime:2.0.0-SNAPSHOT.360",
            )
        }
    }
}

JacocoConfig.applyTo(project)
LicenseReporter.mergeAllReports(project)
PomGenerator.applyTo(project)
