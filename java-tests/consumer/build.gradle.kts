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

import io.spine.internal.dependency.Kotest
import io.spine.internal.dependency.Spine
import io.spine.protodata.gradle.plugin.CreateSettingsDirectory
import io.spine.protodata.gradle.plugin.LaunchProtoData
import io.spine.util.theOnly

protoData {
    plugins(
        // Suppress warnings in the generated code.
        "io.spine.protodata.java.annotation.SuppressWarningsAnnotation\$Plugin",
        "io.spine.validation.java.JavaValidationPlugin",
        "io.spine.validation.test.MoneyValidationPlugin"
    )
}

val settingsDirTask: CreateSettingsDirectory = tasks.withType<CreateSettingsDirectory>().theOnly()

val copySettings by tasks.registering(Copy::class) {
    from(project.layout.projectDirectory.file(
        "io.spine.validation.java.JavaValidationPlugin.pb.json")
    )
    into(settingsDirTask.settingsDir.get())
    dependsOn(settingsDirTask)
}

tasks.withType<LaunchProtoData>().configureEach {
    dependsOn(copySettings)
}

dependencies {
    protoData(project(":java-tests:extensions"))
    protoData(project(":java-tests:extensions"))
    implementation(project(":java-tests:extensions"))
    implementation(project(":java-tests:extra-definitions"))
    implementation(Spine.time)
    testImplementation(Kotest.assertions)
}

// Uncomment the below block when remote debugging of code generation is needed.
//
//tasks.findByName("launchProtoData")?.apply {this as JavaExec
//    debugOptions {
//        // Set this option to `true` to enable remote debugging.
//        enabled.set(true)
//        port.set(5566)
//        server.set(true)
//        suspend.set(true)
//    }
//    System.err.println("Debug session for `:java-tests:consumer test` configured.")
//}
