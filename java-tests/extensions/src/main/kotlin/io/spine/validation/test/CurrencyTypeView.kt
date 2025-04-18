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
import io.spine.core.Subscribe
import io.spine.core.Where
import io.spine.protobuf.unpack
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.FieldEntered
import io.spine.protodata.ast.event.TypeExited
import io.spine.protodata.ast.event.TypeOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.route.EventRouting
import io.spine.validation.OPTION_NAME
import io.spine.validation.test.money.Currency
import io.spine.validation.test.money.CurrencyType

/**
 * A view on a message type which stores an amount of money is a certain currency.
 */
public class CurrencyTypeView : View<TypeName, CurrencyType, CurrencyType.Builder>() {

    @Subscribe
    internal fun on(
        @External @Where(field = OPTION_NAME, equals = "currency")
        event: TypeOptionDiscovered
    ) {
        val option = event.option.value.unpack<Currency>()
        alter {
            currency = option
        }
    }

    @Subscribe
    internal fun on(@External event: FieldEntered) {
        val field = event.field
        alter {
            when (field.orderOfDeclaration) {
                0 -> majorUnitField = field
                1 -> minorUnitField = field
            }
        }
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    internal fun on(@External event: TypeExited) {
        if (!builder().hasCurrency()) {
            deleted = true
        }
    }

    internal class Repo : ViewRepository<TypeName, CurrencyTypeView, CurrencyType>() {

        override fun setupEventRouting(routing: EventRouting<TypeName>) {
            super.setupEventRouting(routing)
            routing.unicast(TypeOptionDiscovered::class.java) { e, _ -> e.type }
            routing.unicast(FieldEntered::class.java) { e, _ -> e.type }
            routing.unicast(TypeExited::class.java) { e, _ -> e.type }
        }
    }
}
