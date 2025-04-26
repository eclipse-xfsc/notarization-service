/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.request_processing.domain.exception

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.SessionId

class InvalidRequestStateException : BusinessException {
    val id: SessionId
    val expected: Set<NotarizationRequestState>
    val actual: NotarizationRequestState?

    constructor(id: SessionId, expected: NotarizationRequestState, actual: NotarizationRequestState) : super(
        asMessage(id, expected, actual)
    ) {
        this.id = id
        this.expected = setOf(expected)
        this.actual = actual
    }

    constructor(id: SessionId, expected: Set<NotarizationRequestState>, actual: NotarizationRequestState?) : super(
        asMessage(id, expected, actual)
    ) {
        this.id = id
        this.expected = expected
        this.actual = actual
    }

    companion object {
        private fun asMessage(
            id: SessionId,
            expected: NotarizationRequestState,
            actual: NotarizationRequestState
        ): String {
            return String.format(
                "Expected session %s to have the state %s, but was %s",
                id.id,
                expected.toString(),
                actual.toString()
            )
        }

        private fun asMessage(
            id: SessionId, expected: Set<NotarizationRequestState>,
            actual: NotarizationRequestState?
        ): String {
            return String.format(
                "Expected session %s to have one of the states: %s, but was %s",
                id.id,
                expected.toString(),
                actual.toString()
            )
        }
    }
}
