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

package io.spine.validation.test

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.fileDescriptorProto
import com.google.protobuf.fileDescriptorSet
import io.spine.base.fieldPath
import io.spine.validate.templateString
import io.spine.validation.api.FieldViolation
import io.spine.validation.api.MessageValidator
import io.spine.validation.api.Validator
import io.spine.validation.api.ValidatorViolation

@Validator(FileDescriptorSet::class)
public class FileDescriptorSetValidator : MessageValidator<FileDescriptorSet> {

    public override fun validate(message: FileDescriptorSet): List<ValidatorViolation> {
        if (message == ValidSet) {
            return emptyList()
        }

        val violation = FieldViolation(
            message = templateString {
                withPlaceholders = "Invalid file descriptor set."
            },
            fieldPath = fieldPath {
                fieldName.add("file")
            },
            fieldValue = message.fileList
        )

        return listOf(violation)
    }

    public companion object {

        /**
         * The [FileDescriptorSetValidator] passes only this instance as valid.
         */
        public val ValidSet: FileDescriptorSet = fileDescriptorSet {
            file.add(fileDescriptorProto {  })
        }
    }
}
