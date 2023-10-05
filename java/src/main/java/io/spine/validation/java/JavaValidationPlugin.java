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
import io.spine.protodata.plugin.View;
import io.spine.protodata.plugin.ViewRepository;
import io.spine.protodata.renderer.Renderer;
import io.spine.server.BoundedContextBuilder;
import io.spine.validation.ValidationPlugin;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A plugin that sets up everything needed to generate Java validation code.
 *
 * <p>This plugin uses a delegate plugin to set up some on the components needed for
 * code generation. By default, a {@link ValidationPlugin} is used. However, API users may
 * extend this plugin's behavior and supply a more rich base plugin.
 */
@SuppressWarnings("unused") // Accessed via reflection.
public final class JavaValidationPlugin implements Plugin {

    private final Plugin base;

    /**
     * Constructs a {@code JavaValidationPlugin} that contains all the components from
     * the given {@code base} plugin.
     *
     * @param base the base plugin to extend
     */
    public JavaValidationPlugin(Plugin base) {
        this.base = checkNotNull(base);
    }

    /**
     * Constructs a {@code JavaValidationPlugin} based on a {@link ValidationPlugin}.
     *
     * <p>This is the default constructor used by ProtoData's reflective mechanisms.
     */
    public JavaValidationPlugin() {
        this(new ValidationPlugin());
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@code JavaValidationPlugin} orders the renderers in such a way that the renderers of
     * the {@code base} plugin always come before its own renderers.
     */
    @Override
    public List<Renderer<?>> renderers() {
        var result = ImmutableList.<Renderer<?>>builder();
        result.addAll(base.renderers());
        result.add(new PrintValidationInsertionPoints(),
                   new JavaValidationRenderer());
        return result.build();
    }

    @Override
    public void extend(BoundedContextBuilder context) {
        base.extend(context);
    }

    @Override
    public Set<Class<? extends View<?, ?, ?>>> views() {
        return base.views();
    }

    @Override
    public Set<Policy<?>> policies() {
        return base.policies();
    }

    @Override
    public Set<ViewRepository<?, ?, ?>> viewRepositories() {
        return base.viewRepositories();
    }
}
