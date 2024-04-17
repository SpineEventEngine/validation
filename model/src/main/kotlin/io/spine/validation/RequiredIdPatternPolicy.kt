/*
 * Copyright 2024, TeamDev. All rights reserved.
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
package io.spine.validation

import io.spine.core.External
import io.spine.protodata.Field
import io.spine.protodata.FilePattern
import io.spine.protodata.MessageType
import io.spine.protodata.event.TypeDiscovered
import io.spine.protodata.matches
import io.spine.protodata.qualifiedName
import io.spine.protodata.settings.loadSettings
import io.spine.server.event.React
import io.spine.server.model.NoReaction
import io.spine.server.tuple.EitherOf2
import io.spine.validation.event.RuleAdded

/**
 * A policy that marks ID fields in entity state messages and signal messages as required.
 *
 * The messages are discovered via the file patterns, specified in [ValidationConfig].
 * If ProtoData runs with no config, this policy never produces any validation rules.
 *
 * This policy has a sister—[RequiredIdOptionPolicy]. They both implement the required ID
 * constraint. However, this policy looks for the ID fields in messages that are defined in files
 * matching certain path patterns, and the other—in messages marked with certain options.
 *
 * @see RequiredIdOptionPolicy
 */
internal class RequiredIdPatternPolicy : RequiredIdPolicy() {

    private val filePatterns: List<FilePattern> by lazy {
        val markers = config!!.messageMarkers
        markers.allPatterns()
    }

    @React
    override fun whenever(@External event: TypeDiscovered): EitherOf2<RuleAdded, NoReaction> {
        if (config == null) {
            return withNothing()
        }
        val type = event.type
        val matchFile = filePatterns.any {
            it.matches(event.file)
        }
        if (!matchFile) {
            return withNothing()
        }
        val field = type.firstField()
        return withField(field)
    }

}
