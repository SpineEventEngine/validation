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

package io.spine.validation.java.ksp

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.kspSourcesDir
import io.kotest.matchers.shouldBe
import io.spine.validation.api.DiscoveredValidators
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
@DisplayName("`ValidatorProcessor` in Kotlin should")
internal class ValidatorProcessorKotlinSpec : ValidatorCompilationTest() {

    @Nested inner class
    Discover {

        @Test
        fun `a top level validator`() = assertDiscovered(
            """
            @Validator(Timestamp::class)
            public class TimestampValidator : MessageValidator<Timestamp> {
                public override fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }    
            """.trimIndent(),
            "TimestampValidator"
        )

        @Test
        fun `a nested validator`() = assertDiscovered(
            """
            public class Outer {
                @Validator(Timestamp::class)
                public class TimestampValidator : MessageValidator<Timestamp> {
                    public override fun validate(message: Timestamp): List<DetectedViolation> {
                        return emptyList() // Always valid.
                    }
                }
            }   
            """.trimIndent(),
            "Outer.TimestampValidator"
        )

        private fun assertDiscovered(declaration: String, expected: String) {
            val sourceFile = kotlinFile("TimestampValidator", """
                package $VALIDATOR_PACKAGE
                
                $IMPORTS
                
                $declaration
                """.trimIndent()
            )

            compilation.sources = listOf(sourceFile)
            val result = compilation.compileSilently()
            result.exitCode shouldBe OK

            val discovered = compilation.kspSourcesDir
                .resolve("resources")
                .resolve(DiscoveredValidators.RESOURCES_LOCATION)

            with(discovered) {
                exists() shouldBe true
                readText() shouldBe "$MESSAGE_CLASS:$VALIDATOR_PACKAGE.$expected\n"
            }
        }
    }

    @Nested inner class
    RejectValidator {

        @Test
        fun `declared as inner class`() {

        }

        @Test
        fun `not implementing 'MessageValidator' interface`() {

        }

        @Test
        fun `not having a public, no-args constructor`() {

        }

        @Test
        fun `validating a local message`() {

        }

        @Test
        fun `validating the same message twice`() {

        }
    }

    companion object {
        private const val MESSAGE_CLASS = "com.google.protobuf.Timestamp"
        private const val VALIDATOR_PACKAGE = "io.spine.validation.java.ksp.test"
        private val IMPORTS = """
            import io.spine.validation.api.DetectedViolation
            import io.spine.validation.api.MessageValidator
            import io.spine.validation.api.Validator
            import com.google.protobuf.Timestamp
        """.trimIndent()
    }
}
