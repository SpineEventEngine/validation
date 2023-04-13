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

import io.spine.internal.dependency.AutoService
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `build-proto-model`
}

dependencies {
    annotationProcessor(AutoService.processor)
    compileOnly(AutoService.annotations)

    val spine = io.spine.internal.dependency.Spine(project)
    implementation(spine.base)
    testImplementation(spine.testlib)
}

/**
 * Turn off codegen of Validation 1.0.
 */
modelCompiler {
    java {
        codegen {
            validation { skipValidation() }
        }
    }
}

tasks.withType<KotlinCompile> {
    exclude {
        it.file.absolutePath.startsWith("${buildDir.absolutePath}/generated/source/proto")
    }
//
//    doFirst {
//        println(sources)
//    }
}

tasks.withType<JavaCompile> {
    exclude {
        it.file.absolutePath.startsWith("${buildDir.absolutePath}/generated/source/proto")
    }
}

//tasks.withType<LaunchProtoData> {
//    enabled = false
//}
    sourceSets {
//        all {
//            allJava.exclude {
//                it.file.absolutePath.startsWith("${buildDir.absolutePath}/generated/source/proto")
//            }
//            allSource.exclude {
//                it.file.absolutePath.startsWith("${buildDir.absolutePath}/generated/source/proto")
//            }
//            kotlin.exclude {
//                it.file.absolutePath.startsWith("${buildDir.absolutePath}/generated/source/proto")
//            }
//        }

        main {
            java.srcDir("$projectDir/generated/main/java")
            java.srcDir("$projectDir/src/main/java")
            kotlin.srcDir("$projectDir/generated/main/kotlin")
            kotlin.srcDir("$projectDir/src/main/kotlin")
        }

        test {
            java.srcDir("$projectDir/generated/test/java")
            java.srcDir("$projectDir/src/test/java")
            kotlin.srcDir("$projectDir/generated/test/kotlin")
            kotlin.srcDir("$projectDir/src/test/kotlin")
        }
    }
