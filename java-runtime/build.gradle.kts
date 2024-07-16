/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.internal.dependency.AutoService
import io.spine.internal.dependency.Spine

plugins {
    `build-proto-model`
    module
    id("io.spine.mc-java")
}

dependencies {
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    implementation(Spine.base)
    implementation(Spine.Logging.lib)

    testImplementation(Spine.testlib)
}

// We deliberately turn off validation codegen in this module to ease the issues
// associated with the migration when ProtoData or Validation API changes.
//
// The production Protobuf types of this module do not declare validation constraints, again,
// to ease the possible migration issues.
//
// Please see `java-tests/runtime` submodule for the tests associated with the Validation Runtime
// and the generated code.
//
//modelCompiler {
//    java {
//        codegen {
//            validation().enabled.set(false)
//        }
//    }
//}
