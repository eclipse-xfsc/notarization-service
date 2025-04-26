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
package eu.gaiax.notarization.request_processing.infrastructure.rest.dto

import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.model.AccessToken
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
class SessionInfoResponse {
    @get:Schema(description = "The identifier of the created notarization request session.")
    var sessionId: SessionId? = null

    @get:Schema(description = "A new access token that provides access to the created notarization request session.")
    var token: AccessToken? = null

    companion object {
        fun valueOf(sessionInfo: Session): SessionInfoResponse {
            val session = SessionInfoResponse()
            session.sessionId = SessionId(sessionInfo.id!!)
            session.token = AccessToken(sessionInfo.accessToken)
            return session
        }
    }
}
