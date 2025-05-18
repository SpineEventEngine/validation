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
package io.spine.validate.option

import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.errorprone.annotations.Immutable
import io.spine.annotation.Internal

/**
 * A factory of standard validating options for non-primitive types.
 */
@AutoService(ValidatingOptionFactory::class)
@Internal
@Immutable
public class NonPrimitiveOptionFactory : StandardOptionFactory {

    override fun forString(): Set<FieldValidatingOption<*>> {
        return Sets.union<FieldValidatingOption<*>>(stringOptions, collectionOptions)
    }

    override fun forByteString(): Set<FieldValidatingOption<*>> {
        return collectionOptions
    }

    override fun forEnum(): Set<FieldValidatingOption<*>> {
        return collectionOptions
    }

    override fun forMessage(): Set<FieldValidatingOption<*>> {
        return Sets.union<FieldValidatingOption<*>>(messageOptions, collectionOptions)
    }

    public companion object {

        private val stringOptions by lazy {
            ImmutableSet.of<FieldValidatingOption<*>>(Pattern.create())
        }

        private val collectionOptions by lazy {
            ImmutableSet.of<FieldValidatingOption<*>>(
                Required.create(false),
                Goes.create(),
                Distinct.create()
            )
        }

        private val messageOptions by lazy {
            ImmutableSet.of<FieldValidatingOption<*>>(Valid())
        }
    }
}
