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

import io.spine.protodata.ast.Field
import io.spine.protodata.ast.PrimitiveType.TYPE_STRING
import io.spine.protodata.ast.isEnum
import io.spine.protodata.ast.isMessage
import io.spine.protodata.ast.isPrimitive
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.render.SourceFileSet
import io.spine.validation.SetOnceField
import io.spine.validation.java.setonce.SetOnceEnumField
import io.spine.validation.java.setonce.SetOnceJavaCode
import io.spine.validation.java.setonce.SetOnceMessageField
import io.spine.validation.java.setonce.SetOncePrimitiveField
import io.spine.validation.java.setonce.SetOncePrimitiveField.Companion.SupportedPrimitiveTypes
import io.spine.validation.java.setonce.SetOnceStringField

internal class SetOnceValidationRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        val allKnownMessages = findMessageTypes().associateBy { it.message.name }
        val setOnceProtoFields = select<SetOnceField>().all()
        val fieldsToMessages = setOnceProtoFields.associateWith {
            allKnownMessages[it.id.type] ?: error("Messages `${it.id.name}` not found.")
        }

        fieldsToMessages.forEach { (protoField, messageWithFile) ->
            val javaField = javaField(protoField.subject, messageWithFile)
            val sourceFile = sources.javaFileOf(messageWithFile.message)
            javaField.render(sourceFile)
        }
    }

    private fun javaField(field: Field, message: MessageWithFile): SetOnceJavaCode =
        when {
            field.type.isPrimitive -> javaFieldPrimitive(field, message)
            field.type.isMessage -> SetOnceMessageField(field, message)
            field.type.isEnum -> SetOnceEnumField(field, message)
            else -> error("Unsupported `(set_once)` field type: `${field.type}`.")
        }

    private fun javaFieldPrimitive(field: Field, message: MessageWithFile) =
        when (field.type.primitive) {
            TYPE_STRING -> SetOnceStringField(field, message)
            in SupportedPrimitiveTypes -> SetOncePrimitiveField(field, message)
            else -> error("Unsupported `(set_once)` field type: `${field.type.primitive}`.")
        }
}
