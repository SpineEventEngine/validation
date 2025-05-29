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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import io.spine.string.qualified
import io.spine.string.qualifiedClassName
import io.spine.string.simply
import io.spine.validation.api.DiscoveredValidators
import io.spine.validation.api.MessageValidator
import io.spine.validation.api.Validator

/**
 * Discovers classes annotated with the [@Validator][Validator] annotation.
 *
 * The discovered validator and their validated message classes are written
 * to the [DiscoveredValidators.RESOURCES_LOCATION] file.
 */
internal class ValidatorProcessor(codeGenerator: CodeGenerator) : SymbolProcessor {

    /**
     * Already discovered validators.
     * 
     * The map contains a mapping of the message class to the validator.
     *
     * The same validator can be discovered several times because
     * KSP may have several rounds of the code analysis.
     * 
     * This map is used to prevent outputting the same validator twice
     * and to check is the same message has more than one validator.
     */
    private val alreadyDiscovered = mutableMapOf<KSClassDeclaration, KSClassDeclaration>()

    /**
     * The output file with the discovered validators.
     *
     * Each line represents a single mapping: `${MESSAGE_CLASS}:${VALIDATOR_CLASS}`.
     */
    private val output = codeGenerator.createNewFileByPath(
        dependencies = Dependencies(aggregating = true),
        path = DiscoveredValidators.RESOURCES_LOCATION,
        extensionName = ""
    ).writer()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val messageValidatorInterface = resolver
            .getClassDeclarationByName<MessageValidator<*>>()!!
            .asStarProjectedType()
        val annotatedValidators = resolver
            .getSymbolsWithAnnotation(ValidatorAnnotation.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>() // Matches the declared annotation target.
            .onEach { it.checkApplicability(messageValidatorInterface) }
        val newlyDiscovered = annotatedValidators
            .map { validator ->
                val message = validator.validatedMessage(messageValidatorInterface)
                message to validator
            }
            .filterNot { (message, validator) ->
                when (val previous = alreadyDiscovered[message]) {
                    validator -> true // Prevents the same validator being discovered twice.
                    null -> {
                        alreadyDiscovered[message] = validator
                        false
                    }
                    else -> message.reportDuplicateValidator(validator, previous)
                }
            }

        output.use { writer ->
            newlyDiscovered.forEach { (message, validator) ->
                val validatorFQN = validator.qualifiedName?.asString()!!
                val messageFQN = message.qualifiedName?.asString()!!
                writer.appendLine("$messageFQN:$validatorFQN")
            }
        }

        return emptyList()
    }
}

/**
 * Checks if the [Validator] annotation can be used with this [KSClassDeclaration].
 *
 * The method ensures the following:
 *
 * 1. This class is not `inner`.
 * 2. It implements [MessageValidator] interface.
 * 3. It has a public, no-args constructor.
 */
private fun KSClassDeclaration.checkApplicability(messageValidator: KSType) {
    check(!modifiers.contains(Modifier.INNER)) {
        """
        The `${qualifiedName?.asString()}` class cannot be marked with the `@${simply<Validator>()}` annotation.
        This annotation is not applicable to the `inner` classes.
        Please consider making the class nested or top-level.
        """.trimIndent()
    }
    check(messageValidator.isAssignableFrom(asStarProjectedType())) {
        """
        The `${qualifiedName?.asString()}` class cannot be marked with the `@${simply<Validator>()}` annotation.
        This annotation requires the target class to implement the `${qualified<MessageValidator<*>>()}` interface.
        """.trimIndent()
    }
    check(hasPublicNoArgConstructor()) {
        """
        The `${qualifiedName?.asString()}` class cannot be marked with the `@${simply<Validator>()}` annotation.
        This annotation requires the target class to have a public, no-args constructor.
        """.trimIndent()
    }
}

/**
 * Returns `true` if this class has a public, no-args constructor.
 */
private fun KSClassDeclaration.hasPublicNoArgConstructor(): Boolean =
    getConstructors()
        .any { it.isPublic() && it.parameters.isEmpty() }

/**
 * Returns a class of the validated message of this validator [KSClassDeclaration].
 */
private fun KSClassDeclaration.validatedMessage(messageValidator: KSType): KSClassDeclaration {

    val annotation = annotations.first { it.shortName.asString() == ValidatorAnnotation.simpleName }
    val annotationArg = annotation.arguments
        .first { it.name?.asString() == VALIDATOR_ARGUMENT_NAME }
        .value

    // The argument value can be a `KSType` or a `KSTypeReference`.
    // The latter must be resolved.
    val annotationMessage = when (annotationArg) {
        is KSType -> annotationArg
        is KSTypeReference -> annotationArg.resolve()
        else -> error(
            """
            `${simply<ValidatorProcessor>()}` cannot parse the argument parameter of the `@${annotation.shortName.asString()}` annotation.
            Unexpected KSP type of the argument value: `${annotationArg?.qualifiedClassName}`.
            The argument value: `$annotationArg`.
            """.trimIndent()
        )
    }

    val interfaceMessage = interfaceMessage(messageValidator.declaration as KSClassDeclaration)!!
    check(annotationMessage == interfaceMessage) {
        """
        The `@${annotation.shortName.asString()}` annotation is applied to incompatible `${qualifiedName?.asString()}` validator.
        The validated message type of the annotation and the validator must match.
        The message type specified for the annotation: `${annotationMessage.declaration.qualifiedName?.asString()}`.
        The message type specified for the validator: `${interfaceMessage.declaration.qualifiedName?.asString()}`.
        """.trimIndent()
    }

    return annotationMessage.declaration as KSClassDeclaration
}

/**
 * Walks the inheritance tree of this [KSClassDeclaration] and, if it implements
 * the generic interface [messageValidator], returns its single type‐argument.
 */
private fun KSClassDeclaration.interfaceMessage(
    messageValidator: KSClassDeclaration,
    visited: MutableSet<KSClassDeclaration> = mutableSetOf()
): KSType? {

    // Prevents cycles.
    if (!visited.add(this)) {
        return null
    }

    for (superRef: KSTypeReference in superTypes) {
        val superType = superRef.resolve()
        val superDecl = superType.declaration as? KSClassDeclaration ?: continue

        if (superDecl.qualifiedName?.asString() == messageValidator.qualifiedName?.asString()) {
            return superType.arguments.first().type?.resolve()
        }

        superDecl.interfaceMessage(messageValidator, visited)
            ?.let { return it }
    }

    return null
}

private fun KSClassDeclaration.reportDuplicateValidator(
    newValidator: KSClassDeclaration,
    oldValidator: KSClassDeclaration
): Nothing = error("""
    Cannot register the `${newValidator.qualifiedName?.asString()}` validator.
    The message type `${qualifiedName?.asString()}` is already validated by the `${oldValidator.qualifiedName?.asString()}` validator.
    Only one validator is allowed per message type.
""".trimIndent())

/**
 * The name of the [Validator.value] property.
 */
private const val VALIDATOR_ARGUMENT_NAME = "value"

/**
 * The class of the [Validator] annotation.
 */
private val ValidatorAnnotation = Validator::class
