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

package io.spine.validation.test

import io.spine.core.External
import io.spine.protodata.Field
import io.spine.protodata.TypeExited
import io.spine.protodata.plugin.Policy
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.query.select
import io.spine.server.tuple.EitherOf2
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.SimpleRule
import io.spine.validation.Value
import io.spine.validation.event.SimpleRuleAdded
import io.spine.validation.test.money.CurrencyType

/**
 * A policy which, if a type is a currency type, produces an event with a validation rule.
 *
 * We do not have enough data on `TypeOptionDiscovered`, thus we collect info in the `CurrencyType`
 * view and only then decide if we want to emit the validation event.
 */
public class CurrencyValidationPolicy : Policy<TypeExited>() {

    @React
    override fun whenever(@External event: TypeExited): EitherOf2<SimpleRuleAdded, Nothing> {
        val currencyType = select<CurrencyType>().withId(event.type)
        if (!currencyType.isPresent || !currencyType.get().hasCurrency()) {
            return EitherOf2.withB(nothing())
        }
        val currency = currencyType.get()
        val minorUnits = currency.minorUnitField
        val otherValue = minorUnitsPerUnit(currency)
        val rule = constructRule(currency.majorUnitField, minorUnits, otherValue)
        return EitherOf2.withA(
            SimpleRuleAdded
                .newBuilder()
                .setType(event.type)
                .setRule(rule)
                .build()
        )
    }

    private fun minorUnitsPerUnit(currencyType: CurrencyType): Value {
        return Value
            .newBuilder()
            .setIntValue(currencyType.currency.minorUnits.toLong())
            .build()
    }

    private fun constructRule(majorUnits: Field, minorUnits: Field, otherValue: Value): SimpleRule {
        val msg = "Expected less than {other} ${minorUnits.prettyName()} per one " +
                "${majorUnits.prettyName()}, but got {value}."
        return SimpleRule
            .newBuilder()
            .setErrorMessage(msg)
            .setField(minorUnits.name)
            .setOperator(LESS_THAN)
            .setOtherValue(otherValue)
            .build()
    }
}

private fun Field.prettyName() = name.value.replaceFirstChar { it.uppercase() }
