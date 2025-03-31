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

package io.spine.validation.required

import io.spine.core.External
import io.spine.protodata.ast.File
import io.spine.protodata.ast.FilePattern
import io.spine.protodata.ast.event.TypeDiscovered
import io.spine.protodata.ast.firstField
import io.spine.protodata.ast.matches
import io.spine.server.event.NoReaction
import io.spine.server.event.React
import io.spine.server.tuple.EitherOf2
import io.spine.validation.MessageMarkers
import io.spine.validation.event.RequiredFieldDiscovered

/**
 * A policy that marks ID fields in entity state messages and signal
 * messages as required.
 *
 * The messages are discovered via the file patterns, specified in
 * [ValidationConfig][io.spine.validation.ValidationConfig].
 *
 * The policy does nothing if ProtoData runs without the config.
 *
 * @see RequiredIdOptionPolicy
 */
internal class RequiredIdPatternPolicy : RequiredIdPolicy() {

    private val filePatterns: Set<FilePattern> by lazy {
        if (config == null) {
            emptySet()
        } else {
            val markers = config!!.messageMarkers
            markers.allPatterns()
        }
    }

    @React
    @Suppress("ReturnCount") // Prefer sooner exit and precise conditions.
    override fun whenever(
        @External event: TypeDiscovered
    ): EitherOf2<RequiredFieldDiscovered, NoReaction> {
        if (filePatterns.isEmpty()) {
            return ignore()
        }
        if (!event.file.matchesPatterns()) {
            return ignore()
        }
        val type = event.type
        val field = type.firstField
        return withField(field)
    }

    private fun File.matchesPatterns(): Boolean =
        filePatterns.any {
            it.matches(this)
        }
}

/**
 * All the file patterns that mark different types of Protobuf files.
 */
private fun MessageMarkers.allPatterns() = buildSet {
    addAll(eventPatternList)
    addAll(commandPatternList)
    addAll(rejectionPatternList)
}
