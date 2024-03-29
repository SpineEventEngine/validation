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

package io.spine.test.tools.validate;

import io.spine.base.FieldPath;
import io.spine.type.TypeName;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static io.spine.base.Time.currentTime;

@DisplayName("`(goes)` option should be compiled so that")
@Disabled // https://github.com/SpineEventEngine/mc-java/issues/119
class GoesConstraintTest {

    @Test
    @DisplayName("if the associated field is not set and the target field is set, " +
            "a violation is produced")
    void failIfNotSet() {
        var paper = Paper.newBuilder()
                .setWhenArchived(currentTime())
                .buildPartial();
        var error = paper.validate();
        assertThat(error)
                .isPresent();
        var violations = error.get().getConstraintViolationList();
        assertThat(violations)
                .hasSize(1);
        assertThat(violations.get(0))
                .comparingExpectedFieldsOnly()
                .isEqualTo(ConstraintViolation.newBuilder()
                                   .setTypeName(TypeName.of(paper).value())
                                   .setFieldPath(FieldPath.newBuilder()
                                                         .addFieldName("when_archived"))
                                   .build());
    }

    @Test
    @DisplayName("if both fields are set, no violation")
    void bothSet() {
        var paper = Paper.newBuilder()
                .setArchiveId(ArchiveId.generate())
                .setWhenArchived(currentTime())
                .build();
        assertThat(paper.validate()).isEmpty();
    }

    @Test
    @DisplayName("if neither field is set, no violation")
    void noneSet() {
        var paper = Paper.newBuilder().build();
        assertThat(paper.validate()).isEmpty();
    }

    @Test
    @DisplayName("if the associated field is set and target is not set, no violation")
    void targetNotSet() {
        var paper = Paper.newBuilder()
                .setArchiveId(ArchiveId.generate())
                .build();
        assertThat(paper.validate()).isEmpty();
    }
}
