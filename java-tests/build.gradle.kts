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

import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.spine.McJava
import io.spine.internal.dependency.spine.ProtoData
import io.spine.internal.dependency.spine.Spine

buildscript {
    forceCodegenPlugins()
}

plugins {
    java
}

allprojects {
    // No need to generate documentation for these test environments.
    disableDocumentationTasks()
}

/**
 * The list of `java-tests` subprojects to which we apply McJava Gradle Plugin.
 *
 * Subprojects of `java-tests` which are not listed here will get ProtoData Gradle Plugin applied.
 */
val applyMcJava = setOf(
    "extensions",
    "extra-definitions",
    "runtime",
    "validation",
    "validation-gen",
)

subprojects {
    applyPlugins()

    dependencies {
        implementation(Spine.base)
        implementation(project(":java-runtime"))
    }

    configureTaskDependencies()

    protobuf {
        protoc {
            artifact = Protobuf.compiler
        }
    }
}

fun Project.applyPlugins() {
    val forcedProtoData = listOf(
        ProtoData.fatCli,
        ProtoData.protocPlugin
    )

    if (project.name in applyMcJava) {
        apply(plugin = McJava.pluginId)
        configurations.all {
            resolutionStrategy {
                dependencySubstitution {
                    // Use the current version of Java validation code generation instead of
                    // the version used in `mc-java`.
                    substitute(
                        module("io.spine.validation:spine-validation-java-bundle")
                    ).using(project(":java"))
                }
                forcedProtoData.forEach { force(it) }
            }
        }
    } else {
        apply(plugin = ProtoData.pluginId)
        configurations.all {
            resolutionStrategy {
                forcedProtoData.forEach { force(it) }
            }
        }
    }
}
