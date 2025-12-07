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

import io.spine.tools.compiler.plugin.Plugin
import io.spine.tools.compiler.plugin.Reaction
import io.spine.tools.compiler.plugin.View
import io.spine.tools.compiler.plugin.ViewRepository
import io.spine.tools.compiler.render.Renderer
import io.spine.tools.validation.bound.MaxFieldView
import io.spine.tools.validation.bound.MaxReaction
import io.spine.tools.validation.bound.MinFieldView
import io.spine.tools.validation.bound.MinReaction
import io.spine.tools.validation.bound.RangeFieldView
import io.spine.tools.validation.bound.RangeReaction
import io.spine.tools.validation.option.ChoiceGroupView
import io.spine.tools.validation.option.ChoiceReaction
import io.spine.tools.validation.option.DistinctFieldView
import io.spine.tools.validation.option.DistinctReaction
import io.spine.tools.validation.option.GoesFieldView
import io.spine.tools.validation.option.GoesReaction
import io.spine.tools.validation.option.IfHasDuplicatesReaction
import io.spine.tools.validation.option.IfInvalidReaction
import io.spine.tools.validation.option.IfSetAgainReaction
import io.spine.tools.validation.option.IsRequiredReaction
import io.spine.tools.validation.option.PatternFieldView
import io.spine.tools.validation.option.PatternReaction
import io.spine.tools.validation.option.RequireMessageView
import io.spine.tools.validation.option.RequireReaction
import io.spine.tools.validation.option.SetOnceFieldView
import io.spine.tools.validation.option.SetOnceReaction
import io.spine.tools.validation.option.ValidateReaction
import io.spine.tools.validation.option.ValidatedFieldView
import io.spine.tools.validation.option.WhenFieldView
import io.spine.tools.validation.option.WhenReaction
import io.spine.tools.validation.option.required.IfMissingReaction
import io.spine.tools.validation.option.required.RequiredFieldView
import io.spine.tools.validation.option.required.RequiredIdOptionReaction
import io.spine.tools.validation.option.required.RequiredReaction
import io.spine.tools.validation.option.required.RequiredIdPatternReaction

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
    reactions: Set<Reaction<*>> = setOf(),
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
    reactions = reactions + setOf<Reaction<*>>(
        RequiredReaction(),
        IfMissingReaction(),
        RangeReaction(),
        MinReaction(),
        MaxReaction(),
        DistinctReaction(),
        IfHasDuplicatesReaction(),
        ValidateReaction(),
        IfInvalidReaction(),
        PatternReaction(),
        ChoiceReaction(),
        IsRequiredReaction(),
        WhenReaction(),
        RequiredIdPatternReaction(),
        RequiredIdOptionReaction(),
        GoesReaction(),
        SetOnceReaction(),
        IfSetAgainReaction(),
        RequireReaction()
    )
)
