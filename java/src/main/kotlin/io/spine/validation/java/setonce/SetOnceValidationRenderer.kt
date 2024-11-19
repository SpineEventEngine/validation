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

package io.spine.validation.java.setonce

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType.TYPE_BOOL
import io.spine.protodata.ast.PrimitiveType.TYPE_BYTES
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.isList
import io.spine.protodata.ast.isMap
import io.spine.protodata.ast.qualifiedName
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.render.SourceFileSet
import io.spine.string.shortly
import io.spine.validation.SET_ONCE
import io.spine.validation.SetOnceField
import io.spine.validation.java.findMessageTypes
import io.spine.validation.java.setonce.SetOncePrimitiveField.Companion.SupportedPrimitives
import io.spine.validation.java.setonce.SetOnceBooleanField
import io.spine.validation.java.setonce.SetOnceBytesField
import io.spine.validation.java.setonce.SetOnceEnumField
import io.spine.validation.java.setonce.SetOnceJavaConstraints
import io.spine.validation.java.setonce.SetOnceMessageField
import io.spine.validation.java.setonce.SetOnceNumberField
import io.spine.validation.java.setonce.SetOnceNumberField.Companion.SupportedNumbers
import io.spine.validation.java.setonce.SetOnceStringField

/**
 * Takes the discovered [SetOnceField]s and modifies their Java builders to make sure
 * that the field value is assigned only once.
 *
 * Along with the direct field setter, auxiliary setters and merge methods are also affected
 * to enforce the constraint. For different field types, different methods are modified.
 * Take a look on [SetOnceJavaConstraints] and its inheritors for details.
 */
internal class SetOnceValidationRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        val compilationMessages = findMessageTypes().associateBy { it.message.name }
        val setOnceFields = setOnceFields().filter { it.enabled }
        setOnceFields
            .associateWith { compilationMessages[it.id.type]!! }
            .forEach { (protoField, declaredIn) ->
                val javaConstraints = javaConstraints(protoField.subject)
                val sourceFile = sources.javaFileOf(declaredIn.message)
                javaConstraints.render(sourceFile)
            }
    }

    private fun setOnceFields() = select<SetOnceField>().all()
        .onEach {
            val field = it.subject
            check(!field.isMap && !field.isList) {
                "The `($SET_ONCE)` option is not applicable to repeated fields or maps. " +
                        "The invalid field: `${field.qualifiedName}`."
            }
        }

    private fun javaConstraints(field: Field): SetOnceJavaConstraints {
        val typeSystem = typeSystem!!
        return when {
            field.type.isMessage -> SetOnceMessageField(field, typeSystem)
            field.type.isEnum -> SetOnceEnumField(field, typeSystem)
            field.type.primitive in SupportedPrimitives -> SetOncePrimitiveField(field, typeSystem)
            else -> error(
                "Cannot define constraints for the field `${field.qualifiedName}` which has" +
                        " the type `${field.type.shortly()}` not supported by" +
                        " the `($SET_ONCE)` option."
            )
        }
    }
    private fun javaConstraints(field: Field, message: MessageWithFile): SetOnceJavaConstraints<*> =
        when {
            field.type.isMessage -> SetOnceMessageField(field, message)
            field.type.isEnum -> SetOnceEnumField(field, message)
            field.type.isPrimitive -> forPrimitives(field, message)
            else -> throwUnsupportedType(field)
        }

    private fun forPrimitives(field: Field, message: MessageWithFile): SetOnceJavaConstraints<*> =
        when (field.type.primitive) {
            TYPE_STRING -> SetOnceStringField(field, message)
            TYPE_BOOL -> SetOnceBooleanField(field, message)
            TYPE_BYTES -> SetOnceBytesField(field, message)
            in SupportedNumbers -> SetOnceNumberField(field, message)
            else -> throwUnsupportedType(field)
        }

    private fun throwUnsupportedType(field: Field): Nothing = error(
        "Unsupported `(set_once)` field type: `${field.type}`, " +
                "the declaring message: `${field.declaringType}`."
    )
}
