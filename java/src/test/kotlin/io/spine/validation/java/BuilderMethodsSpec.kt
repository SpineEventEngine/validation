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

package io.spine.validation.java

import io.kotest.matchers.string.shouldContain
import io.spine.protodata.renderer.SourceFileSet
import io.spine.validation.java.given.MockPrinter
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("Message builder should")
class BuilderMethodsSpec {

    private lateinit var code: String

    @BeforeEach
    fun setUp(@TempDir sources: Path) {
        val file = sources / "Example.java"
        file.writeText("""
            package foo.bar;
            
            class Example extends com.google.protobuf.GeneratedMessageV3 {
           
                class Builder {
                
                    @Override
                    public foo.bar.Example build() {
                        return new Example();
                    }
                    
                    @Override
                    public foo.bar.Example buildPartial() {
                        return new Example();
                    }
                }
            }
        """.trimIndent())
        val sourceFiles = SourceFileSet.from(sources)
        val printer = MockPrinter(setOf(
            BuildMethodReturnTypeAnnotation(),
            BuildPartialReturnTypeAnnotation()
        ))
        printer.renderSources(sourceFiles)
        sourceFiles.forEach { it.text() } // Run lazy operations.
        sourceFiles.write()
        code = file.readText()
    }

    @Suppress("MaxLineLength")
    @Test
    fun `allow to annotate 'build()'`() {
        code shouldContain Regex(
            "public foo\\.bar\\. /\\* .+${BuildMethodReturnTypeAnnotation::class.simpleName}.+ \\*/ Example build\\(\\)"
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `allow to annotate 'buildPartial()'`() {
        code shouldContain Regex(
            "public foo\\.bar\\. /\\* .*${BuildPartialReturnTypeAnnotation::class.simpleName}.* \\*/ Example buildPartial\\(\\)"
        )
    }
}
