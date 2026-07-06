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

package io.spine.validation

import com.google.protobuf.Empty
import com.google.protobuf.Message
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.base.FieldPath
import io.spine.string.templateString
import io.spine.type.TypeName
import java.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Unit tests for the [validate][ValidatingBuilder.validate] default method.
 *
 * The method probes the builder's product: it collects the violations reported
 * by a [ValidatableMessage], and treats a product that does not support
 * validation as valid. Real generated builders always produce a
 * [ValidatableMessage], so these tests use minimal stubs to cover each branch of
 * the contract in isolation — including the non-[ValidatableMessage] case that a
 * generated builder can never reach. The valid and invalid cases are also
 * verified end-to-end against generated code in the `tests/validating` module.
 */
@DisplayName("`ValidatingBuilder.validate()` should")
internal class ValidatingBuilderSpec {

    private val violation = constraintViolation {
        message = templateString { withPlaceholders = "The field is required." }
    }

    @Test
    fun `report no violations when the validatable product is valid`() {
        val builder = StubBuilder(StubValidatable(emptyList()))

        builder.validate().shouldBeEmpty()
    }

    @Test
    fun `report the violations of an invalid validatable product without throwing`() {
        val builder = StubBuilder(StubValidatable(listOf(violation)))

        val violations = assertDoesNotThrow { builder.validate() }

        violations shouldBe listOf(violation)
    }

    @Test
    fun `treat a product without validation support as valid`() {
        val builder = StubBuilder(Empty.getDefaultInstance())

        builder.validate().shouldBeEmpty()
    }
}

/**
 * A minimal [ValidatingBuilder] that yields the given [product] from both
 * [build] and [buildPartial]; all other [Message.Builder] members are delegated
 * to a real [Empty] builder.
 */
private class StubBuilder(
    private val product: Message,
    private val delegate: Message.Builder = Empty.newBuilder()
) : Message.Builder by delegate, ValidatingBuilder<Message> {

    override fun build(): Message = product
    override fun buildPartial(): Message = product
}

/**
 * A minimal [ValidatableMessage] reporting the given [violations] from its
 * [validate] method; all [Message] members are delegated to a plain [Empty].
 */
private class StubValidatable(
    private val violations: List<ConstraintViolation>,
    private val delegate: Message = Empty.getDefaultInstance()
) : Message by delegate, ValidatableMessage {

    override fun validate(parentPath: FieldPath, parentName: TypeName?): Optional<ValidationError> =
        if (violations.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(validationError { constraintViolation.addAll(violations) })
        }
}
