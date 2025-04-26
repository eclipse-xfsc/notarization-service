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
package eu.gaiax.notarization.profile.infrastructure.rest.client

import eu.gaiax.notarization.api.issuance.Issuance2ApiAsync
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.ServiceInitRequest
import eu.gaiax.notarization.api.issuance.SignatureType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

/**
 *
 * @author Neil Crossley
 */
@RegisterRestClient(configKey = "ssi-issuance-api-v2")
interface SsiIssuanceV2HttpClient : Issuance2ApiAsync {
    companion object {
        fun requestFrom(profileId: String, keyType: KeyType?, signatureType: SignatureType?): ServiceInitRequest {
            val result = ServiceInitRequest(
                profileId = profileId,
                keyType = keyType ?: KeyType.ED25519,
                signatureType = signatureType ?: SignatureType.ED25519SIGNATURE2018,
            )
            return result
        }
    }
}
