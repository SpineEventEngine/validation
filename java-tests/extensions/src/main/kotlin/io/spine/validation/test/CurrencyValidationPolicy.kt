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

package io.spine.validation.test

import io.spine.core.External
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.event.TypeExited
import io.spine.protodata.plugin.Policy
import io.spine.protodata.value.Value
import io.spine.protodata.value.value
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.event.asA
import io.spine.server.query.select
import io.spine.server.tuple.EitherOf2
import io.spine.validation.ComparisonOperator.LESS_THAN
import io.spine.validation.SimpleRule
import io.spine.validation.event.SimpleRuleAdded
import io.spine.validation.event.simpleRuleAdded
import io.spine.validation.simpleRule
import io.spine.validation.test.money.CurrencyType

/**
 * A policy which, if a type is a currency type, produces an event with a validation rule.
 *
 * We do not have enough data on `TypeOptionDiscovered`, thus we collect info in the `CurrencyType`
 * view and only then decide if we want to emit the validation event.
 */
public class CurrencyValidationPolicy : Policy<TypeExited>() {

    @React
    override fun whenever(@External event: TypeExited): EitherOf2<SimpleRuleAdded, NoReaction> {
        val currencyType = select<CurrencyType>().findById(event.type)
        if (currencyType == null || currencyType.hasCurrency().not()) {
            return ignore()
        }
        val minorUnits = currencyType.minorUnitField
        val otherValue = minorUnitsPerUnit(currencyType)
        val rule = constructRule(currencyType.majorUnitField, minorUnits, otherValue)
        return simpleRuleAdded {
            type = event.type
            this.rule = rule
        }.asA()
    }

    private fun minorUnitsPerUnit(currencyType: CurrencyType): Value =
        value { intValue = currencyType.currency.minorUnits.toLong() }

    private fun constructRule(majorUnits: Field, minorUnits: Field, otherValue: Value): SimpleRule =
        simpleRule {
            errorMessage = "Expected `${minorUnits.name.value}` field to have less than `{other}`" +
                    " per one unit in `${majorUnits.name.value}` field, but got `{value}`."
            field = minorUnits.name
            operator = LESS_THAN
            this.otherValue = otherValue
        }
}
