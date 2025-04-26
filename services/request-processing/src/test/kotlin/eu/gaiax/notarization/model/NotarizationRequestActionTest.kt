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
package eu.gaiax.notarization.model

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import java.util.Set

/**
 *
 * @author Neil Crossley
 */
class NotarizationRequestActionTest {
    private val allActions = setOf(*NotarizationRequestAction.values())
    @Test
    fun allActionsBelongToAnActor() {
        val sumOfAllActors = HashSet<NotarizationRequestAction>()
        sumOfAllActors.addAll(NotarizationRequestAction.RequestorActions)
        sumOfAllActors.addAll(NotarizationRequestAction.NotaryActions)
        sumOfAllActors.addAll(NotarizationRequestAction.SystemActions)
        sumOfAllActors.addAll(NotarizationRequestAction.CallbackActions)
        MatcherAssert.assertThat(sumOfAllActors, CoreMatchers.equalTo(allActions))
    }

    @Test
    fun requestorActionsAndNotaryActionsAreDisjunct() {
        val intersection = HashSet(NotarizationRequestAction.RequestorActions)
        intersection.retainAll(NotarizationRequestAction.NotaryActions)
        MatcherAssert.assertThat(intersection, CoreMatchers.equalTo(Set.of<Any>()))
    }

    @Test
    fun requestorActionsAndSystemActionsAreDisjunct() {
        val intersection = HashSet(NotarizationRequestAction.RequestorActions)
        intersection.retainAll(NotarizationRequestAction.SystemActions)
        MatcherAssert.assertThat(intersection, CoreMatchers.equalTo(Set.of<Any>()))
    }

    @Test
    fun notaryActionsAndSystemActionsAreDisjunct() {
        val intersection = HashSet(NotarizationRequestAction.NotaryActions)
        intersection.retainAll(NotarizationRequestAction.SystemActions)
        MatcherAssert.assertThat(intersection, CoreMatchers.equalTo(Set.of<Any>()))
    }

    @Test
    fun testFailure() {
        val intersection = HashSet(NotarizationRequestAction.NotaryActions)
        intersection.retainAll(NotarizationRequestAction.SystemActions)
        MatcherAssert.assertThat(intersection, CoreMatchers.equalTo(Set.of<Any>()))
    }
}
