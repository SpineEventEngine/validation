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

import io.spine.protodata.gradle.plugin.LaunchProtoData

plugins {
    id("io.spine.protodata")
}

protoData {
    renderers(
        "io.spine.validation.java.PrintValidationInsertionPoints",
        "io.spine.validation.java.JavaValidationRenderer",

        // Suppress warnings in the generated code.
        "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",
        "io.spine.protodata.codegen.java.suppress.SuppressRenderer"

    )
    plugins(
        "io.spine.validation.ValidationPlugin",
        "io.spine.validation.test.MoneyValidationPlugin"
    )
}

tasks.withType<LaunchProtoData>().configureEach {
    configuration.set(file("protodata.pb.json"))
}

modelCompiler {
    java {
        codegen {
            validation { skipValidation() }
        }
    }
}

val spineBaseVersion: String by extra
val spineTimeVersion: String by extra

dependencies {
    protoData(project(":java-tests:extensions"))
    implementation(project(":java-tests:extensions"))
    implementation(project(":java-runtime-bundle"))
    implementation("io.spine:spine-base:$spineBaseVersion")
    implementation("io.spine:spine-time:$spineTimeVersion")
}
