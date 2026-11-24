import io.spine.dependency.lib.AutoServiceKsp
import io.spine.gradle.report.license.LicenseReporter

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

buildscript {
    forceCodegenPlugins()
    configurations.all {
        resolutionStrategy {
            force(
                io.spine.dependency.lib.JetBrainsAnnotations.lib,
            )
        }
    }
}

plugins {
    java
    id("com.google.protobuf")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("module-testing")
}
LicenseReporter.generateReportIn(project)

dependencies {
    ksp(AutoServiceKsp.processor)
    spineCompiler(project(":java"))
    implementation(project(":java"))
    implementation(project(":tests:validator-dependency"))
}

spine {
    compiler {
        plugins(
            "io.spine.validation.java.JavaValidationPlugin",
            "io.spine.tools.compiler.jvm.style.JavaCodeStyleFormatterPlugin"
        )
    }

}
