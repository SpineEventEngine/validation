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

package io.spine.tools.validation

import io.spine.tools.compiler.ast.Field

/**
 * A common interface for the views of fields marked with a validation option.
 */
public interface FieldView {

    /**
     * The field in which the option was discovered.
     */
    public val subject: Field
}

/**
 * Sorts these views by the order of declaration of their fields.
 *
 * Querying returns views in no particular order, and the order changes from run
 * to run. Sort the views before generating code for them, so that the generated
 * code stays the same from build to build.
 *
 * The views of the fields of one message go in the order in which the fields
 * are declared in the message.
 */
public fun <V : FieldView> Iterable<V>.sortedByDeclaration(): List<V> =
    sortedBy { it.subject.orderOfDeclaration }
