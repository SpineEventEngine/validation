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

package io.spine.tools.validation.java

import io.spine.tools.compiler.jvm.ClassName
import io.spine.tools.validation.java.generate.MessageClass
import io.spine.tools.validation.java.generate.ValidatorClass
import io.spine.tools.validation.java.setonce.SetOnceRenderer
import io.spine.tools.validation.ValidationPlugin
import io.spine.tools.validation.ksp.DiscoveredValidators
import java.io.File
import java.util.*

/**
 * An implementation of [ValidationPlugin] for Java language.
 *
 * The validation constraints for Java are implemented with two renderers:
 *
 * 1. [JavaValidationRenderer][io.spine.tools.validation.java.JavaValidationRenderer]
 *   is the main renderer for Java. It renders the validation
 *   code for all options that perform an assertion upon a message field value.
 * 2. [SetOnceRenderer][io.spine.tools.validation.java.setonce.SetOnceRenderer]
 *   is responsible for the validation code of `(set_once)` option.
 *   It is a standalone renderer because it significantly differs from the rest of constraints.
 *   Its implementation modifies the message builder behavior, affecting every setter or merge
 *   method that can change the field value.
 */
@Suppress("unused") // Accessed via reflection.
public open class JavaValidationPlugin : ValidationPlugin(
    renderers = listOf(
        JavaValidationRenderer(customOptions.map { it.generator }, customValidators),
        SetOnceRenderer()
    ),
    views = customOptions.flatMap { it.view }.toSet(),
    reactions = customOptions.flatMap { it.reactions }.toSet(),
)

/**
 * Dynamically discovered instances of custom [ValidationOption]s.
 */
private val customOptions: List<ValidationOption> by lazy {
    ServiceLoader.load(ValidationOption::class.java)
        .filterNotNull()
}

/**
 * Dynamically discovered instances of custom
 * [MessageValidator][io.spine.validate.MessageValidator]s.
 *
 * Please note that the KSP module is responsible for the actual discovering
 * of the message validators. The discovered validators are written to a text file
 * in the KSP task output. This property loads the validators from that file.
 */
private val customValidators: Map<MessageClass, ValidatorClass> by lazy {
    val workingDir = System.getProperty("user.dir")
    val kspOutput = File("$workingDir/$KSP_GENERATED_RESOURCES")
    val messageValidators =  DiscoveredValidators.resolve(kspOutput)
    if (!messageValidators.exists()) {
        return@lazy emptyMap()
    }

    messageValidators.readLines().associate {
        val (message, validator) = it.split(":")
        ClassName.guess(message) to ClassName.guess(validator)
    }
}

/**
 * The default location to which the KSP task puts the generated output.
 */
private const val KSP_GENERATED_RESOURCES = "build/generated/ksp/main"
