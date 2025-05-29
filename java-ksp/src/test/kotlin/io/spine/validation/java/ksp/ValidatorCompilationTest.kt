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

package io.spine.validation.java.ksp

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import com.intellij.util.lang.JavaVersion
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.java
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import com.tschuchort.compiletesting.configureKsp
import io.spine.logging.testing.ConsoleTap
import io.spine.logging.testing.tapConsole
import io.spine.validation.api.Validator
import io.spine.validation.api.MessageValidator
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base for tests checking discovering of
 * the [@Validator][io.spine.validation.api.Validator] annotation.
 */
@OptIn(ExperimentalCompilerApi::class)
internal sealed class ValidatorCompilationTest {

    companion object {

        /**
         * Suppress excessive console output produced by [KotlinCompilation.compile].
         *
         * @see <a href="https://github.com/google/ksp/issues/1687">Related issue</a>
         * @see KotlinCompilation.compileSilently
         */
        @BeforeAll
        @JvmStatic
        fun redirectStreams() {
            ConsoleTap.install()
        }
    }

    protected lateinit var compilation: KotlinCompilation

    @BeforeEach
    fun prepareCompilation() {
        compilation = KotlinCompilation()
        compilation.jvmTarget = JavaVersion.current().toString()

        val dependencyJars = setOf(
            Validator::class.java, // The annotation.
            MessageValidator::class.java, // The validator interface.
            Timestamp::class.java, // The external message to use.
            Message::class.java, // Protobuf.
            ValidatorProcessorProvider::class.java, // The test subject.
        ).map { it.classpathFile() }

        compilation.apply {
            javaPackagePrefix = "io.spine.validation.java.ksp"
            configureKsp (useKsp2 = true) {
                symbolProcessorProviders += ValidatorProcessorProvider()
            }
            classpaths = classpaths + dependencyJars
        }
    }

    /**
     * Performs compilation with redirected console output.
     *
     * @see io.spine.logging.testing.ConsoleTap.install
     * @see tapConsole
     */
    @OptIn(ExperimentalCompilerApi::class)
    fun KotlinCompilation.compileSilently(): JvmCompilationResult {
        var result: JvmCompilationResult? = null
        tapConsole {
            result = compile()
        }
        return result!!
    }
}

/**
 * Obtains the path to the classpath element which contains the receiver class.
 */
private fun Class<*>.classpathFile(): File = File(protectionDomain.codeSource.location.path)

/**
 * The package used to define Java and Kotlin files.
 */
private const val packageDir = "io/spine/validation/java/ksp/test"

/**
 * Creates an instance of [SourceFile] with the Java file containing the class
 * with the specified name and contents.
 */
internal fun javaFile(simpleClassName: String, contents: String): SourceFile = java(
    name = "$packageDir/${simpleClassName}.java",
    contents = contents,
    trimIndent = true
)

/**
 * Creates an instance of [SourceFile] with the Kotlin file containing the class
 * with the specified name and contents.
 */
internal fun kotlinFile(simpleClassName: String, contents: String): SourceFile = kotlin(
    name = "$packageDir/${simpleClassName}.kt",
    contents = contents,
    trimIndent = true
)
