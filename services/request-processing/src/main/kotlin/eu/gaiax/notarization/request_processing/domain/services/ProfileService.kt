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

import eu.gaiax.notarization.api.profile.NoFilter
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.query.PagedView
import eu.gaiax.notarization.api.query.SortDirection
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import io.smallrye.mutiny.Uni

/**
 *
 * @author Neil Crossley
 */
interface ProfileService {
    fun find(id: ProfileId): Uni<Profile?>
    fun list(
        index: Int?,
        size: Int?,
        sort: SortDirection?
    ): Uni<PagedView<Profile, NoFilter>>

    fun listProfileIdentifiers(): Uni<List<String>>
}
