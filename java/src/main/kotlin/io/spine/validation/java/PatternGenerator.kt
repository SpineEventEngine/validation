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

package io.spine.validation.java

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import io.spine.option.PatternOption
import io.spine.protodata.java.Expression
import io.spine.protodata.java.Literal
import io.spine.protodata.java.MethodCall
import io.spine.protodata.java.ReadVar
import io.spine.validation.Regex
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.lang.model.element.Modifier

/**
 * Generates code for the [Regex] operator.
 */
internal class PatternGenerator(
    private val feature: Regex,
    ctx: GenerationContext
) : SimpleRuleGenerator(ctx) {

    private val fieldName = ctx.simpleRuleField.name
    private val patternConstant = ReadVar<Pattern>("${fieldName.value}_PATTERN")

    override fun condition(): Expression<Boolean> {
        val matcher = MethodCall<Matcher>(patternConstant, "matcher", ctx.fieldOrElement!!)
        val matchingMethod = if (feature.modifier.partialMatch) {
            "find"
        } else {
            "matches"
        }
        return matcher.chain(matchingMethod)
    }

    override fun supportingMembers(): CodeBlock {
        val compileModifiers = feature.hasModifier() && feature.modifier.containsFlags()
        val field = FieldSpec.builder(
            Pattern::class.java,
            "$patternConstant",
            Modifier.PRIVATE,
            Modifier.STATIC,
            Modifier.FINAL
        )
        if (compileModifiers) {
            field.initializer(
                "\$T.compile(\$S, \$L)",
                Pattern::class.java,
                feature.pattern,
                feature.modifier.flagsMask()
            )
        } else {
            field.initializer("\$T.compile(\$S)", Pattern::class.java, feature.pattern)
        }
        return super.supportingMembers()
            .toBuilder()
            .add("\$L", field.build())
            .build()
    }
}

/**
 * Checks if this pattern modifier contains flags matching to those in `java.util.regex.Pattern`.
 */
private fun PatternOption.Modifier.containsFlags() =
    dotAll || caseInsensitive || multiline || unicode

/**
 * Converts this modifier into a bitwise mask built from `java.util.regex.Pattern` constants.
 */
private fun PatternOption.Modifier.flagsMask(): Expression<Int> {
    var mask = 0
    if (dotAll) mask = mask or Pattern.DOTALL
    if (caseInsensitive) mask = mask or Pattern.CASE_INSENSITIVE
    if (multiline) mask = mask or Pattern.MULTILINE
    if (unicode) mask = mask or Pattern.UNICODE_CASE
    return Literal(mask)
}
