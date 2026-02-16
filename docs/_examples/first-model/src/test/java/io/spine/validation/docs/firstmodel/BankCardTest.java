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

package io.spine.validation.docs.firstmodel;

import io.spine.validation.ValidationException;
import io.spine.validation.TemplateStrings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`BankCard` should")
class BankCardTest {

    @Test
    @DisplayName("throw `ValidationException` if digits are invalid")
    void invalidDigits() {
        // #docfragment "invalid-digits"
        assertThrows(ValidationException.class, () ->
            BankCard.newBuilder()
                .setDigits("invalid")
                .setOwner("ALEX SMITH")
                .build()
        );
        // #enddocfragment "invalid-digits"
    }

    @Test
    @DisplayName("throw `ValidationException` if owner is invalid")
    void invalidOwner() {
        assertThrows(ValidationException.class, () ->
            BankCard.newBuilder()
                .setDigits("1234 5678 1234 5678")
                .setOwner("Al")
                .build()
        );
    }

    @Test
    @DisplayName("throw `ValidationException` if tags are not distinct")
    void duplicateTags() {
        assertThrows(ValidationException.class, () ->
            BankCard.newBuilder()
                .setDigits("1234 5678 1234 5678")
                .setOwner("ALEX SMITH")
                .addTags("personal")
                .addTags("personal")
                .build()
        );
    }

    @Test
    @DisplayName("be built if all fields are valid")
    void validCard() {
        assertDoesNotThrow(() ->
            BankCard.newBuilder()
                .setDigits("1234 5678 1234 5678")
                .setOwner("ALEX SMITH")
                .addTags("personal")
                .addTags("travel")
                .build()
        );
    }

    @Test
    @DisplayName("allow multiple words in the owner name")
    void multipleWordsOwner() {
        assertDoesNotThrow(() ->
            BankCard.newBuilder()
                .setDigits("1234 5678 1234 5678")
                .setOwner("John Jacob Jingleheimer Schmidt")
                .build()
        );
    }

    @Test
    @DisplayName("provide a formatted error message for an invalid card")
    void formattedErrorMessage() {
        // #docfragment "error-message"
        var card = BankCard.newBuilder()
                .setOwner("ALEX SMITH")
                .setDigits("wrong number")
                .buildPartial();
        var error = card.validate();
        assertThat(error).isPresent();

        var violation = error.get().getConstraintViolation(0);
        var formatted = TemplateStrings.format(violation.getMessage());

        assertThat(formatted).contains("digits");
        assertThat(formatted).contains("wrong number");
        // #enddocfragment "error-message"
    }
}
