/*
 * Copyright 2023, TeamDev. All rights reserved.
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

import io.spine.gradle.publish.SpinePublishing

plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow")
}

/**
 * Creates a custom publication called `fatJar`, taking the output of `tasks.shadowJar`
 * as the sole publication artifact.
 *
 * This kind of publishing works in combination with exclusion of standard publishing for this
 * module specified in the root project under [SpinePublishing.modulesWithCustomPublishing].
 */
publishing {
    publications {
        create("fatJar", MavenPublication::class) {
            artifact(tasks.shadowJar)
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude(
        /*
         * Excluding this type to avoid it being located in the fat JAR.
         *
         * Locating this type in its own `io:spine:protodata` artifact is crucial
         * for obtaining proper version values from the manifest file.
         * This file is only present in `io:spine:protodata` artifact.
         */
        "io/spine/protodata/gradle/plugin/Plugin.class",
        "META-INF/gradle-plugins/io.spine.protodata.properties",

        /*
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

        /*
         * Remove all third-party plugin declarations as well.
         *
         * They should be loaded from their respective dependencies.
         */
        "META-INF/gradle-plugins/com**",
        "META-INF/gradle-plugins/net**",
        "META-INF/gradle-plugins/org**",

        /* Exclude license files that cause or may cause issues with LicenseReport.
           We analyze these files when building artifacts we depend on. */
        "about_files/**",
        "license/**",

        "ant_tasks/**", // `resource-ant.jar` is of no use here.

        /* Exclude `https://github.com/JetBrains/pty4j`.
          We don't need the terminal. */
        "resources/com/pty4j/**",

        // Protobuf files.
        "google/**",
        "spine/**",
        "src/**",

        // Java source code files of the package `org.osgi`.
        "OSGI-OPT/**",

        // Kotlin runtime. It's going to be provided.
        "kotlin/**",
        "kotlinx/**",

        // Annotations available via ProtoData classpath.
        "android/annotation/**",
        "javax/annotation/**",
    )

    isZip64 = true  /* The archive has way too many items. So using the Zip64 mode. */
    archiveClassifier.set("")    /** To prevent Gradle setting something like `osx-x86_64`. */
    mergeServiceFiles("desc.ref")
    mergeServiceFiles("META-INF/services/io.spine.option.OptionsProvider")
}

/**
 * Makes the receiver task depend on the `jar` task.
 */
fun Task.dependsOnJar() {
    dependsOn(tasks.jar)
    logger.debug("Task `${this.name}` now depends on `${tasks.jar.name}`.")
}

/**
 * Declare dependencies explicitly to address Gradle warnings.
 */

tasks.withType<PublishToMavenLocal> {
    this.dependsOnJar()
}

tasks.withType<PublishToMavenRepository> {
    this.dependsOnJar()
}
