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

import io.spine.dependency.lib.Jackson
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvmCompiler
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation.javaBundleModule
import io.spine.dependency.local.Validation.runtimeModule

buildscript {
    forceCodegenPlugins()
}

plugins {
    java
}

allprojects {
    // No need to generate documentation for these test environments.
    disableDocumentationTasks()

    configurations {
        all {
            resolutionStrategy {
                Jackson.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataFormat.forceArtifacts(project, this@all, this@resolutionStrategy)
                Jackson.DataType.forceArtifacts(project, this@all, this@resolutionStrategy)

                force(
                    Jackson.annotations,
                    Jackson.bom,
                    Protobuf.javaLib,
                    ToolBase.jvmTools,
                    ToolBase.gradlePluginApi,
                    ToolBase.psiJava,
                )
            }
        }
    }
}

/**
 * The list of `java-tests` subprojects to which we apply McJava Gradle Plugin.
 *
 * Subprojects of `java-tests` which are not listed here will get Compiler Gradle Plugin applied.
 */
val applyMcJava = setOf(
    "extensions",
    "consumer-dependency",
    "runtime",
    "validating"
)

subprojects {
    applyPlugins()

    dependencies {
        implementation(Base.lib)
        implementation(project(":java-runtime"))
    }

    configureTaskDependencies()
}

fun Project.applyPlugins() {
    if (project.name in applyMcJava) {
        apply(plugin = CoreJvmCompiler.pluginId)
    } else {
        apply(plugin = Compiler.pluginId)
    }

    val forcedCompiler = listOf(
        Compiler.fatCli,
        Compiler.protocPlugin
    )

    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                // Use the current version of Java validation code generation instead of
                // the version used in `mc-java`.
                substitute(module(javaBundleModule)).using(project(":java"))

                // Use the current version of Java runtime in the generated code of tests.
                substitute(module(runtimeModule)).using(project(":java-runtime"))
            }
            forcedCompiler.forEach { force(it) }
        }
    }
}
