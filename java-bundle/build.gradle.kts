import io.spine.internal.gradle.publish.SpinePublishing

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

plugins {
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.2")
    java
}

/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<SpinePublishing>()

/**
 * The ID of the far JAR artifact.
 */
val pArtifact = spinePublishing.artifactPrefix + "java-extensions"

dependencies {
    implementation(project(":java"))
}

publishing {
    val pGroup = project.group.toString()
    val pVersion = project.version.toString()

    publications {
        create("fat-jar", MavenPublication::class) {
            groupId = pGroup
            artifactId = pArtifact
            version = pVersion
            artifact(tasks.shadowJar)
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude(
        /**
         * Excluding this type to avoid it being located in the fat JAR.
         *
         * Locating this type in its own `io:spine:protodata` artifact is crucial
         * for obtaining proper version values from the manifest file.
         * This file is only present in `io:spine:protodata` artifact.
         */
        "io/spine/protodata/gradle/plugin/Plugin.class",
        "META-INF/gradle-plugins/io.spine.protodata.properties",

        /**
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

        /**
         * Remove all third-party plugin declarations as well.
         *
         * They should be loaded from their respective dependencies.
         */
        "META-INF/gradle-plugins/com**",
        "META-INF/gradle-plugins/net**",
        "META-INF/gradle-plugins/org**")

    setZip64(true)  /* The archive has way too many items. So using the Zip64 mode. */
    archiveClassifier.set("")    /** To prevent Gradle setting something like `osx-x86_64`. */
    mergeServiceFiles("desc.ref")
    mergeServiceFiles("META-INF/services/io.spine.option.OptionsProvider")
}
