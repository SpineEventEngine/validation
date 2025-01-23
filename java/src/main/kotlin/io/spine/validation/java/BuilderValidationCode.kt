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

package io.spine.validation.java

import com.intellij.psi.PsiClass
import io.spine.protodata.java.ClassName
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.createInterfaceReference
import io.spine.tools.psi.java.implement
import io.spine.tools.psi.java.method
import io.spine.validate.NonValidated
import io.spine.validate.Validated
import io.spine.validate.ValidatingBuilder
import io.spine.validation.java.psi.addBefore
import io.spine.validation.java.psi.createStatementsFromText
import io.spine.validation.java.psi.deepSearch

internal class BuilderValidationCode(private val messageClassName: ClassName) {

    fun render(builderClass: PsiClass) =
        with(builderClass) {
            implementInterface()
            injectValidation()
            annotateBuild()
            annotateBuildPartial()
        }

    private fun PsiClass.implementInterface() {
        val qualifiedName = ValidatingBuilder::class.java.canonicalName
        val genericParameter = messageClassName.canonical
        val reference = elementFactory.createInterfaceReference(qualifiedName, genericParameter)
        implement(reference)
    }

    private fun PsiClass.injectValidation() = method("build").run {
        val returningResult = deepSearch("return result;")
        val invokeValidate = elementFactory.createStatementsFromText(
            """
            java.util.Optional<io.spine.validate.ValidationError> error = result.validate();
            if (error.isPresent()) {
                var violations = error.get().getConstraintViolationList();
                throw new io.spine.validate.ValidationException(violations);
            }
            """.trimIndent(), this
        )
        body!!.addBefore(invokeValidate, returningResult)
    }

    private fun PsiClass.annotateBuild() = method("build")
        .run { returnTypeElement!!.addAnnotation(Validated::class.qualifiedName!!) }

    private fun PsiClass.annotateBuildPartial() = method("buildPartial")
        .run { returnTypeElement!!.addAnnotation(NonValidated::class.qualifiedName!!) }
}
