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

package io.spine.validate

/**
 * Allows determining safe variants of number conversions without losing precision.
 *
 * Mimics the actual automatic conversions that are applied to primitive number types.
 */
internal object NumberConversion {

    private val checkers: List<ConversionChecker<out Number>> = listOf(
        ByteChecker(), ShortChecker(), IntegerChecker(), LongChecker(),
        FloatChecker(), DoubleChecker()
    )

    /**
     * Determines if the supplied [number] can be safely converted to the type of
     * [anotherNumber] without loss of precision.
     */
    @JvmStatic
    public fun check(number: Number, anotherNumber: Number): Boolean {
        val unwrappedNumber = unwrap(number)
        val unwrappedAnotherNumber = unwrap(anotherNumber)
        for (caster in checkers) {
            if (caster.supports(unwrappedNumber)) {
                return caster.isConvertible(unwrappedAnotherNumber)
            }
        }
        return false
    }

    /**
     * Unwraps the actual value from the [ComparableNumber].
     */
    private fun unwrap(number: Number): Number =
        if (number is ComparableNumber) number.value() else number
}

/**
 * Allows determining which types a number of type [T] can be converted to.
 */
private interface ConversionChecker<T : Number> {

    /**
     * Determines if the supplied [number] can be safely converted to the type of the caster.
     */
    fun isConvertible(number: Number): Boolean {
        val numberClass = number.javaClass
        return convertibleTypes().any { it == numberClass }
    }

    /** Determines if the supplied [number] is supported by the caster. */
    fun supports(number: Number): Boolean = casterType().isInstance(number)

    /** Returns [T] type class instance. */
    fun casterType(): Class<T>

    /** Returns types which [T] type can be safely converted to. */
    fun convertibleTypes(): List<Class<out Number>>
}

private class ByteChecker : ConversionChecker<Byte> {
    override fun casterType(): Class<Byte> = Byte::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> = listOf(Byte::class.javaObjectType)
}

private class ShortChecker : ConversionChecker<Short> {
    override fun casterType(): Class<Short> = Short::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> =
        listOf(Byte::class.javaObjectType, Short::class.javaObjectType)
}

private class IntegerChecker : ConversionChecker<Int> {
    override fun casterType(): Class<Int> = Int::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> =
        listOf(Byte::class.javaObjectType, Short::class.javaObjectType, Int::class.javaObjectType)
}

private class LongChecker : ConversionChecker<Long> {
    override fun casterType(): Class<Long> = Long::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> = listOf(
        Byte::class.javaObjectType,
        Short::class.javaObjectType,
        Int::class.javaObjectType,
        Long::class.javaObjectType
    )
}

private class FloatChecker : ConversionChecker<Float> {
    override fun casterType(): Class<Float> = Float::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> = listOf(Float::class.javaObjectType)
}

private class DoubleChecker : ConversionChecker<Double> {
    override fun casterType(): Class<Double> = Double::class.javaObjectType
    override fun convertibleTypes(): List<Class<out Number>> =
        listOf(Float::class.javaObjectType, Double::class.javaObjectType)
}
