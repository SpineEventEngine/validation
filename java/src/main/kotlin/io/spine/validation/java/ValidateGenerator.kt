/*
 * Copyright 2024, TeamDev. All rights reserved.
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

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.ast.Cardinality.CARDINALITY_LIST
import io.spine.protodata.ast.name
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.ReadVar
import io.spine.protodata.java.field
import io.spine.string.titleCase
import io.spine.tools.java.codeBlock
import io.spine.validate.ConstraintViolation
import io.spine.validate.Validate
import io.spine.validate.ValidationError
import io.spine.validation.refersToAny
import io.spine.validation.refersToMessage
import java.util.*

/**
 * Generates code for the [RecursiveValidation][io.spine.validation.RecursiveValidation] operator.
 */
internal class ValidateGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    private val validationError =
        readVar<Optional<ValidationError>>(prefix = "validationError", ctx)

    private val violationList =
        readVar<List<ConstraintViolation>>(prefix = "violationList", ctx)

    init {
        val field = ctx.simpleRuleField
        val fieldType = field.type
        check(fieldType.refersToMessage()) {
            "The `(validate)` option supports only `Message` types," +
                    " but the field `${field.qualifiedName}` has the type `${fieldType.name}`."
        }
    }

    /**
     * Compose a variable name after the format `prefixOfFieldName`.
     * E.g., `validationErrorOfUserId`.
     */
    private fun <T> readVar(prefix: String, ctx: GenerationContext): ReadVar<T> {
        val fieldNameSuffix = ctx.simpleRuleField.name.value.titleCase()
        return ReadVar("${prefix}Of$fieldNameSuffix")
    }

    override fun prologue(): CodeBlock {
        return if (field.type.refersToAny()) {
            codeBlock {
                add(unpackAndValidate())
                add(wrapIntoError())
            }
        } else {
            useGeneratedMethod()
        }
    }

    /**
     * Generates the code obtaining an optional `ValidationError` by invoking Spine-generated
     * `.validate()` method of the validated field.
     *
     * NOTE: such an approach will not work for the types which code was not generated by Spine,
     * such as built-in Google Protobuf types.
     *
     * Sample output:
     * ```
     *     Optional<ValidationError> [validationErrorVar] = this.get[fieldName]().validate();
     * ```
     */
    private fun useGeneratedMethod(): CodeBlock = codeBlock {
        val error = MethodCall<Optional<ValidationError>>(ctx.fieldOrElement!!, "validate")
        addStatement(
            "\$T<\$T> \$L = \$L",
            Optional::class.java,
            ValidationError::class.java,
            validationError,
            error
        )
    }

    /**
     * Generates the code which unpacks the value of the `Any` field and invokes the plain-old
     * `io.spine.validate.Validate.violationsOf(..)`
     *
     * Sample output:
     *
     * ```
     *     List<ConstraintViolation> [violationListVar] =
     *         Validate.violationsOf(AnyPacker.unpack(this.get[fieldName]()));
     * ```
     */
    private fun unpackAndValidate(): CodeBlock = codeBlock {
        addStatement(
            "\$T<\$T> \$L = \$T.violationsOf(\$L)",
            List::class.java,
            ConstraintViolation::class.java,
            violationList,
            Validate::class.java,
            ctx.fieldOrElement!!
        )
    }

    /**
     * Generates the code wrapping the value of [violationList] into
     * an optional `ValidationError`.
     *
     * Sample output:
     *
     * ```
     *     Optional<ValidationError> [validationErrorVar] =
     *         Optional.ofNullable([violationListVar].isEmpty()
     *              ? null
     *              : ValidationError.newBuilder()
     *                     .addAllConstraintViolation([violationListVar])
     *                     .build()
     *        );
     * ```
     */
    private fun wrapIntoError(): CodeBlock = codeBlock {
        addStatement(
            "var \$L = \$T.ofNullable(" +
                    "\$L.isEmpty() ? null " +
                    ": \$T.newBuilder().addAllConstraintViolation(\$L).build()" +
                    ")",
            validationError,
            Optional::class.java,
            violationList,
            ValidationError::class.java,
            violationList
        )
    }

    override fun condition(): Expression<Boolean> =
        Expression("!" + MethodCall<Boolean>(validationError, "isPresent"))

    override fun createViolation(): CodeBlock {
        val validationError = MethodCall<ValidationError>(validationError, "get")
        val violations = validationError
            .field("constraint_violation", CARDINALITY_LIST)
            .getter<MutableList<ConstraintViolation>>()
        return CodeBlock.builder()
            .addStatement("\$L.addAll(\$L)", ctx.violationList, violations)
            .build()
    }
}
