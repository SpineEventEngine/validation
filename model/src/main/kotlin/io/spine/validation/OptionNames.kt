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

@file:JvmName("OptionNames")

package io.spine.validation

/**
 * Path to the name field of the option in
 * the [io.spine.protodata.ast.event.FieldOptionDiscovered] event.
 */
public const val OPTION_NAME: String = "option.name"

/**
 * The name of the `(distinct)` option.
 */
public const val DISTINCT: String = "distinct"

/**
 * The name of the `(is_required)` option.
 */
public const val IS_REQUIRED: String = "is_required"

/**
 * The name of the `(max)` option.
 */
public const val MAX: String = "max"

/**
 * The name of the `(min)` option.
 */
public const val MIN: String = "min"

/**
 * The name of the `(pattern)` option.
 */
public const val PATTERN: String = "pattern"

/**
 * The name of the `(range)` option.
 */
public const val RANGE: String = "range"

/**
 * The name of the `(required)` option.
 */
public const val REQUIRED: String = "required"

/**
 * The name of `(if_missing)` option.
 */
public const val IF_MISSING: String = "if_missing"

/**
 * The name of the `(set_once)` option.
 */
public const val SET_ONCE: String = "set_once"

/**
 * The name of `(if_set_again)` option.
 */
public const val IF_SET_AGAIN: String = "if_set_again"

/**
 * The name of the `(validate)` option.
 */
public const val VALIDATE: String = "validate"

/**
 * The name of the `(if_invalid)` option.
 */
public const val IF_INVALID: String = "if_invalid"

/**
 * The name of the `(when)` option.
 */
public const val WHEN: String = "when"

/**
 * The name of the `(goes)` option.
 */
public const val GOES: String = "goes"

/**
 * The name of `(if_has_duplicates)` option.
 */
public const val IF_HAS_DUPLICATES: String = "if_has_duplicates"

/**
 * The name of `(choice)` option.
 */
public const val CHOICE : String = "choice"
