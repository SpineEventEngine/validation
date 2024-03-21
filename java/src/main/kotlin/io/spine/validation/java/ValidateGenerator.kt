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

import com.squareup.javapoet.CodeBlock
import io.spine.protodata.Field
import io.spine.protodata.Type
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.MessageReference
import io.spine.protodata.java.MethodCall
import io.spine.protodata.qualifiedName
import io.spine.string.titleCase
import io.spine.tools.java.codeBlock
import io.spine.validate.ConstraintViolation
import io.spine.validate.Validate
import io.spine.validate.ValidationError
import java.util.*

/**
 * Generates code for the [RecursiveValidation][io.spine.validation.RecursiveValidation] operator.
 */
internal class ValidateGenerator(ctx: GenerationContext) : SimpleRuleGenerator(ctx) {

    private val validationErrorVar = varName(prefix = "validationError", ctx)

    private val violationListVar = varName(prefix = "violationList", ctx)

    init {
        val field = ctx.simpleRuleField
        val fieldType = field.type
        check(fieldType.hasMessage()) {
            "The `(validate)` option supports only `Message` types," +
                    " but the field `${field.qualifiedName}` has the type `$fieldType`."
        }
    }

    /**
     * Compose a variable name after the format `prefixOfFieldName`.
     * E.g., `validationErrorOfUserId`.
     */
    private fun varName(prefix: String, ctx: GenerationContext): Literal {
        val fieldNameSuffix = ctx.simpleRuleField.name.value.titleCase()
        return Literal("${prefix}Of$fieldNameSuffix")
    }

    override fun prologue(): CodeBlock {
        return if (field.type.isAny) {
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
     * NOTE: such an approach won't work for the types which code was not generated by Spine, such
     * as built-in Google Protobuf types.
     *
     * Sample output:
     * ```
     *     Optional<ValidationError> [validationErrorVar] = this.get[fieldName]().validate();
     * ```
     */
    private fun useGeneratedMethod(): CodeBlock = codeBlock {
        val violations = MethodCall(ctx.fieldOrElement!!, "validate")
        addStatement(
            "\$T<\$T> \$L = \$L",
            Optional::class.java,
            ValidationError::class.java,
            validationErrorVar,
            violations
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
            violationListVar,
            Validate::class.java,
            ctx.fieldOrElement!!
        )
    }

    /**
     * Generates the code wrapping the value of [violationListVar] into
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
            validationErrorVar,
            Optional::class.java,
            violationListVar,
            ValidationError::class.java,
            violationListVar
        )
    }

    override fun condition(): Expression =
        Literal("!" + MethodCall(validationErrorVar, "isPresent"))


    override fun createViolation(): CodeBlock {
        val validationError = MethodCall(validationErrorVar, "get")
        val violations = MessageReference(validationError.toCode())
            .field("constraint_violation", Field.CardinalityCase.LIST)
            .getter
        return error().createParentViolation(ctx, violations)
    }
}

/**
 * Tells if this type is `google.protobuf.Any`.
 *
 * TODO: Migrate to the similar property from ProtoData.
 */
private val Type.isAny: Boolean
    get() = (hasMessage()
            && message.packageName.equals("google.protobuf"))
            && message.simpleName.equals("Any")


/**
 * Obtains the name of the field which includes a qualified name of the type which declares it.
 *
 * TODO: Migrate to an extension `val` from ProtoData.
 */
private val Field.qualifiedName: String
    get() = "${declaringType.qualifiedName}.${name.value}"
