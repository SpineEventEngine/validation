/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import io.spine.dependency.artifact
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.Logging
import io.spine.dependency.test.JUnit.Jupiter
import io.spine.gradle.report.license.LicenseReporter

plugins {
    kotlin("jvm")
    id("module-testing")
    protobuf
    `java-test-fixtures`
    prototap
    // Apply Kover so that the in-process compilation tests of this module credit
    // coverage to the `:context` (and other) production classes they exercise.
    // `KoverConfig` at the root folds this module into the aggregated report.
    id("org.jetbrains.kotlinx.kover")
}
LicenseReporter.generateReportIn(project)

dependencies {
    val contextProject = project(":context")
    implementation(contextProject)
    implementation(project(":jvm-runtime"))

    testImplementation(Logging.testLib)?.because("We need `tapConsole`.")
    testImplementation(Compiler.testlib)
    // The Java-side renderer (`JavaValidationPlugin`) and its settings
    // (`JavaValidationRendererSettings`) are needed by `UnsignedFieldWarningSpec`
    // to drive the warning emitted by
    // `JavaValidationRenderer` -> `BoundedFieldGenerator` end-to-end.
    testImplementation(project(":java"))
    testImplementation(project(":java-settings"))

    testFixturesImplementation(Compiler.api)
    testFixturesImplementation(Compiler.testlib)
    testFixturesImplementation(Jupiter.artifact { params })
    testFixturesImplementation(contextProject)
}

protobuf {
    protoc { artifact = Protobuf.compiler }
}
