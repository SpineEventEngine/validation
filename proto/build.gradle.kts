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

import io.spine.dependency.lib.Protobuf

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
        // The below dependency is obtained from https://plugins.gradle.org/m2/.
        spineCompiler.run {
            classpath(pluginLib(dogfoodingVersion))
        }
        classpath(io.spine.dependency.local.CoreJvmCompiler.pluginLib)
    }
}

subprojects {
    apply {
        plugin("io.spine.core-jvm")
    }

    configurations {
        all {
            resolutionStrategy {
                force(
                    io.spine.dependency.local.ToolBase.lib,
                )
            }
        }
    }

    dependencies {
        Protobuf.libs.forEach { implementation(it) }
    }
}

// Temporarily disable this task for this parent Gradle project.
// The tasks in its children still should execute fine.
// See more [here](https://github.com/jk1/Gradle-License-Report/issues/337).
val generateLicenseReport by tasks.getting
generateLicenseReport.enabled = false
