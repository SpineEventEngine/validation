/*
 * Copyright 2025, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.tools.validation.gradle

import io.spine.tools.compiler.gradle.api.Names
import io.spine.tools.meta.MavenArtifact
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/**
 * Gradle plugin that configures the Spine Compiler to run the Validation Compiler in solo mode.
 *
 * The plugin applies the Spine Compiler Gradle plugin (id = `io.spine.compiler`) and then
 * registers the Validation compiler implementation using the Compiler's Gradle extension.
 *
 * We avoid hard dependencies on the Compiler Gradle API types and configure the extension
 * reflectively to stay compatible across minor API changes.
 */
public class ValidationPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Ensure the Spine Compiler plugin is applied.
        project.pluginManager.apply("io.spine.compiler")

        val version = resolveValidationVersion(project)
        val validationBundle =
            "io.spine.validation:spine-validation-java-bundle:$version"
        val validationPluginClass =
            "io.spine.validation.java.JavaValidationPlugin"

        val spineExt = project.extensions.findByName("spineCompiler") ?: return
        configureValidation(spineExt, validationBundle, validationPluginClass)
    }

    private fun resolveValidationVersion(project: Project): String =
        this::class.java.`package`.implementationVersion
            ?: project.findProperty("validationVersion")?.toString()
            ?: project.version.toString()

    private fun configureValidation(
        spineExt: Any,
        validationBundle: String,
        validationPluginClass: String
    ) {
        val pluginsContainer = invokeNoArg(spineExt, "plugins")
        if (pluginsContainer != null) {
            configureLibrary(pluginsContainer, validationBundle)
            configurePluginClass(pluginsContainer, validationPluginClass)
        } else {
            configureLibrary(spineExt, validationBundle)
            configurePluginClass(spineExt, validationPluginClass)
        }
    }

    private fun configureLibrary(target: Any, validationBundle: String) {
        tryInvoke(target, "artifact", validationBundle)
            || tryInvoke(target, "library", validationBundle)
            || tryInvoke(target, "pluginLib", validationBundle)
            || tryInvoke(target, "lib", validationBundle)
    }

    private fun configurePluginClass(target: Any, className: String) {
        tryInvoke(target, "plugin", className)
            || tryInvoke(target, "pluginClass", className)
            || tryInvoke(target, "pluginClasses", listOf(className))
    }

    private fun invokeNoArg(target: Any, name: String): Any? = try {
        val m = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }
        m?.isAccessible = true
        m?.invoke(target)
    } catch (_: Throwable) {
        null
    }

    private fun tryInvoke(target: Any, name: String, vararg args: Any): Boolean = try {
        val methods = target.javaClass.methods
            .filter { it.name == name && it.parameterCount == args.size }
        val method = methods.firstOrNull { m ->
            m.parameterTypes.zip(args).all { (t, a) ->
                t.isAssignableFrom(a.javaClass) || isListToCollection(t, a)
            }
        }
        if (method != null) {
            method.isAccessible = true
            method.invoke(target, *args)
            true
        } else false
    } catch (_: Throwable) {
        false
    }

    private fun isListToCollection(paramType: Class<*>, arg: Any): Boolean =
        (Collection::class.java.isAssignableFrom(paramType)) && (arg is Collection<*>)
}

@Suppress("unused")
private fun Project.addUserClasspathDependency(vararg artifacts: MavenArtifact) =
    artifacts.forEach {
        addDependency(Names.USER_CLASSPATH_CONFIGURATION, it)
    }

private fun Project.addDependency(configuration: String, artifact: MavenArtifact) {
    val dependency = findDependency(artifact) ?: artifact.coordinates
    dependencies.add(configuration, dependency)
}

private fun Project.findDependency(artifact: MavenArtifact): Dependency? {
    val dependencies = configurations.flatMap { c -> c.dependencies }
    val found = dependencies.firstOrNull { d ->
        artifact.group == d.group // `d.group` could be `null`.
                && artifact.name == d.name
    }
    return found
}
