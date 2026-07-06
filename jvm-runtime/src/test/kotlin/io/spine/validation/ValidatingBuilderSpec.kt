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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for the [validate][ValidatingBuilder.validate] default method.
 *
 * The valid and invalid cases are covered against real generated builders in
 * the `tests/validating` module. This spec covers the remaining branch of the
 * contract — a builder whose product does not implement [ValidatableMessage] —
 * which a generated [ValidatingBuilder] can never reach (its product is always
 * a [ValidatableMessage]), and so requires a minimal hand-written builder.
 */
@DisplayName("`ValidatingBuilder.validate()` should")
internal class ValidatingBuilderSpec {

    @Test
    fun `treat a product without validation support as valid`() {
        val builder: ValidatingBuilder<Message> = NonValidatableBuilder()

        builder.validate().shouldBeEmpty()
    }
}

/**
 * A minimal [ValidatingBuilder] whose product is a plain [Message] that does
 * not implement [ValidatableMessage].
 *
 * All [Message.Builder] members are delegated to a real [Empty] builder; only
 * [build] and [buildPartial] are overridden to return the plain message the
 * test needs.
 */
private class NonValidatableBuilder(
    private val delegate: Message.Builder = Empty.newBuilder()
) : Message.Builder by delegate, ValidatingBuilder<Message> {

    override fun build(): Message = Empty.getDefaultInstance()
    override fun buildPartial(): Message = Empty.getDefaultInstance()
}
