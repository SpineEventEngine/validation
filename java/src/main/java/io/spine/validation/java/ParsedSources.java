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

package io.spine.validation.java;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

/**
 * Parses the source code via {@code Roaster} and caches the results for further use.
 */
final class ParsedSources {

    /**
     * Cached results of parsing the Java source code.
     */
    private final LoadingCache<String, JavaSource<?>> cache =
            CacheBuilder.newBuilder()
                    .maximumSize(300)
                    .build(loader());

    private static CacheLoader<String, JavaSource<?>> loader() {
        return new CacheLoader<>() {
            @Override
            public JavaSource<?> load(String code) {
                var result = Roaster.parse(JavaSource.class, code);
                if (result.isClass()) {
                    return new CachingJavaClassSource((JavaClassSource) result);
                }
                return result;
            }
        };
    }

    /**
     * Parses the Java code and returns it as the parsed {@code JavaSource},
     * caching it for future use.
     *
     * <p>If the code was parsed previously, most likely the cached result
     * is returned right away, as the cache stores 300 items max.
     */
    JavaSource<?> get(String code) {
        var result = cache.getUnchecked(code);
        return result;
    }
}
