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
import io.spine.dependency.lib.Kotlin
import io.spine.dependency.lib.KotlinX
import io.spine.dependency.lib.Roaster
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.Spine
import io.spine.dependency.local.ToolBase
import io.spine.gradle.publish.PublishingRepos
import io.spine.gradle.publish.spinePublishing
import io.spine.gradle.report.coverage.JacocoConfig
import io.spine.gradle.report.license.LicenseReporter
import io.spine.gradle.report.pom.PomGenerator
import io.spine.gradle.standardToSpineSdk

plugins {
    idea
    jacoco
    id("project-report")
}

spinePublishing {
    modules = setOf(
        "model",
        ":proto:configuration",
        ":proto:context",
        "java",
        "java-bundle",
        "java-runtime",
        "java-runtime-bundle",
    )
    modulesWithCustomPublishing = setOf(
        "java-bundle",
        "java-runtime-bundle"
    )
    destinations = with(PublishingRepos) {
        setOf(
            gitHub("validation"),
            cloudArtifactRegistry
        )
    }
    artifactPrefix = "spine-validation-"

    dokkaJar {
        java = true
        kotlin = true
    }
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    repositories.standardToSpineSdk()
    group = "io.spine.validation"
    version = extra["validationVersion"]!!

    configurations.all {
        exclude(group = "io.spine", module = "spine-flogger-api")
        exclude(group = "io.spine", module = "spine-logging-backend")

        resolutionStrategy {
            @Suppress("DEPRECATION") // `Kotlin.stdLibJdk7` is a transitive dependency.
            force(
                Roaster.api,
                Roaster.jdt,
                ProtoData.pluginLib,
                ProtoData.backend,
                ProtoData.java,
                Spine.base,
                Logging.lib,
                CoreJava.client,
                CoreJava.server,
                ToolBase.lib,
                ToolBase.pluginBase,
                Grpc.api,
                Grpc.context,
                Grpc.core,
                Grpc.protobuf,
                Grpc.stub,
                Kotlin.stdLibJdk7,
                KotlinX.Coroutines.core,
                KotlinX.Coroutines.jdk8
            )
        }
    }
}

subprojects {
    apply {
        plugin("module")
    }
}

JacocoConfig.applyTo(project)
LicenseReporter.mergeAllReports(project)
PomGenerator.applyTo(project)
