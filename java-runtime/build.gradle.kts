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

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.local.Base
import io.spine.dependency.local.Logging
import io.spine.dependency.local.TestLib
import io.spine.gradle.protobuf.setup

buildscript {
    standardSpineSdkRepositories()
    dependencies {
        classpath(mcJava.pluginLib)
    }
}

plugins {
    // We use it the KSP plugin via its ID because it's added to the build classpath
    // in the root project.
    id("com.google.devtools.ksp")
    `build-proto-model`
    module
}

// This cannot be moved under the `build-proto-model` script. It would not work from there.
// Please see the documentation for `GenerateProtoTask.setup()` for details.
protobuf {
    generateProtoTasks.all().configureEach {
        setup()
    }
}

dependencies {
    ksp(AutoServiceKsp.processor)
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    implementation(Base.lib)
    implementation(Logging.lib)

    testImplementation(TestLib.lib)
}

forceSpineBase()
