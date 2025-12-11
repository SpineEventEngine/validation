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

import io.spine.dependency.local.Compiler
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.publish.SpinePublishing
import io.spine.gradle.publish.addSourceAndDocJars

plugins {
    module
    id("io.spine.artifact-meta")
    `java-gradle-plugin`
}

val moduleArtifactId = "validation-gradle-plugin"

artifactMeta {
    artifactId.set(moduleArtifactId)
    addDependencies(
        // Add Validation module dependencies that we use for project configuration
        // to which the Validation Gradle Plugin is applied.
        Validation.javaBundle,
        Validation.runtime,
        Validation.configuration,
    )
    excludeConfigurations {
        containing(*buildToolConfigurations)
    }
}

gradlePlugin {
    website.set("https://spine.io/")
    vcsUrl.set("https://github.com/SpineEventEngine/validation.git")
    plugins {
        val pluginTags = listOf(
            "validation",
            "ddd",
            "codegen",
            "java",
            "kotlin",
            "jvm"
        )

        create("spineValidation") {
            id = "io.spine.validation"
            implementationClass = "io.spine.tools.validation.gradle.ValidationGradlePlugin"
            displayName = "Spine Validation Compiler Gradle Plugin"
            description = "Configures Spine Compiler to run the Validation Compiler."
            tags.set(pluginTags)
        }
    }
}

dependencies {
    implementation(Compiler.pluginLib)
    implementation(Compiler.gradleApi)
    implementation(ToolBase.jvmTools)
}

// Change the `artifactId` to have the `spine-validation-` prefix
// instead of just `validation-` as for the rest of the tool modules.
afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                val rootExtension = rootProject.the<SpinePublishing>()
                val projectPrefix = rootExtension.artifactPrefix
                artifactId = "$projectPrefix${project.name}"

                addSourceAndDocJars(project)
            }
        }
    }

    val sourcesJar by tasks.getting(Jar::class)
    val writeArtifactMeta  by tasks.getting
    sourcesJar.dependsOn(writeArtifactMeta)
}
