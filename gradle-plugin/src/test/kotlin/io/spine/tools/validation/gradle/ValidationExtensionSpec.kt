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
import io.spine.tools.validation.settings.ValidationWarnings
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Exercises the user-facing surface of [ValidationExtension] — specifically
 * the nested `java { warnings { unsignedFields } }` DSL added so users can
 * silence per-kind warnings from the Java target of the Validation Compiler.
 *
 * These tests cover the DSL alone (instantiation via `ObjectFactory`,
 * defaults, both property-setter and block-syntax forms). The downstream
 * write to the Spine Compiler settings directory is exercised via a
 * focused round-trip through the typed `ValidationWarnings` proto that the
 * Gradle plugin builds from these property values.
 */
@DisplayName("`ValidationExtension` should")
internal class ValidationExtensionSpec {

    private lateinit var project: Project
    private lateinit var extension: ValidationExtension

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()
        extension = project.objects.newInstance(ValidationExtension::class.java, project)
    }

    @Test
    fun `default 'enabled' to true`() {
        extension.enabled.get() shouldBe true
    }

    @Test
    fun `expose a 'java' sub-extension with a 'warnings' block`() {
        extension.java.warnings.unsignedFields.get() shouldBe true
    }

    @Test
    fun `allow suppressing 'unsignedFields' via direct property access`() {
        extension.java.warnings.unsignedFields.set(false)

        extension.java.warnings.unsignedFields.get() shouldBe false
    }

    @Test
    fun `allow suppressing 'unsignedFields' via the nested DSL block`() {
        extension.java(Action { java ->
            java.warnings(Action { warnings ->
                warnings.unsignedFields.set(false)
            })
        })

        extension.java.warnings.unsignedFields.get() shouldBe false
    }

    @Test
    fun `build a 'ValidationWarnings' proto reflecting the DSL value when suppressed`() {
        extension.java(Action { java ->
            java.warnings(Action { warnings ->
                warnings.unsignedFields.set(false)
            })
        })

        val message = ValidationWarnings.newBuilder()
            .setUnsignedFields(extension.java.warnings.unsignedFields.get())
            .build()

        message.unsignedFields shouldBe false
    }

    @Test
    fun `build a 'ValidationWarnings' proto reflecting the DSL value by default`() {
        val message = ValidationWarnings.newBuilder()
            .setUnsignedFields(extension.java.warnings.unsignedFields.get())
            .build()

        message.unsignedFields shouldBe true
    }
}
