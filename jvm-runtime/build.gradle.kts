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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.local.Base
import io.spine.dependency.local.Logging
import io.spine.dependency.local.TestLib
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.StandardJavaPublicationHandler

buildscript {
    standardSpineSdkRepositories()
    configurations {
        all {
            resolutionStrategy {
                force(
                    io.spine.dependency.local.Logging.grpcContext,
                )
            }
        }
    }
    dependencies {
        classpath(io.spine.dependency.local.CoreJvmCompiler.pluginLib)
    }
}

plugins {
    module
    `build-proto-model`
    id("io.spine.generated-sources")
    id("io.spine.descriptor-set-file")
    // We use it the KSP plugin via its ID because it's added to the build classpath
    // in the root project.
    id("com.google.devtools.ksp")
    id("maven-publish")
}

group = "io.spine"

dependencies {
    ksp(AutoServiceKsp.processor)
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    implementation(Base.lib)

    testImplementation(TestLib.lib)
}

forceSpineBase()

// Change the `artifactId` to have the `spine-validation-` prefix
// instead of just `validation-` as for the rest of the tool modules.
afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>(StandardJavaPublicationHandler.PUBLICATION_NAME) {
                val rootExtension = rootProject.the<SpinePublishing>()
                val defaultPrefix = SpinePublishing.DEFAULT_PREFIX
                val projectPrefix = rootExtension.artifactPrefix
                artifactId = "$defaultPrefix$projectPrefix${project.name}"
            }
        }
    }
}
