/*
 * Copyright 2026 CodeMatters, Lda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

import io.spine.dependency.build.ErrorProne
import io.spine.dependency.lib.Asm
import io.spine.dependency.lib.Grpc
import io.spine.dependency.lib.Guava
import io.spine.dependency.lib.JavaPoet
import io.spine.dependency.lib.JavaX
import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.BaseTypes
import io.spine.dependency.local.Change
import io.spine.dependency.local.Logging
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.CoreJvm
import io.spine.dependency.local.Reflect
import io.spine.dependency.local.Time
import io.spine.gradle.report.license.LicenseReporter

plugins {
    `fat-jar`
}
LicenseReporter.generateReportIn(project)

dependencies {

    implementation(project(":java")) {
        arrayOf(
            Asm.group,
            Guava.group,
            JavaX.annotationGroup,
            Protobuf.group,
            ErrorProne.group,
            Grpc.group /* Available via the Compiler backend. */,
        ).forEach {
            exclude(group = it)
        }

        // Transitive dependencies to the Compiler.
        Compiler.modules.forEach {
            it.split(":").let { (group, artifact) ->
                exclude(group = group, module = artifact)
            }
        }

        listOf(
            JavaPoet.group to JavaPoet.artifact /* Available via `tool-base` */,

            // Local dependencies.
            Base.group to Base.libModule,
            BaseTypes.group to BaseTypes.artifact,

            CoreJvm.group to CoreJvm.coreArtifact,
            CoreJvm.group to CoreJvm.clientArtifact,
            CoreJvm.group to CoreJvm.serverArtifact,

            Change.group to Change.artifact,
            Logging.group to Logging.loggingArtifact,
            Reflect.group to Reflect.artifact,
            Time.group to Time.libArtifact,

            CoreJvm.group to CoreJvm.serverArtifact,
        ).forEach { (group, artifact) ->
            exclude(group = group, module = artifact)
        }
    }
}
