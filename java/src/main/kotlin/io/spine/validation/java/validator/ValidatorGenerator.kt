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

package io.spine.validation.java.validator

import com.google.protobuf.Message
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.MessageType
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.CodeBlock
import io.spine.protodata.java.Expression
import io.spine.protodata.java.field
import io.spine.protodata.java.javaClassName
import io.spine.protodata.type.TypeSystem
import io.spine.validate.ConstraintViolation
import io.spine.validation.api.expression.orElse
import io.spine.validation.api.expression.resolve
import io.spine.validation.api.generate.MessageScope.message
import io.spine.validation.api.generate.SingleOptionCode
import io.spine.validation.api.generate.ValidateScope.parentName
import io.spine.validation.api.generate.ValidateScope.parentPath
import io.spine.validation.api.generate.ValidateScope.violations

internal typealias ValidatorClass = ClassName
internal typealias MessageClass = ClassName

internal class ValidatorGenerator(
    private val validators: Map<MessageClass, ValidatorClass>,
    private val typeSystem: TypeSystem
) {

    /*
    // TODO:2025-05-23:yevhenii.nadtochii: Check out.

         - Should we handle `repeated` and `map` fields?
           Yes -> We support.

         - Do we allow custom validators for local messages?
           No -> Error.

         - Can one message have several validators?
           No -> Error.

         + Validator must be a top-level class or nested.

         + Validator must have a public, no args constructor.

         + Validator authors are fully responsible for the instance of `ConstraintViolation`.

         + Specs of this feature must go to the interface `MessageValidator`.

     */

    fun codeFor(type: MessageType): List<SingleOptionCode> {
        val validatorsToApply = type.fieldList.mapNotNull { field ->
            val messageClass = field.type.javaClassName(typeSystem)
            validators[messageClass]?.let {
                field to it
            }
        }.toMap()
        return validatorsToApply.map { (field, validator) ->
            ApplyValidator(field, validator).code()
        }
    }
}

private class ApplyValidator(
    private val field: Field,
    private val validator: ValidatorClass
) {

    fun code(): SingleOptionCode {
        val getter = message.field(field).getter<Message>()
        val fieldPath = parentPath.resolve(field.name)
        val typeName = parentName.orElse(field.declaringType)
        val invokeValidator = Expression<List<ConstraintViolation>>(
            "new $validator().validate($getter, $fieldPath, $typeName)"
        )
        val constraint = CodeBlock("$violations.addAll($invokeValidator);")
        return SingleOptionCode(constraint)
    }
}
