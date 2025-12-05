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

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.INTERNAL_ERROR
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.kspSourcesDir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.spine.string.qualified
import io.spine.validate.MessageValidator
import io.spine.validation.jvm.DiscoveredValidators
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
        fun `a top level validator`() = assertDiscovered("TimestampValidator") {
            """
            @Validator(Timestamp::class)
            public class TimestampValidator : MessageValidator<Timestamp> {
                public override fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }    
            """.trimIndent()
        }

        @Test
        fun `a nested validator`() = assertDiscovered("Outer.TimestampValidator") {
            """
            public class Outer {
                @Validator(Timestamp::class)
                public class TimestampValidator : MessageValidator<Timestamp> {
                    public override fun validate(message: Timestamp): List<DetectedViolation> {
                        return emptyList() // Always valid.
                    }
                }
            }   
            """.trimIndent()
        }

        private fun assertDiscovered(validator: String, declaration: () -> String) {
            val sourceFile = kotlinFile("TimestampValidator", """
                package $VALIDATOR_PACKAGE
                
                $IMPORTS
                
                ${declaration()}
                """.trimIndent()
            )

            compilation.sources = listOf(sourceFile)
            val result = compilation.compileSilently()
            result.exitCode shouldBe OK

            val validators = DiscoveredValidators.resolve(compilation.kspSourcesDir)
            with(validators) {
                exists() shouldBe true
                readText() shouldBe "$TIMESTAMP_CLASS:$VALIDATOR_PACKAGE.$validator\n"
            }
        }
    }

    @Nested inner class
    RejectValidator {

        @Test
        fun `declared as 'inner' class`() = assertRejects(
            """
            public class Outer {
                @Validator(Timestamp::class)
                public inner class TimestampValidator : MessageValidator<Timestamp> {
                    public override fun validate(message: Timestamp): List<DetectedViolation> {
                        return emptyList() // Always valid.
                    }
                }
            }   
            """.trimIndent()
        ) { error ->
            error shouldContain "$VALIDATOR_PACKAGE.Outer.TimestampValidator"
            error shouldContain "This annotation is not applicable to the `inner` classes."
        }

        @Test
        fun `not implementing 'MessageValidator' interface`() = assertRejects(
            """
            @Validator(Timestamp::class)
            public class TimestampValidator {
                public fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }
            """.trimIndent()
        ) { error ->
            error shouldContain "$VALIDATOR_PACKAGE.TimestampValidator"
            error shouldContain "requires the target class to implement" +
                    " the `${qualified<MessageValidator<*>>()}`"
        }

        @Test
        fun `not having a public, no-args constructor`() = assertRejects(
            """
            @Validator(Timestamp::class)
            public class TimestampValidator private constructor() : MessageValidator<Timestamp> {
                public fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }
            """.trimIndent()
        ) { error ->
            error shouldContain "$VALIDATOR_PACKAGE.TimestampValidator"
            error shouldContain "requires the target class to have a public, no-args constructor"
        }

        @Test
        fun `having different message type for annotation and interface`() = assertRejects(
            """
            @Validator(Timestamp::class)
            public class DurationValidator : MessageValidator<Duration> {
                public override fun validate(message: Duration): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }    
            """.trimIndent()
        ) { error ->
            error shouldContain "$VALIDATOR_PACKAGE.DurationValidator"
            error shouldContain TIMESTAMP_CLASS
            error shouldContain DURATION_CLASS
            error shouldContain "message type of the annotation and the validator must match"
        }

        @Test
        fun `validating the same message twice`() = assertRejects(
            """
            @Validator(Timestamp::class)
            public class TimestampValidator : MessageValidator<Timestamp> {
                public override fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }
            
            @Validator(Timestamp::class)
            public class TimestampValidator2 : MessageValidator<Timestamp> {
                public override fun validate(message: Timestamp): List<DetectedViolation> {
                    return emptyList() // Always valid.
                }
            }    
            """.trimIndent()
        ) { error ->
            error shouldContain TIMESTAMP_CLASS
            error shouldContain "$VALIDATOR_PACKAGE.TimestampValidator"
            error shouldContain "$VALIDATOR_PACKAGE.TimestampValidator2"
            error shouldContain "Only one validator is allowed per message type"
        }

        private fun assertRejects(declaration: String, errorMessageAssertions: (String) -> Unit) {
            val sourceFile = kotlinFile("TimestampValidator", """
                package $VALIDATOR_PACKAGE
                
                $IMPORTS
                
                $declaration
                """.trimIndent()
            )

            compilation.sources = listOf(sourceFile)
            val result = compilation.compileSilently()

            result.exitCode shouldBe INTERNAL_ERROR
            errorMessageAssertions(result.messages)
        }
    }
}

private const val TIMESTAMP_CLASS = "com.google.protobuf.Timestamp"
private const val DURATION_CLASS = "com.google.protobuf.Duration"
private const val VALIDATOR_PACKAGE = "io.spine.validation.java.ksp.test"
private val IMPORTS = """
            import io.spine.validate.DetectedViolation
            import io.spine.validate.MessageValidator
            import io.spine.validate.Validator
            import $TIMESTAMP_CLASS
            import $DURATION_CLASS
        """.trimIndent()
