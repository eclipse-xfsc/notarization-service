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

import eu.gaiax.notarization.api.profile.NoFilter
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.request_processing.domain.exception.NotFoundException
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.domain.services.ProfileService
import io.quarkus.cache.Cache
import io.quarkus.cache.CacheName
import io.quarkus.cache.CacheResult
import io.quarkus.cache.CaffeineCache
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import mu.KotlinLogging
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class ProfileServiceClient(
    @param:RestClient private val profileClient: ProfileServiceRestClient,
    @CacheName(cacheProfileById) val cache: Cache,
) : ProfileService {
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = cacheProfileById)
    override fun find(id: ProfileId): Uni<Profile?> {
        val idValue = try {
            URLEncoder.encode(id.id, StandardCharsets.UTF_8)
        } catch (ex: UnsupportedEncodingException) {
            throw IllegalArgumentException(ex)
        }
        logger.debug { "Fetching profile by id $id" }
        return profileClient.fetchProfile(idValue)
            .onFailure(NotFoundException::class.java).recoverWithNull()
    }

    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = "paged-profiles")
    override fun list(index: Int?, size: Int?, sort: SortDirection?): Uni<PagedView<Profile, NoFilter>> {
        return profileClient.list(index = index, size = size, sort = sort)
            .invoke { page ->
                for (profile in page.items) {
                    cache.`as`(CaffeineCache::class.java).put(profile.id, CompletableFuture.completedFuture(profile))
                }
            }
    }

    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @CacheResult(cacheName = "profile-identifiers")
    override fun listProfileIdentifiers(): Uni<List<String>> {
        return profileClient.listProfileIdentifiers()
    }

    companion object {
        const val cacheProfileById = "profile-by-id"
    }
}
