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
import io.spine.protobuf.AnyPacker
import io.spine.protodata.ast.TypeName
import io.spine.protodata.ast.event.FieldEntered
import io.spine.protodata.ast.event.TypeExited
import io.spine.protodata.ast.event.TypeOptionDiscovered
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.server.entity.alter
import io.spine.server.entity.state
import io.spine.server.route.EventRouting
import io.spine.validation.OPTION_NAME
import io.spine.validation.test.money.Currency
import io.spine.validation.test.money.CurrencyType

/**
 * A view on a message type which stores an amount of money is a certain currency.
 */
public class CurrencyTypeView : View<TypeName, CurrencyType, CurrencyType.Builder>() {

    // TODO:2025-03-11:yevhenii.nadtochii: This transaction does not commit for some reason.
    //  Very odd behavior, depends on whether `ValidationCodeInjector` overrides `validate()`.

    @Subscribe
    internal fun on(
        @External @Where(field = OPTION_NAME, equals = "currency")
        event: TypeOptionDiscovered
    ) {
        val currency = AnyPacker.unpack(event.option.value, Currency::class.java)
        println("CurrencyTypeView.TypeOptionDiscovered state `${state().type}`.")
        println("CurrencyTypeView.TypeOptionDiscovered builder `${builder().type}`.")
        println("CurrencyTypeView.TypeOptionDiscovered event `${event.type}`.")
        println("CurrencyTypeView.TypeOptionDiscovered currency `${currency}`.")
        println()
        alter {
            this.currency = currency
        }
    }

    @Subscribe
    internal fun on(@External event: FieldEntered) {
        println("CurrencyTypeView.FieldEntered state `${state().type}`.")
        println("CurrencyTypeView.FieldEntered builder `${builder().type}`.")
        println("CurrencyTypeView.FieldEntered event `${event.type}`.")
        println("CurrencyTypeView.FieldEntered currency `${state.currency}`.")
        println()
        val field = event.field
        alter {
            when (field.orderOfDeclaration) {
                0 -> majorUnitField = field
                1 -> minorUnitField = field
            }
        }
    }

    @Subscribe
    internal fun on(@External event: TypeExited) {
        println("CurrencyTypeView.TypeExited state `${state().type}`.")
        println("CurrencyTypeView.TypeExited builder `${builder().type}`.")
        println("CurrencyTypeView.TypeExited event `${event.type}`.")
        println("CurrencyTypeView.TypeExited currency `${state.currency}`.")
        if (!builder().hasCurrency()) {
            println("CurrencyTypeView.TypeExited deleting `${state().type}`.")
            deleted = true
        }
        println()
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
