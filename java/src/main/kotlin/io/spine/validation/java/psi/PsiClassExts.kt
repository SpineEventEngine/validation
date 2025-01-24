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

package io.spine.validation.java.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.spine.tools.psi.java.Environment.elementFactory

/**
 * Returns a nested class declared in this [PsiClass].
 *
 * @param simpleName The class simple name.
 */
public fun PsiClass.nested(simpleName: String): PsiClass {
    val found = innerClasses.firstOrNull { it.name == simpleName }
    check(found != null) {
        "The class `$qualifiedName` does not declare a nested class named `$name`."
    }
    return found
}

/**
 * Looks for a method in this [PsiClass] matching the given signature
 * specified as [text].
 *
 * An example usage:
 *
 * ```
 * val method = psiClass.findMethodBySignature("public Builder setName(Name value)")
 * ```
 *
 * @param text The method signature as text.
 */
public fun PsiClass.findMethodBySignature(text: String): PsiMethod? {
    val reference = elementFactory.createMethodFromText(text, null)
    return findMethodBySignature(reference, false)
}

/**
 * Returns a method from this [PsiClass] matching the given signature
 * specified as [text].
 *
 * An example usage:
 *
 * ```
 * val method = psiClass.getMethodBySignature("public Builder setName(Name value)")
 * ```
 *
 * @param text The method signature as text.
 * @throws IllegalStateException if this class does not have such a method.
 */
public fun PsiClass.methodWithSignature(text: String): PsiMethod =
    findMethodBySignature(text)
        ?: error(
            "Could not find the method with the signature `$text` " +
                    "in the `$qualifiedName` class."
        )
