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

import io.spine.validation.ValidationPlugin
import io.spine.validation.java.setonce.SetOnceRenderer
import java.util.*

/**
 * An implementation of [ValidationPlugin] for Java language.
 *
 * The validation constraints for Java are implemented with two renderers:
 *
 * 1. [JavaValidationRenderer] is the main renderer for Java. It renders the validation
 * code for all options that perform an assertion upon a message field value.
 * 2. [SetOnceRenderer] is responsible for the validation code of `(set_once)` option.
 * It is a standalone renderer because it significantly differs from the rest of constraints.
 * Its implementation modifies the message builder behavior, affecting every setter or merge
 * method that can change the field value.
 */
@Suppress("unused") // Accessed via reflection.
public class JavaValidationPlugin : ValidationPlugin(
    renderers = listOf(
        JavaValidationRenderer(customOptions.map { it.generator }),
        SetOnceRenderer()
    ),
    views = customOptions.map { it.view }.toSet(),
    policies = customOptions.map { it.policy }.toSet(),
)

/**
 * Dynamically discovered instances of [CustomOption].
 */
private val customOptions by lazy {
    ServiceLoader.load(CustomOption::class.java)
        .filterNotNull()
}
