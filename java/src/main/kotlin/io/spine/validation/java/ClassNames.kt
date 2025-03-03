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

@file:JvmName("ClassNames")

package io.spine.validation.java

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import io.spine.base.FieldPath
import io.spine.protodata.java.ClassName
import io.spine.validate.ConstraintViolation
import io.spine.validate.TemplateString
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * The [ClassName] of [String].
 */
internal val StringClass = ClassName(String::class)

/**
 * The [ClassName] of [Collectors].
 */
internal val CollectorsClass = ClassName(Collectors::class)

/**
 * The [ClassName] of [TemplateString].
 */
internal val TemplateStringClass = ClassName(TemplateString::class)

/**
 * The [ClassName] of [Pattern].
 */
internal val PatternClass = ClassName(Pattern::class)

/**
 * The [ClassName] of [ImmutableList].
 */
internal val ImmutableListClass = ClassName(ImmutableList::class)

/**
 * The [ClassName] of [ImmutableSet].
 */
internal val ImmutableSetClass = ClassName(ImmutableSet::class)

/**
 * The [ClassName] of [LinkedHashMultiset].
 */
internal val LinkedHashMultisetClass = ClassName(LinkedHashMultiset::class)

/**
 * The [ClassName] of [Multiset.Entry] class.
 */
internal val MultiSetEntryClass = ClassName(Multiset.Entry::class)

/**
 * The [ClassName] of [FieldPath].
 */
internal val FieldPathClass = ClassName(FieldPath::class)

/**
 * The [ClassName] of [ConstraintViolation].
 */
internal val ConstraintViolationClass = ClassName(ConstraintViolation::class)

/**
 * The [ClassName] of `io.spine.base.FieldPaths`.
 *
 * Note: `FieldPaths` is a synthetic Java class, which contains Kotlin extensions
 * declared for [FieldPath]. It is available from Java, but not from Kotlin.
 * So, we specify it as a string literal here.
 */
internal val FieldPathsClass = ClassName("io.spine.base", "FieldPaths")
