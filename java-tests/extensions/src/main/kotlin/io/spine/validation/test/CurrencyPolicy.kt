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
import io.spine.protodata.Compilation
import io.spine.protodata.ast.Field
import io.spine.protodata.ast.File
import io.spine.protodata.ast.MessageType
import io.spine.protodata.ast.PrimitiveType.TYPE_INT32
import io.spine.protodata.ast.PrimitiveType.TYPE_INT64
import io.spine.protodata.ast.event.MessageOptionDiscovered
import io.spine.protodata.ast.unpack
import io.spine.protodata.check
import io.spine.protodata.plugin.Policy
import io.spine.server.event.Just
import io.spine.server.event.React
import io.spine.server.event.just
import io.spine.validation.OPTION_NAME
import io.spine.validation.event.GoesFieldDiscovered
import io.spine.validation.test.money.Currency
import io.spine.validation.test.money.CurrencyMessageDiscovered
import io.spine.validation.test.money.currencyMessageDiscovered

/**
 * The name of the option.
 */
private const val CURRENCY = "currency"

/**
 * Controls whether a message should be validated with the `(currency)` option.
 *
 * Whenever a message marked with the `(currency)` option is discovered, emits
 * [CurrencyMessageDiscovered] event if the message has exactly two integer fields.
 *
 * Otherwise, a compilation error is reported.
 */
public class CurrencyPolicy : Policy<MessageOptionDiscovered>() {

    @React
    override fun whenever(
        @External @Where(field = OPTION_NAME, equals = CURRENCY)
        event: MessageOptionDiscovered
    ): Just<CurrencyMessageDiscovered> {
        val file = event.file
        val messageType = event.subject
        val fields = messageType.fieldList
        checkFieldType(fields.size == 2, file, messageType)

        val firstField = messageType.fieldList[0]
        val secondField = messageType.fieldList[1]
        checkFieldType(firstField.isInteger && secondField.isInteger, file, messageType)

        val option = event.option.unpack<Currency>()
        val message = errorMessage(firstField, secondField)
        return currencyMessageDiscovered {
            type = messageType.name
            currency = option
            majorUnitField = firstField
            minorUnitField = secondField
            errorMessage = message
        }.just()
    }
}

private val Field.isInteger: Boolean
    get() = type.primitive in listOf(TYPE_INT32, TYPE_INT64)

private fun errorMessage(minor: Field, major: Field) =
    "Expected `${minor.name.value}` field to have less than `\$MINOR_VALUE`" +
            " per one unit in `${major.name.value}` field, but got `\$MAJOR_VALUE`."

private fun checkFieldType(condition: Boolean, file: File, message: MessageType) =
    Compilation.check(condition, file, message.span) {
        "The `($CURRENCY)` option cannot be applied to `${message.qualifiedName}`. It is" +
                " applicable only to messages that have exactly two integer fields."
    }
