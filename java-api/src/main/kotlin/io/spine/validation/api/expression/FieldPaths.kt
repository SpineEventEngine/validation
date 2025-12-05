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

package io.spine.validation.api.expression

import com.google.protobuf.Message
import io.spine.base.FieldPath
import io.spine.tools.compiler.ast.FieldName
import io.spine.tools.compiler.ast.OneofName
import io.spine.tools.compiler.jvm.Expression
import io.spine.tools.compiler.jvm.StringLiteral
import io.spine.tools.compiler.jvm.call
import io.spine.tools.compiler.jvm.toBuilder

/**
 * Returns an expression that yields a new instance of [FieldPath] by appending
 * the provided [field] name to this parental [FieldPath] expression.
 */
public fun Expression<FieldPath>.resolve(field: FieldName): Expression<FieldPath> =
    toBuilder()
        .chainAdd("field_name", StringLiteral(field.value))
        .chainBuild()

/**
 * Returns an expression that merges the provided [FieldPath] expression into this one.
 *
 * To perform merging, this method uses the [Message.Builder.mergeFrom] method.
 */
public fun Expression<FieldPath>.mergeFrom(other: Expression<FieldPath>): Expression<FieldPath> =
    toBuilder()
        .chain<Message.Builder>("mergeFrom", other)
        .chainBuild()

/**
 * Returns an expression that yields a new instance of [FieldPath] by appending
 * the provided [oneof] group name to this parental [FieldPath] expression.
 *
 * Strictly speaking, [OneofName] does not represent a field, but a group of fields.
 * But we still use [FieldPath] to provide a path to this message member because
 * [io.spine.validate.ConstraintViolation] expects exactly [FieldPath] type.
 */
public fun Expression<FieldPath>.resolve(oneof: OneofName): Expression<FieldPath> =
    toBuilder()
        .chainAdd("field_name", StringLiteral(oneof.value))
        .chainBuild()
