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

package io.spine.validation;

import com.google.common.collect.ImmutableSet;
import io.spine.protodata.plugin.Plugin;
import io.spine.protodata.plugin.Policy;
import io.spine.protodata.plugin.ViewRepository;
import org.jetbrains.annotations.NotNull;

/**
 * A ProtoData plugin which attaches validation-related policies and views.
 */
@SuppressWarnings({
        "OverlyCoupledMethod", // Registers a lot of policies and repositories.
        "OverlyCoupledClass", // Registers a lot of policies and repositories.
        "unused" // Loaded by ProtoData via reflection.
})
public class ValidationPlugin implements Plugin {

    @Override
    public ImmutableSet<Policy<?>> policies() {
        return ImmutableSet.of(
                new RequiredPolicy(),
                new RangePolicy(),
                new MinPolicy(),
                new MaxPolicy(),
                new DistinctPolicy(),
                new ValidatePolicy(),
                new PatternPolicy(),
                new IsRequiredPolicy(),
                new WhenPolicy(),
                new RequiredIdPatternPolicy(),
                new RequiredIdOptionPolicy()
        );
    }

    @Override
    public @NotNull ImmutableSet<ViewRepository<?, ?, ?>> viewRepositories() {
        return ImmutableSet.of(
                new MessageValidationRepository(),
                new RequiredFieldRepository(),
                new ValidatedFieldRepository(),
                new SetOnceFieldRepository()
        );
    }
}
