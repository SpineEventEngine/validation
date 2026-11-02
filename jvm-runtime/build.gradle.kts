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

@file:Suppress("RemoveRedundantQualifierName")

import io.spine.dependency.lib.AutoService
import io.spine.dependency.lib.AutoServiceKsp
import io.spine.dependency.local.Base
import io.spine.dependency.local.Spine
import io.spine.dependency.local.TestLib

buildscript {
    standardSpineSdkRepositories()
    configurations {
        all {
            resolutionStrategy {
                force(
                    io.spine.dependency.local.Logging.grpcContext,
                    io.spine.dependency.lib.JetBrainsAnnotations.lib,
                )
            }
        }
    }
}

plugins {
    module
    `build-proto-model`
    id("io.spine.generated-sources")
    id("io.spine.descriptor-set-file")
    // We use it the KSP plugin via its ID because it's added to the build classpath
    // in the root project.
    id("com.google.devtools.ksp")
    id("maven-publish")
}

group = Spine.group

dependencies {
    ksp(AutoServiceKsp.processor)
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    api(Base.lib)

    testImplementation(TestLib.lib)
}

forceSpineBase()

// KSP reads the Java sources that the Protobuf tasks write to `generated/<source set>/java`.
mapOf(
    "kspKotlin" to "generateProto",
    "kspTestKotlin" to "generateTestProto",
).forEach { (ksp, generateProto) ->
    tasks.named { it == ksp }.configureEach {
        dependsOn(generateProto)
    }
}
