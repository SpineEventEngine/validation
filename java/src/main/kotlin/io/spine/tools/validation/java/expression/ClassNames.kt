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

package io.spine.tools.validation.java.expression

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.LinkedHashMultiset
import com.google.common.collect.Multiset
import com.google.protobuf.Any
import com.google.protobuf.Message
import com.google.protobuf.util.Timestamps
import io.spine.base.FieldPath
import io.spine.base.Time
import io.spine.protobuf.AnyPacker
import io.spine.tools.compiler.jvm.ClassName
import io.spine.type.KnownTypes
import io.spine.type.TypeName
import io.spine.type.TypeUrl
import io.spine.validation.ConstraintViolation
import io.spine.validation.TemplateString
import io.spine.validation.ValidatableMessage
import io.spine.validation.ValidationError
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

/**
 * The [ClassName] of [String].
 */
public val StringClass: ClassName = ClassName(String::class)

/**
 * The [ClassName] of [Collectors].
 */
public val CollectorsClass: ClassName = ClassName(Collectors::class)

/**
 * The [ClassName] of [TemplateString].
 */
public val TemplateStringClass: ClassName = ClassName(TemplateString::class)

/**
 * The [ClassName] of [Pattern].
 */
public val PatternClass: ClassName = ClassName(Pattern::class)

/**
 * The [ClassName] of [Map].
 */
public val MapClass: ClassName = ClassName(Map::class)

/**
 * The [ClassName] of [LinkedHashMap].
 */
public val LinkedHashMapClass: ClassName = ClassName(LinkedHashMap::class)

/**
 * The [ClassName] of [ImmutableList].
 */
public val ImmutableListClass: ClassName = ClassName(ImmutableList::class)

/**
 * The [ClassName] of [ImmutableSet].
 */
public val ImmutableSetClass: ClassName = ClassName(ImmutableSet::class)

/**
 * The [ClassName] of [LinkedHashMultiset].
 */
public val LinkedHashMultisetClass: ClassName = ClassName(LinkedHashMultiset::class)

/**
 * The [ClassName] of [Multiset.Entry] class.
 */
public val MultiSetEntryClass: ClassName = ClassName(Multiset.Entry::class)

/**
 * The [ClassName] of [FieldPath].
 */
public val FieldPathClass: ClassName = ClassName(FieldPath::class)

/**
 * The [ClassName] of [ConstraintViolation].
 */
public val ConstraintViolationClass: ClassName = ClassName(ConstraintViolation::class)

/**
 * The [ClassName] of `io.spine.base.FieldPaths`.
 *
 * Note: `FieldPaths` is a synthetic Java class, which contains Kotlin extensions
 * declared for [FieldPath]. It is available from Java, but not from Kotlin.
 * So, we specify it as a string literal here.
 */
public val FieldPathsClass: ClassName = ClassName("io.spine.base", "FieldPaths")

/**
 * The [ClassName] of [ValidationError].
 */
public val ValidationErrorClass: ClassName = ClassName(ValidationError::class)

/**
 * The [ClassName] of [ValidatableMessage].
 */
public val ValidatableMessageClass: ClassName = ClassName(ValidatableMessage::class)

/**
 * The [ClassName] of [AnyPacker].
 */
public val AnyPackerClass: ClassName = ClassName(AnyPacker::class)

/**
 * The [ClassName] of [Message].
 */
public val MessageClass: ClassName = ClassName(Message::class)

/**
 * The [ClassName] of Protobuf [Any].
 */
public val AnyClass: ClassName = ClassName(Any::class)

/**
 * The [ClassName] of [KnownTypes].
 */
public val KnownTypesClass: ClassName = ClassName(KnownTypes::class)

/**
 * The [ClassName] of [TypeUrl].
 */
public val TypeUrlClass: ClassName = ClassName(TypeUrl::class)

/**
 * The [ClassName] of [TypeName] from `java-base`.
 */
public val TypeNameClass: ClassName = ClassName(TypeName::class)

/**
 * The [ClassName] of [java.lang.Integer].
 */
public val IntegerClass: ClassName = ClassName(Integer::class)

/**
 * The [ClassName] of [java.lang.Long].
 */
public val LongClass: ClassName = ClassName(java.lang.Long::class)

/**
 * The [ClassName] of [java.util.Objects].
 */
public val ObjectsClass: ClassName = ClassName(Objects::class)

/**
 * The [ClassName] of [Timestamps].
 */
public val TimestampsClass: ClassName = ClassName(Timestamps::class)

/**
 * The [ClassName] of [io.spine.base.Time].
 */
public val SpineTime: ClassName = ClassName(Time::class)

/**
 * The [ClassName] for `io.spine.type.Json`.
 *
 * Note: `Json` is a synthetic Java class, which contains Kotlin extensions.
 * It is available from Java, but not from Kotlin. So, we specify it as
 * a string literal here.
 */
public val JsonExtensionsClass: ClassName = ClassName("io.spine.type", "Json")
