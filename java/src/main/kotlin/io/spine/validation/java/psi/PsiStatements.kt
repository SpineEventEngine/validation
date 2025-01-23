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

import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory

/**
 * List of statements from [PsiCodeBlock] without surrounding braces,
 * but with the preserved formatting elements.
 *
 * This type resolves two problems:
 *
 * 1. In PSI, the code block cannot be created without curly braces, making it
 *    difficult to insert a code block into a method body.
 * 2. A list of statements can be [retrieved][PsiCodeBlock.getStatements] from the block.
 *    Though, formatting-related elements like whitespaces and new lines are filtered out.
 *    An attempt to add these statements one-by-one to a method block often leads to
 *    non-compilable Java code.
 */
public class PsiStatements(codeBlock: PsiCodeBlock) {

    /**
     * All children of [PsiCodeBlock] without right and left braces.
     */
    private val children = codeBlock.children
        .copyOfRange(1, codeBlock.children.size - 1)

    /**
     * Returns the first child of this element.
     */
    public val firstChild: PsiElement = children.first()

    /**
     * Returns the last child of this element.
     */
    public val lastChild: PsiElement = children.last()
}

/**
 * Creates a new [PsiStatements] from the given [text].
 *
 * @param text The text of the statements to create.
 * @param context The PSI element used as context for resolving references from the statements.
 */
public fun PsiElementFactory.createStatementsFromText(
    text: String,
    context: PsiElement?
): PsiStatements {
    val codeBlock = createCodeBlockFromText("{$text}", context)
    return PsiStatements(codeBlock)
}

/**
 * Adds the given [statements] to this [PsiElement].
 */
public fun PsiElement.add(statements: PsiStatements) {
    addRange(statements.firstChild, statements.lastChild)
}

/**
 * Adds the given [statements] to this [PsiElement] after the [anchor].
 */
public fun PsiElement.addAfter(statements: PsiStatements, anchor: PsiElement) {
    addRangeAfter(statements.firstChild, statements.lastChild, anchor)
}

/**
 * Adds the given [statements] to this [PsiElement] before the [anchor].
 */
public fun PsiElement.addBefore(statements: PsiStatements, anchor: PsiElement) {
    addRangeBefore(statements.firstChild, statements.lastChild, anchor)
}
