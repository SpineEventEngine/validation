/*
 * Copyright 2023, TeamDev. All rights reserved.
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

@file:JvmName("Rules")

package io.spine.validation

import io.spine.protodata.ast.TypeName
import io.spine.validation.event.RuleAdded
import io.spine.validation.event.compositeRuleAdded
import io.spine.validation.event.simpleRuleAdded

/**
 * Tells if this `rule` is a `SimpleRule`.
 */
public val Rule.isSimple: Boolean
    get() = hasSimple()

/**
 * Tells if this `rule` is a message-wide one.
 */
public val Rule.isMessageWide: Boolean
    get() = hasMessageWide()

/**
 * Converts this `rule` to an event.
 *
 * @param type the type name of the validated message
 */
internal fun Rule.toEvent(type: TypeName): RuleAdded =
    if (hasComposite()) {
        compositeRuleAdded {
            this.type = type
            rule = composite
        }
    } else {
        simpleRuleAdded {
            this.type = type
            rule = simple
        }
    }

/**
 * Creates a [Rule] from this `SimpleRule`.
 */
internal fun SimpleRule.wrap(): Rule = rule {
    simple = this@wrap
}

/**
 * Creates a [Rule] from this composite rule.
 */
internal fun CompositeRule.wrap(): Rule = rule {
    composite = this@wrap
}

/**
 * Creates a [Rule] from this message-wide rule.
 */
internal fun MessageWideRule.wrap(): Rule = rule {
    messageWide = this@wrap
}
