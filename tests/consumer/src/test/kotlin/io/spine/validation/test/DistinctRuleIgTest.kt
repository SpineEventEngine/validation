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

package io.spine.validation.test

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`(distinct)` rule should")
internal class DistinctRuleIgTest {

    @Test
    fun `reject a list with duplicate entries`() {
        val flake = snowflake {
            edges = 6
            vertices = 6
        }
        val builder = Blizzard.newBuilder()
            .addSnowflake(flake)
            .addSnowflake(flake)

        assertValidationException(builder)
    }

    @Test
    fun `reject a map with duplicate entries`() {
        val player = player {
            shirtName = "John Doe"
        }
        val builder = Team.newBuilder()
            .putPlayers(7, player)
            .putPlayers(10, player)
        assertValidationException(builder)
    }
}
