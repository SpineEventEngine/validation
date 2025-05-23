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

package io.spine.validation

import io.spine.protodata.plugin.Plugin
import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.protodata.render.Renderer
import io.spine.validation.bound.MaxFieldView
import io.spine.validation.bound.MaxPolicy
import io.spine.validation.bound.MinFieldView
import io.spine.validation.bound.MinPolicy
import io.spine.validation.bound.RangeFieldView
import io.spine.validation.bound.RangePolicy
import io.spine.validation.required.IfMissingPolicy
import io.spine.validation.required.RequireMessageView
import io.spine.validation.required.RequirePolicy
import io.spine.validation.required.RequiredFieldView
import io.spine.validation.required.RequiredIdOptionPolicy
import io.spine.validation.required.RequiredIdPatternPolicy
import io.spine.validation.required.RequiredPolicy

/**
 * The basic implementation of ProtoData validation plugin, which builds
 * language-agnostic representation of the declared constraints.
 *
 * The concrete implementations should provide [renderers], which implement
 * these constraints for a specific programming language.
 */
public abstract class ValidationPlugin(
    renderers: List<Renderer<*>> = emptyList(),
    views: Set<Class<out View<*, *, *>>> = setOf(),
    viewRepositories: Set<ViewRepository<*, *, *>> = setOf(),
    policies: Set<Policy<*>> = setOf(),
) : Plugin(
    renderers = renderers,
    views = views + setOf(
        RequiredFieldView::class.java,
        PatternFieldView::class.java,
        GoesFieldView::class.java,
        DistinctFieldView::class.java,
        ValidatedFieldView::class.java,
        RangeFieldView::class.java,
        MaxFieldView::class.java,
        MinFieldView::class.java,
        SetOnceFieldView::class.java,
        ChoiceGroupView::class.java,
        WhenFieldView::class.java,
        RequireMessageView::class.java,
    ),
    viewRepositories = viewRepositories,
    policies = policies + setOf<Policy<*>>(
        RequiredPolicy(),
        IfMissingPolicy(),
        RangePolicy(),
        MinPolicy(),
        MaxPolicy(),
        DistinctPolicy(),
        IfHasDuplicatesPolicy(),
        ValidatePolicy(),
        IfInvalidPolicy(),
        PatternPolicy(),
        ChoicePolicy(),
        IsRequiredPolicy(),
        WhenPolicy(),
        RequiredIdPatternPolicy(),
        RequiredIdOptionPolicy(),
        GoesPolicy(),
        SetOncePolicy(),
        IfSetAgainPolicy(),
        RequirePolicy()
    )
)
