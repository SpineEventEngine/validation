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

package io.spine.validation.java;

import com.google.common.collect.ImmutableList;
import io.spine.protodata.plugin.Plugin;
import io.spine.protodata.plugin.Policy;
import io.spine.protodata.plugin.ViewRepository;
import io.spine.protodata.renderer.Renderer;
import io.spine.validation.ValidationPlugin;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A plugin that sets up everything needed to generate Java validation code.
 */
@SuppressWarnings("unused") // Accessed via
public final class JavaValidationPlugin implements Plugin {

    private final Plugin base;

    public JavaValidationPlugin(Plugin base) {
        this.base = checkNotNull(base);
    }

    @SuppressWarnings("unused") // Used for reflective initialization.
    public JavaValidationPlugin() {
        this(new ValidationPlugin());
    }

    @Override
    public Set<Policy<?>> policies() {
        return base.policies();
    }

    @Override
    public Set<ViewRepository<?, ?, ?>> viewRepositories() {
        return base.viewRepositories();
    }

    @Override
    public List<Renderer<?>> renderers() {
        return ImmutableList.of(
                new PrintValidationInsertionPoints(),
                new JavaValidationRenderer()
        );
    }
}
