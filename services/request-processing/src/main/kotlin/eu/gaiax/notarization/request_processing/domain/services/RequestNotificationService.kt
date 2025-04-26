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
package eu.gaiax.notarization.request_processing.domain.services

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import java.net.URI

interface RequestNotificationService {
    fun onReadyForReview(request: NotarizationRequestId?, profile: ProfileId?)
    fun onRejected(identifier: SessionId?)
    fun onPendingDid(identifier: SessionId?)
    fun onPreAccepted(identifier: SessionId?)
    fun onAccepted(identifier: SessionId)
    fun onAccepted(identifier: SessionId, invite: String, discriminator: String)
    fun onDeleted(identifier: SessionId?)
    fun onTerminated(identifier: SessionId?)
    fun onExternalTask(identifier: SessionId, redirect: URI?)
    fun onExternalAction(identifier: SessionId, redirect: URI?)
    fun onContactUpdate(identifier: SessionId?, contact: String?)
}
