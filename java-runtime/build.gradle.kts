/*
 * Copyright 2022, TeamDev. All rights reserved.
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

modelCompiler {
    java {
        codegen {
            validation { skipValidation() }
        }
    }
}

// Uncomment the below block when remote debugging of code generation is needed.
//
//tasks.findByName("launchTestProtoData")?.apply {this as JavaExec
//    debugOptions {
//        // Set this option to `true` to enable remote debugging.
//        enabled.set(true)
//        port.set(5566)
//        server.set(true)
//        suspend.set(true)
//    }
//    System.err.println("Debug session for `:java-runtime test` configured.")
//}
