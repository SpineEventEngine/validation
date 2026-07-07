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

package io.spine.tools.validation.gradle

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Verifies the Maven coordinates and the compiler-plugin class name exposed by
 * [ValidationSdk], which the Validation Gradle plugin uses to wire user projects.
 */
@DisplayName("`ValidationSdk` should")
internal class ValidationSdkSpec {

    @Test
    fun `expose the JVM runtime artifact`() {
        ValidationSdk.jvmRuntime.group shouldBe "io.spine"
        ValidationSdk.jvmRuntime.name shouldBe "spine-validation-jvm-runtime"
    }

    @Test
    fun `expose the Java codegen bundle artifact`() {
        ValidationSdk.javaCodegenBundle.group shouldBe "io.spine.tools"
        ValidationSdk.javaCodegenBundle.name shouldBe "validation-java-bundle"
    }

    @Test
    fun `expose the fully qualified name of the Java compiler plugin`() {
        ValidationSdk.javaCompilerPlugin shouldBe
                "io.spine.tools.validation.java.JavaValidationPlugin"
    }
}
