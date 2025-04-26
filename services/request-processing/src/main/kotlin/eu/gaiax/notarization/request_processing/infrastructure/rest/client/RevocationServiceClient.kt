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
package eu.gaiax.notarization.request_processing.infrastructure.rest.client

import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.services.RevocationService
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.ListMapping
import io.quarkus.cache.CacheResult
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
class RevocationServiceClient(
    @param:RestClient private val revocationClient: RevocationServiceRestClient
) : RevocationService {
    @get:CacheResult(cacheName = "listcred-profile-mapping-list")
    override val list: Uni<List<ListMapping>>
        get() = revocationClient.list

    override fun revoke(profileName: String, idx: Long): Uni<Void> {
        return revocationClient.revoke(profileName, idx)
    }

    @CacheResult(cacheName = "listcred-profile-mapping-name-resolved")
    override fun getProfileName(listName: String): Uni<String> {
        return revocationClient.list
            .onItem().transform { l ->
                l.stream()
                    .filter { e: ListMapping -> e.listName == listName }
                    .map { m: ListMapping -> m.profileName }
                    .findFirst()
                    .orElseThrow { NotFoundException("Associated profile") }
            }
    }
}
