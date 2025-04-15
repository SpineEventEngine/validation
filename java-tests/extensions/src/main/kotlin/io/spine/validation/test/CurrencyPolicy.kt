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
import io.spine.core.Where
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.event.TypeOptionDiscovered
import io.spine.protodata.ast.unpack
import io.spine.protodata.plugin.Policy
import io.spine.protodata.value.Value
import io.spine.protodata.value.value
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.validation.test.money.Currency
import io.spine.validation.test.money.CurrencyMessageDiscovered

/**
 * A policy which, if a type is a currency type, produces an event with a validation rule.
 *
 * Such a message must have exactly 2 integer fields, one for the major currency and
 * another one for the minor currency.
 */
public class CurrencyPolicy : Policy<TypeOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = "option.name", equals = "currency")
        event: TypeOptionDiscovered
    ): Just<CurrencyMessageDiscovered> {
        val option = event.option.unpack<Currency>()
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
