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

import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.smallrye.mutiny.Uni
import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.reactive.mutiny.Mutiny
import java.time.OffsetDateTime
import java.util.*

private val logger = KotlinLogging.logger { }

/**
 *
 * @author Neil Crossley
 */
@Entity
@Table(name = "notarizationrequest")
class NotarizationRequest : PanacheEntityBase {

    @Id
    var id: UUID? = null

    var did: String? = null

    var requestorInvitationUrl: String? = null

    @OneToOne
    @JoinColumn(name = "session_id", unique = true)
    var session: Session? = null

    //Needed due to not working query builder
    @Basic
    @Column(insertable = false, updatable = false, unique = true)
    var session_id: String? = null

    var rejectComment: String? = null

    @Convert(converter = StringConverter::class)
    var data: String? = null

    // TODO: try to use ObjectNode with attribute converter
    //    @JdbcTypeCode(SqlTypes.JSON)
    //    @Type(GenericJsonObject.class)
    var credentialAugmentation: String? = null

    var claimedBy: String? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    var lastModified: OffsetDateTime? = null
    fun hasHolderAccess(profile: Profile): Boolean {
        return if (profile.aip == AipVersion.V1_0) {
            true
        } else did != null && !did!!.isBlank()
    }

    fun loadDocuments(): Uni<NotarizationRequest> {
        return Mutiny.fetch(
            session!!.documents
        ).onItem().transform { d: Set<Document?>? -> this }
    }

    fun loadCredentialAugmentation(): Uni<NotarizationRequest> {
        return Mutiny.fetch(credentialAugmentation).onItem().transform { this }
    }

    companion object: PanacheCompanionBase<NotarizationRequest, UUID> {
        @JvmStatic
        fun findById(id: NotarizationRequestId): Uni<NotarizationRequest?> {
            return findById(id.id)
        }
    }
}
