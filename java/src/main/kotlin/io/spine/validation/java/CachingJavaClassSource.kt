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

package io.spine.validation.java

import org.jboss.forge.roaster.model.source.JavaClassSource
import org.jboss.forge.roaster.model.source.JavaSource
import org.jboss.forge.roaster.model.source.MethodSource

/**
 * A `JavaClassSource` which caches some of its parsed components.
 *
 * Please note that, because of the caching, sources updated in an underlying file may not be
 * updated in this model.
 */
internal class CachingJavaClassSource(
    private val delegate: JavaClassSource
) : JavaClassSource by delegate {

    /**
     * Cached value of [JavaClassSource.getNestedTypes()][JavaClassSource.getNestedTypes].
     */
    private val cachedNestedTypes: List<JavaSource<*>> by lazy {
        delegate.nestedTypes
    }

    override fun getNestedTypes(): List<JavaSource<*>> {
        return cachedNestedTypes
    }

    override fun getMethod(
        name: String?,
        vararg paramTypes: Class<*>?
    ): MethodSource<JavaClassSource> {
        return delegate.getMethod(name, *paramTypes)
    }

    override fun getMethod(
        name: String?,
        vararg paramTypes: String?
    ): MethodSource<JavaClassSource> {
        return delegate.getMethod(name, *paramTypes)
    }
}
