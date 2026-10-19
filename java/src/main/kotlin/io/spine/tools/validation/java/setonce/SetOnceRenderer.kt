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

package io.spine.tools.validation.java.setonce

import io.spine.server.query.Querying
import io.spine.server.query.select
import io.spine.string.shortly
import io.spine.tools.compiler.ast.Field
import io.spine.tools.compiler.ast.PrimitiveType
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BOOL
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_BYTES
import io.spine.tools.compiler.ast.PrimitiveType.TYPE_STRING
import io.spine.tools.compiler.ast.qualifiedName
import io.spine.tools.compiler.jvm.file.hasJavaRoot
import io.spine.tools.compiler.jvm.render.JavaRenderer
import io.spine.tools.compiler.jvm.render.findMessageTypes
import io.spine.tools.compiler.render.SourceFileSet
import io.spine.tools.validation.java.setonce.SetOnceNumberField.Companion.SupportedNumbers
import io.spine.tools.validation.option.SET_ONCE
import io.spine.tools.validation.SetOnceField
import io.spine.tools.validation.sortedByDeclaration

/**
 * Takes the discovered [SetOnceField]s and modifies their Java builders to make sure
 * that the field value is assigned only once.
 *
 * Along with the direct field setter, auxiliary setters and merge methods are also affected
 * to enforce the constraint. For different field types, different methods are modified.
 * Take a look on [SetOnceJavaConstraints] and its inheritors for details.
 */
internal class SetOnceRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        // We receive `grpc` and `kotlin` output sources roots here as well.
        // As for now, we modify only `java` sources.
        if (!sources.hasJavaRoot) {
            return
        }

        val allCompilationMessages = findMessageTypes().associateBy { it.name }
        // Process the fields of each message in the order of declaration, so that
        // the code added to the shared builder methods, such as `mergeFrom()`,
        // goes in the same order from build to build.
        (this as Querying).select<SetOnceField>().all()
            .sortedByDeclaration()
            .associateWith { allCompilationMessages[it.id.type]!! }
            .forEach { (protoField, declaredIn) ->
                val javaConstraints = javaConstraints(protoField.subject, protoField.errorMessage)
                val sourceFile = sources.javaFileOf(declaredIn)
                javaConstraints.render(sourceFile)
            }
    }

    private fun javaConstraints(field: Field, errorTemplate: String): SetOnceJavaConstraints<*> =
        when {
            field.type.isMessage -> SetOnceMessageField(field, typeSystem, errorTemplate)
            field.type.isEnum -> SetOnceEnumField(field, typeSystem, errorTemplate)
            field.type.isPrimitive -> javaConstraints(field, field.type.primitive, errorTemplate)
            else -> throwUnsupportedType(field)
        }

    private fun javaConstraints(
        field: Field,
        type: PrimitiveType,
        errorTemplate: String
    ): SetOnceJavaConstraints<*> =
        when (type) {
            TYPE_STRING -> SetOnceStringField(field, typeSystem, errorTemplate)
            TYPE_BOOL -> SetOnceBooleanField(field, typeSystem, errorTemplate)
            TYPE_BYTES -> SetOnceBytesField(field, typeSystem, errorTemplate)
            in SupportedNumbers -> SetOnceNumberField(field, typeSystem, errorTemplate)
            else -> throwUnsupportedType(field)
        }

    private fun throwUnsupportedType(field: Field): Nothing = error(
        "Cannot define constraints for the field `${field.qualifiedName}`, which has" +
                " the type `${field.type.shortly()}` not supported by" +
                " the `($SET_ONCE)` option."
    )
}
