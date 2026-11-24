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

import io.spine.dependency.lib.Protobuf
import io.spine.dependency.local.Base
import io.spine.dependency.local.Compiler
import io.spine.dependency.local.ToolBase
import io.spine.dependency.local.Validation
import io.spine.gradle.artifactId
import io.spine.gradle.publish.addSourceAndDocJars

plugins {
    module
    id("io.spine.artifact-meta")
    `java-gradle-plugin`
}

val moduleArtifactId = "validation-gradle-plugin"

apply(from = "$rootDir/version.gradle.kts")

val validationVersion = extra["validationVersion"] as String

artifactMeta {
    artifactId.set(moduleArtifactId)
    addDependencies(
        // Add Validation module dependencies that we use for project configuration
        // to which the Validation Gradle Plugin is applied.
        Validation.javaBundle(validationVersion),
        Validation.runtime(validationVersion),
    )
    excludeConfigurations {
        containing(*buildToolConfigurations)
    }
}

gradlePlugin {
    website.set("https://spine.io/")
    vcsUrl.set("https://github.com/SpineEventEngine/validation.git")
    plugins {
        val pluginTags = listOf(
            "validation",
            "ddd",
            "codegen",
            "java",
            "kotlin",
            "jvm"
        )

        create("spineValidation") {
            id = "io.spine.validation"
            implementationClass = "io.spine.tools.validation.gradle.ValidationGradlePlugin"
            displayName = "Spine Validation Compiler Gradle Plugin"
            description = "Configures Spine Compiler to run the Validation Compiler."
            tags.set(pluginTags)
        }
    }
}

configurations.all {
    resolutionStrategy {
        force(
            ToolBase.jvmTools
        )
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    // Needed at test runtime too: applying the plugin under `ProjectBuilder`
    // exercises the Spine Compiler Gradle plugin, which references
    // `org.gradle.kotlin.dsl` extensions.
    testImplementation(gradleKotlinDsl())

    arrayOf(
        Protobuf.GradlePlugin.lib,
        Base.format,
        Compiler.api,
        Compiler.params,
        Compiler.pluginLib,
        Compiler.gradleApi,
        ToolBase.jvmTools
    ).forEach {
        implementation(it) {
            excludeJetBrainsAnnotations()
        }
    }
    implementation(project(":java-settings")) {
        excludeJetBrainsAnnotations()
    }
}

// Change the `artifactId` to have the `validation-` prefix.
// This is needed because this module has custom publication.
afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                artifactId = project.artifactId
                addSourceAndDocJars(project)
            }
        }
    }

    val sourcesJar = tasks.getByName<Jar>("sourcesJar")
    val writeArtifactMeta = tasks.getByName("writeArtifactMeta")
    sourcesJar.dependsOn(writeArtifactMeta)
}
