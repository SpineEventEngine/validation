/*
 * Copyright 2021, TeamDev. All rights reserved.
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

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.PublishingRepos.gitHub
import io.spine.internal.gradle.Scripts
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.spinePublishing
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    io.spine.internal.gradle.doApplyStandard(repositories)

    apply(from = "$rootDir/version.gradle.kts")

    val spineBaseVersion: String by extra
    val protoDataVersion: String by extra

    dependencies {
        classpath("io.spine.tools:spine-mc-java:$spineBaseVersion")
        classpath("io.spine:proto-data:$protoDataVersion")
    }
}

plugins {
    `java-library`
    idea

    val protobuf = io.spine.internal.dependency.Protobuf.GradlePlugin
    val errorProne = io.spine.internal.dependency.ErrorProne.GradlePlugin
    val dokka = io.spine.internal.dependency.Kotlin.Dokka

    id(protobuf.id).version(protobuf.version)
    id(errorProne.id).version(errorProne.version)
    kotlin("jvm") version(io.spine.internal.dependency.Kotlin.version)
    id(dokka.pluginId) version(dokka.version)
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")

    repositories {
        applyStandard()
        val protoDataRepo = gitHub("ProtoData")
        maven {
            url = uri(protoDataRepo.releases)
            credentials {
                val creds = protoDataRepo.credentials(project)!!
                username = creds.username
                password = creds.password
            }
        }
    }

    apply {
        plugin("idea")
        plugin("project-report")
    }

    group = "io.spine"
    val validationVersion: String by extra
    version = validationVersion
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("org.jetbrains.dokka")
        plugin("io.spine.mc-java")
        plugin("com.google.protobuf")
        from(Scripts.projectLicenseReport(project))
        from(Scripts.slowTests(project))
        from(Scripts.testOutput(project))
        from(Scripts.javadocOptions(project))
        from(Scripts.modelCompiler(project))
    }

    dependencies {
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xinline-classes",
                "-Xjvm-default=all"
            )
        }
    }

    val dokkaJavadoc by tasks.getting(DokkaTask::class)

    tasks.register("javadocJar", Jar::class) {
        from(dokkaJavadoc.outputDirectory)
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
    }
}

spinePublishing {
    projectsToPublish.addAll(
        "model",
        "java"
    )
    spinePrefix.set(false)
    // Publish to the ProtoData repository reduce configuration for end users.
    targetRepositories.add(gitHub("ProtoData"))
}

apply {
    from(Scripts.repoLicenseReport(project))
    from(Scripts.generatePom(project))
}
