/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Asm
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.JavaPoet
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.lib.Roaster
import io.spine.dependency.local.Base
import io.spine.dependency.local.BaseTypes
import io.spine.dependency.local.Change
import io.spine.dependency.local.CoreJava
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.Text
import io.spine.dependency.local.Time

plugins {
    `fat-jar`
}

dependencies {

    implementation(project(":java")) {
        arrayOf(
            Asm.group,
            Guava.group,
            JavaX.annotationGroup,
            Protobuf.group,
            ErrorProne.group,
            Grpc.group /* Available via ProtoData backend. */,
            Roaster.group /* Available via `tool-base`. */,

            // Local dependencies.
            Compiler.group,
        ).forEach {
            exclude(group = it)
        }

        listOf(
            JavaPoet.group to JavaPoet.artifact /* Available via `tool-base` */,

            // Local dependencies.
            Base.group to Base.artifact,
            BaseTypes.group to BaseTypes.artifact,

            CoreJava.group to CoreJava.coreArtifact,
            CoreJava.group to CoreJava.clientArtifact,
            CoreJava.group to CoreJava.serverArtifact,

            Change.group to Change.artifact,
            Logging.group to Logging.loggingArtifact,
            Reflect.group to Reflect.artifact,
            Text.group to Text.artifact,
            Time.group to Time.artifact,

            CoreJava.group to CoreJava.serverArtifact,
        ).forEach { (group, artifact) ->
            exclude(group = group, module = artifact)
        }
    }
}
