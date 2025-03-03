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

package io.spine.validation.java.rule

import com.google.protobuf.Message
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.FieldName
import io.spine.protodata.ast.File
import io.spine.protodata.ast.TypeName
import io.spine.protodata.java.Expression
import io.spine.protodata.java.MessageOrEnumConvention
import io.spine.protodata.java.JavaValueConverter
import io.spine.protodata.java.field
import io.spine.protodata.type.TypeSystem
import io.spine.validate.ConstraintViolation
import io.spine.validation.Rule
import io.spine.validation.isSimple

/**
 * Context of a [CodeGenerator].
 */
public data class GenerationContext
@JvmOverloads
internal constructor(

    /**
     * The Protobuf types known to the application.
     */
    val typeSystem: TypeSystem,

    /**
     * The rule for which the code is generated.
     */
    val rule: Rule,

    /**
     * A reference to the validated message.
     */
    val msg: Expression<Message>,

    /**
     * The type of the validated message.
     */
    val validatedType: TypeName,

    /**
     * The path to the Protobuf file where the validated type is declared.
     */
    val protoFile: File,

    /**
     * A reference to the mutable violations list, which accumulates all the constraint violations.
     *
     * This is a variable name in the generated code. The variable holds
     * a list of [io.spine.validate.ConstraintViolation]s. when a new violation is discovered,
     * the generated code should add it to this list.
     */
    val violationList: Expression<MutableList<ConstraintViolation>>,

    /**
     * A custom reference to an element of a collection field.
     *
     * If `null`, the associated field is not a collection, or the associated rule
     * does not need to be distributed to collection elements.
     */
    private val elementReference: Expression<*>? = null
) {

    val typeConvention: MessageOrEnumConvention by lazy {
        MessageOrEnumConvention(typeSystem)
    }

    /**
     * A [JavaValueConverter] for transforming `Value`s into Java expressions.
     */
    val valueConverter: JavaValueConverter by lazy {
        JavaValueConverter(typeSystem)
    }

    val otherValueAsCode: Expression<*>?
        get() = if (rule.isSimple && rule.simple.hasOtherValue()) {
            valueConverter.valueToCode(rule.simple.otherValue)
        } else {
            null
        }

    /**
     * The field associated with the given rule.
     *
     * If the [rule] is not a simple rule, the value is absent.
     *
     * @see simpleRuleField
     */
    val fieldFromSimpleRule: Field?
        get() = if (rule.isSimple) {
            lookUpField(rule.simple.field)
        } else {
            null
        }

    /**
     * Obtains a name of the field associated with the simple rule of this context.
     *
     * @throws IllegalStateException if the rule is not a simple rule.
     * @see [fieldFromSimpleRule]
     */
    val simpleRuleField: Field
        get() {
            check(rule.isSimple) { "The rule is not a simple one: `$rule`." }
            return fieldFromSimpleRule!!
        }

    /**
     * If the associated field is a collection and the associated rule needs to be distributed,
     * this is a reference to one element of the collection.
     *
     * If the field is not a collection or the rule does not need to be distributed,
     * this is the reference to the field.
     *
     * If there is no associated field, this is `null`.
     */
    val fieldOrElement: Expression<*>?
        get() = elementReference ?: fieldValue

    /**
     * The reference to the associated field, or `null` if there is no such field.
     */
    val fieldValue: Expression<*>?
        get() {
            val protoField = fieldFromSimpleRule ?: return null
            return getterFor(protoField)
        }

    /**
     * `true` if the [fieldOrElement] contains a reference to an element of a collection,
     * `false` otherwise.
     */
    val isElement: Boolean = elementReference != null

    /**
     * Obtains a getter for a given field.
     *
     * The [field] should be a field of the message referenced in [msg].
     */
    public fun getterFor(field: Field): Expression<*> =
        msg.field(field).getter<Any>()

    /**
     * Finds the field by the given name in the validated type.
     *
     * @throws IllegalArgumentException if there is no such field
     */
    public fun lookUpField(name: FieldName): Field =
        typeSystem.findMessage(validatedType)!!
            .first.fieldList
            .first { it.name == name }
}
