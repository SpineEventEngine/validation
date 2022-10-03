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

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.AutoService
import io.spine.internal.dependency.Protobuf
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.protobuf.setup
import io.spine.internal.gradle.publish.excludeGoogleProtoFromArtifacts

val spineBaseVersion: String by extra

dependencies {
    Protobuf.libs.forEach { api(it) }
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)
    implementation("io.spine:spine-base:$spineBaseVersion")
    testImplementation("io.spine.tools:spine-testlib:$spineBaseVersion")
}

val generatedDir by extra("$projectDir/generated")
val generatedJavaDir by extra("$generatedDir/main/java")
val generatedTestJavaDir by extra("$generatedDir/test/java")

sourceSets {
    main {
        java.srcDir(generatedJavaDir)
        resources.srcDirs(
            "$generatedDir/main/resources",
            "$buildDir/descriptors/main"
        )
    }
    test {
        java.srcDir(generatedTestJavaDir)
        resources.srcDirs(
            "$generatedDir/test/resources",
            "$buildDir/descriptors/test"
        )
    }
}

protobuf {
    configurations.excludeProtobufLite()
    generatedFilesBaseDir = generatedDir
    protoc {
        artifact = Protobuf.compiler
    }
    generateProtoTasks {
        for (task in all()) {
            task.setup(generatedDir)
        }
    }
}

tasks {
    excludeGoogleProtoFromArtifacts()
}