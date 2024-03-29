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

package io.spine.test.protobuf;

import io.spine.validate.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.validate.Validate.check;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generated message should")
class GeneratedMessageTest {

    private final CardNumber.Builder valid = CardNumber.newBuilder()
            .setDigits("0000 0000 0000 0000");
    private final CardNumber.Builder invalid = CardNumber.newBuilder()
            .setDigits("zazazazazazaz");

    @Test
    @DisplayName("create a valid message using `build()`")
    void obtainValid() {
        var number = valid.build();
        check(number);
    }

    @Test
    @DisplayName("throw `ValidationException` if the message is not valid")
    void throwIfInvalid() {
        var exception = assertThrows(ValidationException.class, invalid::build);
        assertThat(exception.getConstraintViolations()).isNotEmpty();
    }

    @Test
    @DisplayName("ignore invalid message when skipping validation intentionally")
    void ignoreIfPartial() {
        var number = invalid.buildPartial();
        assertThat(number).isNotNull();
        assertThrows(ValidationException.class, () -> check(number));
    }
}
