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
package io.spine.validation.internal

import io.spine.protobuf.TypeConverter
import io.spine.protodata.TypeName
import io.spine.protodata.codegen.java.JavaRenderer
import io.spine.protodata.codegen.java.TypedInsertionPoint
import io.spine.protodata.renderer.SourceFileSet
import java.nio.file.Paths

/**
 * A renderer which adds utility methods to two diagnostic messages.
 *
 * The messages are `ListOfAnys` and `MapOfAnys`. This renderer allows their users to construct
 * instances of the messages by simply calling a static method.
 */
@Suppress("unused") // Accessed via reflection.
public class DiagsRenderer : JavaRenderer() {

    override fun render(sources: SourceFileSet) {
        sources.file(Paths.get("io/spine/validation/ListOfAnys.java"))
            .at(TypedInsertionPoint.CLASS_SCOPE.forType(LIST_TYPE))
            .withExtraIndentation(1)
            .add(
                "public static ListOfAnys from(Iterable<?> values) {",
                "    Builder b = newBuilder();",
                "    for (Object value : values) {",
                "        b.addValue($PACK(value));",
                "    }",
                "    return b.build();",
                "}"
            )
        sources.file(Paths.get("io/spine/validation/MapOfAnys.java"))
            .at(TypedInsertionPoint.CLASS_SCOPE.forType(MAP_TYPE))
            .withExtraIndentation(1)
            .add(
                "public static MapOfAnys from(${Map::class.java.canonicalName}<?, ?> map) {",
                "    Builder b = newBuilder();",
                "    map.forEach((k, v) -> {",
                "        b.addEntry(MapOfAnys.Entry.newBuilder()",
                "            .setKey($PACK(k))",
                "            .setValue($PACK(v)));",
                "    });",
                "    return b.build();",
                "}"
            )
    }
}

private val LIST_TYPE = TypeName
    .newBuilder()
    .setPackageName("spine.validation")
    .setSimpleName("ListOfAnys")
    .setTypeUrlPrefix("type.spine.io")
    .build()
private val MAP_TYPE = TypeName
    .newBuilder()
    .setPackageName("spine.validation")
    .setSimpleName("MapOfAnys")
    .setTypeUrlPrefix("type.spine.io")
    .build()
private val PACK = TypeConverter::class.qualifiedName + ".toAny"
