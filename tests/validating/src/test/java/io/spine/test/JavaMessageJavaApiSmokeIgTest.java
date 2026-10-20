/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package io.spine.test;

import io.spine.test.protobuf.CardNumber;
import io.spine.type.TypeName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generated Java code for a message when called from Java should")
class JavaMessageJavaApiSmokeIgTest {

    /**
     * The builder which should produce a valid instance.
     */
    private final CardNumber.Builder valid =
            CardNumber.newBuilder()
                    .setDigits("0000 0000 0000 0000");

    @Test
    @DisplayName("throw `NullPointerException` if given 'null' for the parent path")
    @SuppressWarnings("DataFlowIssue" /* We pass `null` intentionally */)
    void rejectNullParentPath() {
        var message = valid.build();
        assertThrows(NullPointerException.class, () ->
                message.validate(null, TypeName.of("Something"))
        );
    }
}
