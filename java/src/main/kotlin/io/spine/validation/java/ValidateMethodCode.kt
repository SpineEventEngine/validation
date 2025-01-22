/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.validation.java

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.squareup.javapoet.CodeBlock
import io.spine.string.joinByLines
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Wraps the passed constraints code into a method.
 */
internal class ValidateMethodCode(
    private val constraints: List<CodeBlock>
) {

    fun generate(psiClass: PsiClass): PsiMethod {
        return elementFactory.createMethodFromText(
            """
            |public java.util.Optional<io.spine.validate.ValidationError> validate() {
            |    var violations = new java.util.ArrayList<io.spine.validate.ConstraintViolation>();
            |    
            |    ${constraints.joinByLines()}
            |    
            |    if (!violations.isEmpty()) {
            |        return java.util.Optional.of(io.spine.validate.ValidationError.newBuilder().addAllConstraintViolation(violations).build());
            |    } else {
            |        return java.util.Optional.empty();
            |     }
            |}
            """.trimMargin(), psiClass
        )
    }
}
