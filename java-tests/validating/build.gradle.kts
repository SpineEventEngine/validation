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

import io.spine.dependency.artifact
import io.spine.dependency.lib.AutoService
import io.spine.dependency.local.Base
import io.spine.dependency.local.Logging
import io.spine.dependency.local.ProtoData
import io.spine.dependency.local.TestLib
import io.spine.dependency.local.Time
import io.spine.dependency.local.Validation
import io.spine.dependency.test.JUnit
import io.spine.dependency.test.JUnit.Jupiter
import io.spine.dependency.test.Kotest
import io.spine.dependency.test.Truth

plugins {
    `java-test-fixtures`
}

dependencies {
    testFixturesAnnotationProcessor(AutoService.processor)
    testFixturesCompileOnly(AutoService.annotations)

    val testFixtureDependencies = listOf(
        Base.lib,
        Time.lib,
        Logging.lib,
        Validation.runtime,
        Kotest.assertions
    ) + JUnit.artifacts.values + Truth.libs

    testFixtureDependencies.forEach {
        testFixturesImplementation(it)
    }

    testImplementation(Jupiter.artifact { params })
    testImplementation(TestLib.lib)
    testImplementation(Time.lib)
    testImplementation(ProtoData.api)
}

testProtoDataRemoteDebug(enabled = false)

/**
 * Sets a dependency for the KSP task to avoid the Gradle warning on a missing dependency.
 *
 * Note that this block uses [Task.mustRunAfter] because it does not require the task
 * existence to declare a dependency.
 */
afterEvaluate {
    tasks.named("kspTestFixturesKotlin") {
        mustRunAfter("launchTestFixturesProtoData")
    }
}
