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

package io.spine.validation.java.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import io.spine.validation.api.MessageValidatorsDescriptor
import io.spine.validation.api.Validator

internal class ValidatorProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private val discoveredValidators = mutableSetOf<String>()
    private val output by lazy {
        codeGenerator.createNewFileByPath(
            dependencies = Dependencies(aggregating = true),
            path = MessageValidatorsDescriptor.RESOURCES_LOCATION,
            extensionName = ""
        ).writer()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val validators = resolver.getSymbolsWithAnnotation(validator.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>().associateBy { kclass ->
                val annotation = kclass.annotations.find {
                    it.shortName.getShortName() == validator.simpleName
                }!!
                annotation.argumentValue()
            }

        if (validators.isEmpty()) {
            return emptyList()
        }

        output.use { writer ->
            validators.forEach { (message, validator) ->
                val validatorFQN = validator.qualifiedName?.asString()
                    ?: noQualifiedName(validator)
                val messageFQN = message.qualifiedName?.asString()
                    ?: noQualifiedName(validator)
                if (discoveredValidators.add(validatorFQN)) {
                    writer.appendLine("$messageFQN:$validatorFQN")
                }
            }
        }

        // Return an empty list: no deferred symbols
        return emptyList()
    }

    private fun noQualifiedName(validator: KSClassDeclaration): Nothing = error(
        "Validator `$validator` has no qualified name."
    )

    private fun KSAnnotation.argumentValue(argumentName: String = "value"): KSClassDeclaration {
        val valueArg = arguments.firstOrNull { it.name?.asString() == argumentName }
            ?: error("Annotation `@$shortName` has no argument named `$argumentName`.")

        // the raw .value can be a KSType or a KSTypeReference
        val kType: KSType = when (val raw = valueArg.value) {
            is KSType -> raw
            is KSTypeReference -> raw.resolve()
            else -> error("Unsupported annotation parameter type: `${raw?.javaClass}`.")
        }

        // its declaration is a KSClassDeclaration
        val declaration = kType.declaration as? KSClassDeclaration
            ?: error("Expected a class declaration, but got `$kType`.")

        return declaration
    }

    private companion object {
        val validator = Validator::class
    }
}
