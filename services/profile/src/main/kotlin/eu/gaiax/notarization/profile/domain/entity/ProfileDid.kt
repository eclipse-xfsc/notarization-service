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
package eu.gaiax.notarization.profile.domain.entity

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.profile.domain.exception.UnknownProfileException
import io.quarkiverse.hibernate.types.json.JsonNodeStringType
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import io.quarkiverse.hibernate.types.json.JsonType
import io.quarkiverse.hibernate.types.json.JsonTypes
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni

import io.smallrye.mutiny.coroutines.awaitSuspending
import io.vertx.core.json.JsonObject
import jakarta.persistence.*
import jakarta.transaction.Transactional
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@Entity
class ProfileDid : PanacheEntityBase {
    @Id
    var id: UUID? = null

    @Column(unique = true)
    var profileId: String? = null

    @Type(value = JsonNodeStringType::class)
    @Column(columnDefinition = JsonTypes.JSON)
    var issuanceContent: JsonNode? = null

    @Column(name = "revocation_list_created_at")
    var revocationListCreatedAt: Instant? = null

    @Column(name = "issuance_version")
    @Enumerated(EnumType.STRING)
    var issuanceVersion: ApiVersion? = ApiVersion.V1

    @Enumerated(EnumType.STRING)
    var keyType: KeyType? = null

    @Enumerated(EnumType.STRING)
    var signatureType: SignatureType? = null

    fun setDefaults(issuanceVersion: ApiVersion, kind: CredentialKind) {
        this.issuanceVersion = issuanceVersion
        when(issuanceVersion) {
            ApiVersion.V1 -> {
                when(kind) {
                    CredentialKind.SD_JWT -> {
                        throw IllegalArgumentException("SD-JWT is not supported")
                    }
                    CredentialKind.JsonLD, CredentialKind.AnonCred -> {
                    }
                }
            }
            ApiVersion.V2 -> {
                when(kind) {
                    CredentialKind.SD_JWT -> {
                        keyType = KeyType.ED25519
                        signatureType = SignatureType.ED25519SIGNATURE2018
                    }
                    CredentialKind.JsonLD -> {
                        keyType = KeyType.ED25519
                        signatureType = SignatureType.ED25519SIGNATURE2018
                    }
                    CredentialKind.AnonCred -> {
                        throw IllegalArgumentException("AnonCred is not supported")

                    }
                }
            }
        }

    }

    companion object : PanacheCompanionBase<ProfileDid, UUID> {
        @WithTransaction
        fun findByProfileId(profileId: String): Uni<List<ProfileDid>> {
            return ProfileDid.find("profileId", profileId).list()
                .onFailure(NoResultException::class.java)
                .transform { t -> UnknownProfileException(profileId, t) }
        }
        @WithTransaction
        fun findByProfileIdAndVersion(profileId: String, issuanceVersion: ApiVersion): Uni<ProfileDid?> {
            return ProfileDid.find("profileId = ?1 and issuanceVersion = ?2", profileId, issuanceVersion).firstResult()
                .onFailure(NoResultException::class.java)
                .transform { t -> UnknownProfileException(profileId, t) }
        }
        @WithTransaction
        fun findByProfileIdOptionally(profileId: String): Uni<List<ProfileDid>> {
            return ProfileDid.find("profileId", profileId).list()
        }
    }
}
