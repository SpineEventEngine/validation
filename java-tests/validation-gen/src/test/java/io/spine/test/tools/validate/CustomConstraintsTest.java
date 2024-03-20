/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.test.tools.validate;

import com.google.protobuf.ByteString;
import io.spine.base.FieldPath;
import io.spine.test.tools.validate.rule.BytesAllRequiredFactory;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.option.ValidatingOptionsLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.testing.Correspondences.type;

@DisplayName("Custom constraints should")
@Disabled // https://github.com/SpineEventEngine/mc-java/issues/119
class CustomConstraintsTest {

    @Test
    @DisplayName("be discovered")
    void discovery() {
        assertThat(ValidatingOptionsLoader
                           .INSTANCE
                           .implementations())
                .comparingElementsUsing(type())
                .contains(BytesAllRequiredFactory.class);

    }

    @Test
    @DisplayName("be applied to validated messages")
    void application() {
        var matrix = ByteMatrix.newBuilder()
                .addValue(ByteString.copyFrom(new byte[]{42}))
                .addValue(ByteString.EMPTY)
                .buildPartial();
        var error = matrix.validate();
        assertThat(error)
                .isPresent();
        assertThat(error.get().getConstraintViolationList())
                .comparingExpectedFieldsOnly()
                .containsExactly(ConstraintViolation.newBuilder()
                                         .setFieldPath(FieldPath.newBuilder()
                                                               .addFieldName("value"))
                                         .build());
    }

    @Test
    @DisplayName("be applied to valid messages and pass")
    void validMessages() {
        var matrix = ByteMatrix.newBuilder()
                .addValue(ByteString.copyFrom(new byte[]{42}))
                .build();
        assertThat(matrix.validate())
                .isEmpty();
    }
}
