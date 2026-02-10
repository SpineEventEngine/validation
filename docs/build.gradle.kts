/*
 * Copyright 2026, TeamDev. All rights reserved.
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

import io.spine.gradle.RunGradle
import io.spine.gradle.docs.UpdatePluginVersion
import io.spine.gradle.report.license.LicenseReporter

LicenseReporter.generateReportIn(project)

/**
 * Builds and runs the site locally.
 */
val runSite by tasks.registering(Exec::class) {
    dependsOn("installDependencies")
    commandLine("$projectDir/_script/hugo-serve")
}

/**
 * Builds the site without starting the server.
 */
val buildSite by tasks.registering(Exec::class) {
    dependsOn("installDependencies")
    commandLine("$projectDir/_script/hugo-build")
}

/**
 * Installs the Node.js dependencies required for building the site.
 */
val installDependencies by tasks.registering(Exec::class) {
    commandLine("$projectDir/_script/install-dependencies")
}

/**
 * Embeds the code samples into pages of the site.
 */
val embedCode by tasks.registering(Exec::class) {
    commandLine("$projectDir/_script/embed-code")
}

/**
 * Verifies that the source code samples embedded into the pages are up-to-date.
 */
val checkSamples by tasks.registering(Exec::class) {
    commandLine("$projectDir/_script/check-samples")
}

val localPublish = rootProject.tasks.findByName("publishToMavenLocal")!!

val buildFirstModel by tasks.registering(RunGradle::class) {
    directory = "$projectDir/_code/first-model"
    task("buildAll")
    dependsOn(localPublish)
}

val updatePluginVersion = tasks.register<UpdatePluginVersion>("updatePluginVersion") {
    directory.set(file("$projectDir/_code/"))
    val validationVersion: String by rootProject.extra
    version.set(validationVersion)
    pluginId.set("io.spine.validation")
}

tasks.register("buildAll") {
    dependsOn(localPublish)
    dependsOn(updatePluginVersion)
    dependsOn(buildFirstModel)
}
