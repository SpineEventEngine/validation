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

package io.spine.validate

import com.google.common.collect.Range
import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import io.spine.base.Field.named
import io.spine.code.proto.FieldContext
import io.spine.code.proto.FieldDeclaration
import io.spine.code.proto.FieldName
import io.spine.protobuf.TypeConverter.toAny
import io.spine.protobuf.ensureUnpacked
import io.spine.type.MessageType
import io.spine.validate.MessageValue.atTopLevel
import io.spine.validate.MessageValue.nestedIn
import io.spine.validate.option.DistinctConstraint
import io.spine.validate.option.GoesConstraint
import io.spine.validate.option.IsRequiredConstraint
import io.spine.validate.option.PatternConstraint
import io.spine.validate.option.RangedConstraint
import io.spine.validate.option.RequiredConstraint
import io.spine.validate.option.RequiredFieldConstraint
import io.spine.validate.option.ValidateConstraint
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors.toList
import kotlin.streams.toList

/**
 * Validates a given message according to the constraints.
 *
 * The output result of this [ConstraintTranslator] is a [ValidationError].
 *
 * The class uses [Optional] to keep its compatibility with the remaining Java code.
 */
@Suppress("TooManyFunctions") // Covers almost all runtime validation.
internal class MessageValidator private constructor(private val validatedMessage: MessageValue) :
    ConstraintTranslator<Optional<ValidationError>> {

    private val violations = mutableListOf<ConstraintViolation>()

    /**
     * Creates a new validator for the [top-level][MessageValue.atTopLevel] [message].
     */
    constructor(message: Message) : this(atTopLevel(message))

    /**
     * Creates a new validator for the [message] with the specific field [context].
     */
    constructor(message: Message, context: FieldContext) : this(nestedIn(context, message))

    override fun visitRange(constraint: RangedConstraint<*>) {
        val value = validatedMessage.valueOf(constraint.field())
        val range = constraint.range()
        checkTypeConsistency(range, value)
        value.values()
            .map { any -> Number::class.java.cast(any) }
            .map { number -> ComparableNumber(number) }
            .filter(range.negate())
            .map { comparableNumber -> violation(constraint, value, comparableNumber.value()) }
            .forEach { e: ConstraintViolation -> violations.add(e) }
    }

    override fun visitRequired(constraint: RequiredConstraint) {
        if (constraint.optionValue()) {
            val fieldValue = validatedMessage.valueOf(constraint.field())
            if (fieldValue.isDefault) {
                violations.add(violation(constraint, fieldValue))
            }
        }
    }

    override fun visitPattern(constraint: PatternConstraint) {
        val fieldValue = validatedMessage.valueOf(constraint.field())
        val regex = constraint.regex()
        val flags = constraint.flagsMask()
        val compiledPattern = Pattern.compile(regex, flags)
        val partialMatch = constraint.allowsPartialMatch()
        fieldValue.nonDefault()
            .filter { value: Any ->
                val matcher = compiledPattern.matcher(value as String)
                if (partialMatch) {
                    !matcher.find()
                } else {
                    !matcher.matches()
                }
            }
            .map { value: Any ->
                val violation = violation(constraint, fieldValue, value)
                val withRegex = violation.message.toBuilder()
                    .withRegex(regex)
                    .build()
                violation.copy {
                    message = withRegex
                }
            }
            .forEach { e: ConstraintViolation -> violations.add(e) }
    }

    override fun visitDistinct(constraint: DistinctConstraint) {
        val fieldValue = validatedMessage.valueOf(constraint.field())
        val duplicates = findDuplicates(fieldValue)
        val distinctViolations = duplicates.map { duplicate ->
            violation(constraint, fieldValue, duplicate)
        }
        violations.addAll(distinctViolations)
    }

    override fun visitGoesWith(constraint: GoesConstraint) {
        val field = constraint.field()
        val value = validatedMessage.valueOf(field)
        val declaration = withField(validatedMessage, constraint)
        val withFieldName = constraint.optionValue().with
        check(declaration.isPresent) {
            "The field `$withFieldName` specified in the `(goes).with` option is not found."
        }
        val withField = declaration.get()
        if (!value.isDefault && fieldValueNotSet(withField)) {
            val violation = violation(constraint, value)
            val template = violation.message.toBuilder()
                .withField(field)
                .withCompanion(withField)
            violations.add(
                violation.copy {
                    message = template.build()
                }
            )
        }
    }

    override fun visitValidate(constraint: ValidateConstraint) {
        val fieldValue = validatedMessage.valueOf(constraint.field())
        if (!fieldValue.isDefault) {
            val childViolations = fieldValue.values()
                .map { any -> (any as Message).ensureUnpacked() }
                .map { msg -> childViolations(fieldValue.context(), msg) }
                .flatMap { violations -> violations.stream() }
                .collect(toList())
            violations.addAll(childViolations)
        }
    }

    override fun visitRequiredField(constraint: RequiredFieldConstraint) {
        val check = RequiredFieldCheck(
            constraint.optionValue(),
            constraint.alternatives(),
            validatedMessage
        )
        val violation = check.perform()
        violation.ifPresent { e -> violations.add(e) }
    }

    override fun visitRequiredOneof(constraint: IsRequiredConstraint) {
        val fieldValue = validatedMessage.valueOf(constraint.declaration())
        val noneSet = fieldValue.isEmpty
        if (noneSet) {
            val oneofName = constraint.oneofName()
            val oneofField = named(oneofName.value())
            val targetType = constraint.targetType()
            violations.add(
                constraintViolation {
                    message = constraint.errorMessage(validatedMessage.context())
                    fieldPath = oneofField.path()
                    typeName = targetType.name().value
                }
            )
        }
    }

    override fun visitCustom(constraint: CustomConstraint) {
        val violations = constraint.validate(validatedMessage)
        this.violations.addAll(violations)
    }

    /**
     * Obtains the resulting [ValidationError] or an [Optional.empty] if
     * the message value is valid.
     */
    override fun translate(): Optional<ValidationError> =
        if (violations.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(
                validationError {
                    constraintViolation += violations
                }
            )
        }

    private fun fieldValueNotSet(field: FieldDeclaration): Boolean =
        validatedMessage
            .valueOf(field.descriptor())
            .map { it.isDefault }
            .orElse(false)

    private companion object {

        fun childViolations(field: FieldContext, message: Message): List<ConstraintViolation> {
            val messageValue = nestedIn(field, message.ensureUnpacked())
            val childInterpreter = MessageValidator(messageValue)
            return Constraints.of(MessageType.of(message), field)
                .runThrough(childInterpreter)
                .map { error: ValidationError -> error.constraintViolationList }
                .orElse(emptyList())
        }
    }
}

private fun findDuplicates(fieldValue: FieldValue): Set<*> =
    fieldValue.values().toList()
        .groupingBy { it }
        .eachCount()
        .filter { (_, count) -> count > 1 }
        .keys

private fun withField(message: MessageValue, goes: GoesConstraint): Optional<FieldDeclaration> {
    val withField = FieldName.of(goes.optionValue().with)
    for (field in message.declaration().fields()) {
        if (withField == field.name()) {
            return Optional.of(field)
        }
    }
    return Optional.empty()
}

private fun violation(constraint: Constraint, value: FieldValue): ConstraintViolation =
    violation(constraint, value, null)

private fun violation(
    constraint: Constraint,
    value: FieldValue,
    violatingValue: Any?
): ConstraintViolation {
    val context = value.context()
    val fieldPath = context.fieldPath()
    val typeName = constraint.targetType().name()
    val violation = ConstraintViolation.newBuilder()
        .setMessage(constraint.errorMessage(context))
        .setFieldPath(fieldPath)
        .setTypeName(typeName.value())
    if (violatingValue != null) {
        violation.setFieldValue(toFieldValue(violatingValue))
    }
    return violation.build()
}

/**
 * Converts the `violatingValue` to a wrapped [Any].
 *
 * If the violation is caused by an enum, unwraps the enum value from the descriptor before
 * doing the conversion.
 */
private fun toFieldValue(violatingValue: Any): com.google.protobuf.Any =
    if (violatingValue is Descriptors.EnumValueDescriptor) {
        toAny(violatingValue.toProto())
    } else {
        toAny(violatingValue)
    }

private fun checkTypeConsistency(range: Range<ComparableNumber>, value: FieldValue) {
    if (range.hasLowerBound() && range.hasUpperBound()) {
        val upper = range.upperEndpoint().toText()
        val lower = range.lowerEndpoint().toText()
        check(upper.isOfSameType(lower)) {
            "Boundaries have inconsistent types: lower is `$lower`, upper is `$upper`."
        }
        checkBoundaryAndValue(upper, value)
    } else {
        checkSingleBoundary(range, value)
    }
}

private fun checkSingleBoundary(range: Range<ComparableNumber>, value: FieldValue) {
    val singleBoundary =
        if (range.hasLowerBound()) {
            range.lowerEndpoint().toText()
        } else {
            range.upperEndpoint().toText()
        }
    checkBoundaryAndValue(singleBoundary, value)
}

private fun checkBoundaryAndValue(boundary: NumberText, value: FieldValue) {
    val boundaryNumber = boundary.toNumber()
    val valueNumber = value.singleValue() as Number
    check(NumberConversion.check(boundaryNumber, valueNumber)) {
        "Boundary values must have types consistent with the values they bind: " +
                "boundary is $boundary`, value is `$valueNumber`."
    }
}
