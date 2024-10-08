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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import io.spine.logging.WithLogging
import io.spine.protodata.java.ClassName
import io.spine.protodata.java.render.JavaRenderer
import io.spine.protodata.java.file.hasJavaRoot
import io.spine.protodata.java.javaClassName
import io.spine.protodata.render.SourceFile
import io.spine.protodata.render.SourceFileSet
import io.spine.tools.code.Java
import io.spine.tools.java.reference
import io.spine.tools.kotlin.reference
import io.spine.tools.psi.java.Environment.elementFactory
import io.spine.tools.psi.java.createClassReference
import io.spine.tools.psi.java.execute
import io.spine.tools.psi.java.implement
import io.spine.tools.psi.java.locate
import io.spine.validate.ValidatingBuilder

/**
 * Makes a message builder class implement [ValidatingBuilder] interface.
 */
internal class ImplementValidatingBuilder : JavaRenderer(), WithLogging {

    override fun render(sources: SourceFileSet) {
        if (!sources.hasJavaRoot) {
            return
        }
        val types = findMessageTypes()
        types.forEach {
            val type = it.message
            val messageClass = type.javaClassName(typeSystem!!)
            val file = sources.javaFileOf(type)
            execute {
                file.extendBuilderClass(messageClass, ::logError)
            }
        }
    }

    private fun logError(e: Throwable) {
        logger.atError().withCause(e).log {
            """
            Caught exception in `${ImplementValidatingBuilder::class.reference}`.
            Message: ${e.message}.                                       
            """.trimIndent()
        }
    }
}

/**
 * Locates a builder of the given [messageClass] and makes it implement [ValidatingBuilder].
 *
 * @param messageClass the name of the message class.
 * @param onError the callback to invoke when an error occurs.
 */
@Suppress("TooGenericExceptionCaught") // ... to log diagnostic.
private fun SourceFile<Java>.extendBuilderClass(
    messageClass: ClassName,
    onError: (Throwable) -> Unit
) {
    val psiFile = psi() as PsiJavaFile
    val builderClass = psiFile.locateBuilderClass(messageClass)
    try {
        builderClass.implementValidatingBuilder(messageClass)
        val updatedCode = psiFile.text
        overwrite(updatedCode)
    } catch (e: Throwable) {
        onError(e)
        throw e
    }
}

/**
 * Locates a builder class for the given [messageClass] in this file.
 */
private fun PsiJavaFile.locateBuilderClass(messageClass: ClassName): PsiClass {
    val className = messageClass.nested("Builder")
    val cls = locate(className.simpleNames)
    check(cls != null) {
        "Unable to locate the class `$className` in the file `$this`."
    }
    return cls
}

/**
 * Makes this class implement [ValidatingBuilder] if it does not already do so.
 */
private fun PsiClass.implementValidatingBuilder(messageClass: ClassName) {
    val superInterface = elementFactory.createClassReference(
        ValidatingBuilder::class.java.reference,
        messageClass.simpleName,
        context = this,
    )
    implement(superInterface)
}
