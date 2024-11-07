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

package io.spine.validation.java

import io.spine.protodata.plugin.Plugin
import io.spine.protodata.render.Renderer
import io.spine.server.BoundedContextBuilder
import io.spine.validation.ValidationPlugin
import io.spine.validation.java.point.PrintValidationInsertionPoints

/**
 * A plugin that sets up everything needed to generate Java validation code.
 *
 * This plugin uses a delegate plugin to set up some on the components needed for
 * code generation. By default, a [ValidationPlugin] is used. However, API users may
 * extend this plugin's behavior and supply a more rich base plugin.
 *
 * @param base The base plugin to extend.
 * @constructor Creates an instance that contains all the components from the given [base] plugin.
 */
@Suppress("unused") // Accessed via reflection.
public class JavaValidationPlugin(
    private val base: Plugin
) : Plugin(
    renderers = mergeRenderers(base),
    views = base.views,
    viewRepositories = base.viewRepositories,
    policies = base.policies
) {
    /**
     * The constructor to be involed reflectively by ProtoData.
     */
    public constructor() : this(ValidationPlugin())

    override fun extend(context: BoundedContextBuilder) {
        base.extend(context)
    }

    public companion object {

        /**
         * Orders the renderers in such a way that the renderers of
         * the `base` plugin always come before its own renderers.
         */
        private fun mergeRenderers(base: Plugin): List<Renderer<*>> = buildList {
            addAll(base.renderers)
            add(PrintValidationInsertionPoints())
            add(JavaValidationRenderer())
            add(ImplementValidatingBuilder())
            add(SetOnceValidationRenderer())
        }
    }
}
