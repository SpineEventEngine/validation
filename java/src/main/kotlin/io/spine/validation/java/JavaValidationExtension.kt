/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.spine.protodata.plugin.Policy
import io.spine.protodata.plugin.View
import io.spine.protodata.plugin.ViewRepository
import io.spine.validation.java.generate.OptionGenerator

/**
 * Extends the Java validation library with the custom components.
 */
public interface JavaValidationExtension {

    /**
     * The option [generators][OptionGenerator] added by this extension.
     *
     * The generators are called in the order of their declaration in the extension.
     */
    public val generators: List<OptionGenerator>
        get() = emptyList()

    /**
     * The [views][View] added by this extension represented via their Java classes.
     */
    public val views: Set<Class<out View<*, *, *>>>
        get() = emptySet()

    /**
     * The [views][View] added by this extension represented via their
     * [repositories][ViewRepository].
     *
     * If passing events to a [View] does not require custom routing,
     * the view may not have a need for repository. In such a case,
     * please use [JavaValidationExtension.views] instead.
     */
    public val viewRepositories: Set<ViewRepository<*, *, *>>
        get() = emptySet()

    /**
     * The [policies][Policy] added by this extension.
     */
    public val policies: Set<Policy<*>>
        get() = emptySet()

}
