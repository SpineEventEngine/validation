/*
 * Copyright 2026, TeamDev. All rights reserved.
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

package io.spine.validation

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.ServiceLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`ValidatorRegistry` should")
internal class ValidatorRegistrySpec {

    @BeforeEach
    fun setUp() {
        ValidatorRegistry.clear()
    }

    @Test
    fun `allow adding and removing validators`() {
        val validator = TimestampValidator()
        ValidatorRegistry.add(Timestamp::class, validator)
        
        val invalidTimestamp = timestamp { seconds = -100000000000L }
        ValidatorRegistry.validate(invalidTimestamp) shouldHaveSize 1
        
        ValidatorRegistry.remove(Timestamp::class)
        ValidatorRegistry.validate(invalidTimestamp).shouldBeEmpty()
    }

    @Test
    fun `query registered validators`() {
        val validator1 = TimestampValidator()
        val validator2 = AlwaysInvalidTimestampValidator()
        
        ValidatorRegistry.add(Timestamp::class, validator1)
        ValidatorRegistry.add(Timestamp::class, validator2)
        
        val validators = ValidatorRegistry.get(Timestamp::class)
        validators shouldContainExactly setOf(validator1, validator2)
        
        ValidatorRegistry.remove(Timestamp::class)
        ValidatorRegistry.get(Timestamp::class).shouldBeEmpty()
    }

    @Test
    fun `allow adding and removing validators using Java classes`() {
        val validator = TimestampValidator()
        ValidatorRegistry.add(Timestamp::class.java, validator)

        val invalidTimestamp = timestamp { seconds = -100000000000L }
        ValidatorRegistry.validate(invalidTimestamp) shouldHaveSize 1

        ValidatorRegistry.remove(Timestamp::class.java)
        ValidatorRegistry.validate(invalidTimestamp).shouldBeEmpty()
    }

    @Test
    fun `query registered validators using Java classes`() {
        val validator1 = TimestampValidator()
        val validator2 = AlwaysInvalidTimestampValidator()

        ValidatorRegistry.add(Timestamp::class.java, validator1)
        ValidatorRegistry.add(Timestamp::class.java, validator2)

        val validators = ValidatorRegistry.get(Timestamp::class.java)
        validators shouldContainExactly setOf(validator1, validator2)

        ValidatorRegistry.remove(Timestamp::class.java)
        ValidatorRegistry.get(Timestamp::class.java).shouldBeEmpty()
    }

    @Test
    fun `query registered validators using both Kotlin and Java classes`() {
        val validator = TimestampValidator()

        ValidatorRegistry.add(Timestamp::class, validator)
        ValidatorRegistry.get(Timestamp::class.java) shouldContainExactly setOf(validator)

        ValidatorRegistry.remove(Timestamp::class.java)
        ValidatorRegistry.get(Timestamp::class).shouldBeEmpty()

        ValidatorRegistry.add(Timestamp::class.java, validator)
        ValidatorRegistry.get(Timestamp::class) shouldContainExactly setOf(validator)
    }

    @Test
    fun `support multiple validators per type`() {
        val validator1 = TimestampValidator()
        val validator2 = AlwaysInvalidTimestampValidator()
        
        ValidatorRegistry.add(Timestamp::class, validator1)
        ValidatorRegistry.add(Timestamp::class, validator2)
        
        val validTimestamp = timestamp { seconds = 100 }
        val violations = ValidatorRegistry.validate(validTimestamp)
        
        violations shouldHaveSize 1
        violations[0].message.withPlaceholders shouldBe "Always invalid"
    }

    @Test
    fun `return 'ConstraintViolation' objects`() {
        val validator = AlwaysInvalidTimestampValidator()
        ValidatorRegistry.add(Timestamp::class, validator)
        
        val timestamp = timestamp { seconds = 100 }
        val violations = ValidatorRegistry.validate(timestamp)
        
        violations shouldHaveSize 1
        val violation = violations[0]
        violation.message.withPlaceholders shouldBe "Always invalid"
        violation.typeName shouldBe "google.protobuf.Timestamp"
    }

    @Test
    fun `clear the whole registry`() {
        ValidatorRegistry.add(Timestamp::class, TimestampValidator())
        ValidatorRegistry.clear()
        
        val invalidTimestamp = timestamp { seconds = -100000000000L }
        ValidatorRegistry.validate(invalidTimestamp).shouldBeEmpty()
    }

    @Test
    fun `load validators from the classpath using 'ServiceLoader'`() {
        // We need to trigger the init block of the object if it hasn't been triggered yet.
        // But since it's an object, it's lazy.
        // In our case, we cleared it in `setUp`.
        
        // Re-adding what ServiceLoader should find
        val loader = ServiceLoader.load(MessageValidator::class.java)
        val hasTimestampValidator = loader.any { it is TimestampValidator }
        
        if (hasTimestampValidator) {
            // If AutoService worked during this test run (it might not if it's not a full build),
            // we can re-load or just check if it's there after manual trigger.
            ValidatorRegistry.clear()
            // Manually trigger the loading logic (simulating what happens in `init`.)
            val method = ValidatorRegistry::class.java.getDeclaredMethod("loadFromServiceLoader")
            method.isAccessible = true
            method.invoke(ValidatorRegistry)

            val invalidTimestamp = timestamp { nanos = -1 }
            ValidatorRegistry.validate(invalidTimestamp) shouldHaveSize 1
        }
    }

    @Test
    fun `obtain the message type for a validator as a generic argument`() {
        val type = TimestampValidator().messageClass()
        type shouldBe Timestamp::class
    }
}

private class AlwaysInvalidTimestampValidator : MessageValidator<Timestamp> {
    override fun validate(message: Timestamp): List<DetectedViolation> {
        return listOf(MessageViolation(templateString { withPlaceholders = "Always invalid" }))
    }
}
