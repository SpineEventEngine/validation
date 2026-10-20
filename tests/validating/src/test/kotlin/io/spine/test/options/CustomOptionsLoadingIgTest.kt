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

package io.spine.test.options

import com.google.protobuf.Extension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.spine.option.OptionsProto
import io.spine.option.OptionsProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Options should be registered with `ExtensionRegistry` so that it")
internal class CustomOptionsLoadingIgTest {

    private val registry by lazy {
        OptionsProvider.registryWithAllOptions()
    }

    @Test
    fun `contains standard options`() {
        assertContains(OptionsProto.required)
    }

    @Test
    fun `contains custom options`() {
        assertContains(BytesDirectionOptionProto.direction)
    }

    private fun assertContains(option: Extension<*, *>) {
        val descriptor = option.descriptor
        val name = descriptor.fullName
        val registeredExtension = registry.findImmutableExtensionByName(name)

        registeredExtension shouldNotBe null
        registeredExtension.descriptor shouldBe descriptor
    }
}
