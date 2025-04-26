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
package eu.gaiax.notarization.request_processing.domain.entity

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@Entity
@Immutable
@Table(name = "notarizationrequest_view")
class NotarizationRequestDbView: PanacheEntityBase {

    companion object : PanacheCompanionBase<NotarizationRequestDbView, String> {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "session_id", updatable = false, nullable = false)
    var sessionId: String? = null
    var did: String? = null
    var profileId: String? = null
    var state: NotarizationRequestState? = null

    @Basic
    @Column(name = "request_id", insertable = false, updatable = false, unique = true)
    var requestId: UUID? = null
    var rejectComment: String? = null

    @Convert(converter = StringConverter::class)
    var data: String? = null
    var claimedBy: String? = null

    @Column(name = "total_documents", insertable = false, updatable = false, unique = true)
    var totalDocuments: Int? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    var lastModified: OffsetDateTime? = null
}
