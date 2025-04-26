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
package eu.gaiax.notarization.request_processing.infrastructure.rest.mock

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import jakarta.json.Json
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.lang.JoseException
import java.io.StringReader
import java.time.Instant
import java.time.Period
import java.util.*


/**
 *
 * @author Neil Crossley
 */
class MockState {

    companion object {
        val mapper = jacksonObjectMapper()
        val refTypeListIssuanceAction: JavaType = mapper.constructType(object : TypeReference<List<IssuanceAction>>() {})

        val someProfileId: ProfileId = ProfileId("someProfile")
        val profileWithPreIssuanceActionsId: ProfileId = ProfileId("profileWithPreIssuanceActions")
        val augmentingProfileId: ProfileId = ProfileId("augmentingProfile")
        val profileId1: ProfileId = ProfileId("profile-01")
        val profileId2: ProfileId = ProfileId("profile-02")
        val profileId3: ProfileId = ProfileId("profile-03")
        val profileId4: ProfileId = ProfileId("profile-04")
        val profileId5: ProfileId = ProfileId("profile-05")
        val profileId5Row: ProfileId = ProfileId("profile-05")
        val profileWithOptionalPreConditionId: ProfileId = ProfileId("profile-with-opt-pre-con")
        val profileWithOnlyPreConditionId: ProfileId = ProfileId("profile-with-only-pre-con")
        @JvmField
        val someProfile: Profile
        val profileWithPreIssuanceActions: Profile
        val augmentingProfile: Profile
        @JvmField
        val profile1: Profile
        val profile2: Profile
        val profile3: Profile
        val profile4: Profile
        val profile5: Profile
        val profileWithOptionalPreCondition: Profile
        val profileWithOnlyPreCondition: Profile

        @JvmField
        val profileIds: Set<ProfileId> = setOf(
            someProfileId,
            augmentingProfileId,
            profileId1,
            profileId2,
            profileId3,
            profileId4,
            profileId5,
            profileWithOptionalPreConditionId,
            profileWithOnlyPreConditionId
        )
        @JvmField
        val notary1 = Notary(
            UUID.fromString("d6b8bd85-95c5-43d0-a93f-5bb034aef7e4"),
            "notary-01",
            setOf(someProfileId, profileId1),
            "notary-01-access-token-12345"
        )
        @JvmField
        val notary2 = Notary(
            UUID.fromString("653813be-5a71-47be-ba24-c3c838df4fdd"),
            "notary-02", setOf(someProfileId, profileId1, profileId2),
            "notary-02-access-token-23456"
        )
        @JvmField
        val notary3 = Notary(
            UUID.fromString("3aadbd54-9253-4018-8b62-890e9a8b46ff"),
            "notary-03", setOf(someProfileId, profileId1, profileId2, profileId3),
            "notary-03-access-token-34567"
        )
        @JvmField
        val notary4 = Notary(
            UUID.fromString("be4770ac-eba0-43cb-af0f-cf873885c446"),
            "notary-04", setOf(augmentingProfileId, profileId1, profileId2, profileId3, profileId4),
            "notary-04-access-token-45678"
        )
        val notary5 = Notary(
            UUID.fromString("4d63a11e-1e07-4251-abdc-df3092c52438"),
            "notary-05", setOf(profileId5Row),
            "notary-05-access-token-45"
        )
        @JvmField
        val notaries = setOf(
            notary1,
            notary2,
            notary3,
            notary4,
            notary5,
        )
        @JvmField
        val notariesWithSomeProfile = notaries.filter { it.roles.contains(someProfileId) }
        @JvmField
        val notariesBySubject = notaries.associateBy { it.subject }
        val profiles: Set<Profile>
        fun neededTasks_oneof_identiyandupload_identiyandotherupload(): ProfileTaskTree {
            return try {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                mapper.readValue<ProfileTaskTree>(
                    """
                    {"oneOf":[{"allOf":[{"taskName":"identify"},{"taskName":"upload"}]},{"allOf":[{"taskName":"identify"},{"taskName":"upload_other"}]}]}
                    """,
                    ProfileTaskTree::class.java
                )
            } catch (ex: JsonProcessingException) {
                throw IllegalArgumentException(ex)
            }
        }

        fun precondTasks_optional(): ProfileTaskTree {
            return try {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                mapper.readValue(
                    """
                        { "oneOf": [{"taskName":"identify"}, {}] }
                    """.trimIndent(),
                    ProfileTaskTree::class.java
                )
            } catch (ex: JsonProcessingException) {
                throw IllegalArgumentException(ex)
            }
        }

        fun precondTasks_identify(): ProfileTaskTree {
            return try {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                mapper.readValue(
                    """
                    {"taskName":"identify"}
                    """.trimIndent(),
                    ProfileTaskTree::class.java
                )
            } catch (ex: JsonProcessingException) {
                throw IllegalArgumentException(ex)
            }
        }

        fun neededTasks_oneOfTwoDocTasks(): ProfileTaskTree {
            return try {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                mapper.readValue(
                    """
                        {"oneOf":[{"taskName":"upload_sufficient"},{"taskName":"upload_combination_of"}]}
                    """.trimIndent(),
                    ProfileTaskTree::class.java
                )
            } catch (ex: JsonProcessingException) {
                throw IllegalArgumentException(ex)
            }
        }

        fun taskDescriptions(): List<TaskDescription> {
            val json = Json.createReader(
                StringReader(
                    """
                        [
                        {"name":"upload","type":"fileProvisionTask","description":"upload 3 files"},
                        {"name":"upload_other","type":"fileProvisionTask","description":"upload 3 files"},
                        {"name":"upload_sufficient","type":"fileProvisionTask","description":"upload 3 files"},
                        {"name":"upload_combination_of","type":"fileProvisionTask","description":"upload 3 files"},
                        {"name":"identify","type":"browserIdentificationTask","description":"identify yourself via browser"},
                        {"name":"vc_ident","type":"vcIdentificationTask","description":"identify yourself with vc"}
                        ]
                    """.trimIndent()
                )
            ).readArray()
            return json.map { it.asJsonObject() }.map {
                TaskDescription(
                    it.getString("name"),
                    TaskType.fromString(it.getString("type")),
                    it.getString("description"),
                )
            }
        }

        fun issuanceActions(): List<IssuanceAction> {
            return listOf(
                IssuanceAction(
                    name = "enrolInTrain", description = null, serviceName = "train", serviceLocation = null, encryptAtRest = null
                ),
                IssuanceAction(
                    name = "enrollment", description = null, serviceName = null, serviceLocation = null, encryptAtRest = null
                ),
                IssuanceAction(
                    name = "compliance", description = null, serviceName = null, serviceLocation = null, encryptAtRest = null
                ),
                IssuanceAction(
                    name = "checkVPCompliance", description = null, serviceName = "compliance", serviceLocation = null, encryptAtRest = null
                )
            )
        }
        fun actionsRequireMultiple(): ProfileTaskTree {
            val result = ProfileTaskTree()
            result.allOf = setOf(
                ProfileTaskTree("enrollment"),
                ProfileTaskTree("compliance")
            )
            return result
        }

        init {
            try {
                val someJwk: PublicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(
                    """
                {
                "kty": "EC",
                "d": "ssgbzdN5aYkKEFR6L3YSB9hpKfKuiiZxjsDf4y5r8Ck",
                "use": "enc",
                "crv": "P-256",
                "x": "MHOzeo5914c4RZvgTGNHoxdzjg2iziqCJ1y0D0_ONAs",
                "y": "c0k8MWQy3sQknEjOEnR0ZjgqQFWXPJ0clVzjruRT3fQ",
                "alg": "ECDH-ES"
                }
                """.trimIndent()
                )
                val someOtherJwk: PublicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(
                    """
                {"kty":"EC",
                 "d":"gHltlw4V2JKep2v4oRziohrYvU3cvRQ50T3-6WPf0B3d4_2yZ-AGf6sghQVoCPj-",
                 "crv":"P-384",
                 "x":"I9_0yRjMwcRLAhNBwWLPmII2BJK6pIPTqr0LRy0WkoLJiyA846iLUWmLuvM3P8-w",
                 "y":"rPaBxsFyHKPs-h5Pkp6zQ0y5ygdIjvDOc9PTPDqxZnmyBZngFlPNkcl29SSqKzAs"}
                """.trimIndent()
                )
                profile1 = Profile(
                    profileId1.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "First name",
                    "Some first description",
                    setOf(profileId1.id),
                    "A256GCM",
                    listOf(
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        )
                    ),
                    Period.ofYears(1),
                    false,
                    mapper.readTree(
                        """
                            {"first object":"other", "first field": 1}
                        """.trimIndent()
                    ),
                    """
                    { "credentialSubject": { "evidenceDocument": [ <documents:{doc|"<doc.sha256.base64>"}; separator=" ,">  ] } }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    ProfileTaskTree(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profile2 = Profile(
                    profileId2.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Second name",
                    "Some second description",
                    setOf(profileId2.id),
                    "A256GCM",
                    listOf(
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someOtherJwk
                        )
                    ),
                    Period.ofYears(1),
                    true,
                    mapper.readTree(
                        """
                    {"second object":"other", "second field": 1}
                """.trimIndent()
                    ),
                    """
                    { "credentialSubject": { "evidenceDocument": [ <documents:{doc|"<doc.sha256.base64>"}; separator=" ,">  ] } }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profile3 = Profile(
                    profileId3.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Third name",
                    "Some third description",
                    setOf(profileId3.id),
                    profile1.encryption,
                    profile1.notaries,
                    Period.ofMonths(7),
                    false,
                    mapper.readTree(
                        """
                        {"third-object":"other", "second field": 1}
                    """.trimIndent()
                    ),
                    """
                    { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profile4 = Profile(
                    profileId4.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Fourth name",
                    "Some fourth description",
                    setOf(profileId4.id),
                    profile2.encryption,
                    profile2.notaries,
                    Period.ofDays(5),
                    true,
                    mapper.readTree(
                        """
                        {"fourth-object":"other", "second field": 1}
                    """.trimIndent()
                    ),
                    """
                    { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profile5 = Profile(
                    profileId5.id,
                    AipVersion.V2_0,
                    CredentialKind.SD_JWT,
                    "Fifty name",
                    "Some fourth description",
                    notaryRoles = setOf(profileId5Row.id),
                    profile2.encryption,
                    profile2.notaries,
                    Period.ofDays(5),
                    true,
                    mapper.readTree(
                        """
                        {"fourth-object":"other", "second field": 1}
                    """.trimIndent()
                    ),
                    """
                    { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profileWithOptionalPreCondition = Profile(
                    profileWithOptionalPreConditionId.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Fourth name",
                    "Some fourth description",
                    setOf(profileWithOptionalPreConditionId.id),
                    profile2.encryption,
                    profile2.notaries,
                    Period.ofDays(5),
                    false,
                    mapper.readTree(
                        """
                        {"fourth-object":"other", "second field": 1}
                    """.trimIndent()
                    ),
                    """
                    { "evidenceDocument": "<documents:{doc|<doc.sha256.base64>}; separator=" ,">" }
                """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_optional(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profileWithOnlyPreCondition = Profile(
                    profileWithOnlyPreConditionId.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Profile-Only-Precondition",
                    "Some fourth description",
                    setOf(profileWithOnlyPreConditionId.id),
                    profile2.encryption,
                    listOf(),
                    Period.ofDays(5),
                    false,
                    mapper.readTree(
                        """
                        {"an-object":"stuff", "second field": 1}
                    """.trimIndent()
                    ),
                    "",
                    taskDescriptions(),
                    ProfileTaskTree(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf(),
                )
                someProfile = Profile(
                    someProfileId.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Some profile name",
                    "Some profile description",
                    setOf(someProfileId.id),
                    profile2.encryption,
                    listOf(
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        ),
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        ),
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        )
                    ),
                    Period.ofDays(155),
                    true,
                    mapper.readTree(
                        """
                        {"some profile field":"other", "second field": "something"}
                    """.trimIndent()
                    ),
                    null,
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                augmentingProfile = Profile(
                    augmentingProfileId.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Augmenting profile name",
                    "A profile that requires credential augmentation by the notary",
                    setOf(augmentingProfileId.id),
                    profile2.encryption,
                    listOf(
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        ),
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        ),
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        )
                    ),
                    Period.ofDays(155),
                    true,
                    mapper.readTree(
                        """
                        {"some profile field":"other", "second field": "something"}
                        """.trimIndent()
                    ),
                    """
                        { "givenData": <credentialAugmentation> }
                    """.trimIndent(),
                    taskDescriptions(),
                    neededTasks_oneOfTwoDocTasks(),
                    precondTasks_identify(),
                    ProfileTaskTree(),
                    listOf(),
                    listOf()
                )
                profileWithPreIssuanceActions = Profile(
                    profileWithPreIssuanceActionsId.id,
                    AipVersion.V2_0,
                    CredentialKind.JsonLD,
                    "Profile with issuance actions",
                    "A profile that requires a pre-issuance action",
                    notaryRoles = setOf(
                        profileWithPreIssuanceActionsId.id,
                        someProfileId.id
                    ),
                    profile2.encryption,
                    listOf(
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        ),
                        NotaryAccess(
                            "ECDH-ES+A256KW",
                            someJwk
                        )
                    ),
                    Period.ofDays(155),
                    true,
                    mapper.readTree(
                        """
                        {"some profile field":"other", "second field": "something"}
                        """.trimIndent()
                    ),
                    """
                        { "givenData": <credentialAugmentation> }
                     """.trimIndent(),
                    taskDescriptions = taskDescriptions(),
                    tasks = ProfileTaskTree(),
                    preconditionTasks = ProfileTaskTree(),
                    preIssuanceActions = actionsRequireMultiple(),
                    postIssuanceActions = listOf(),
                    actionDescriptions = issuanceActions()
                )
            } catch (ex: JoseException) {
                throw RuntimeException("Could not create JWT", ex)
            }
            profiles = setOf(
                profile1,
                profile2,
                profile3,
                profile4,
                profile5,
                someProfile,
                profileWithOptionalPreCondition,
                profileWithOnlyPreCondition,
                augmentingProfile,
                profileWithPreIssuanceActions,
            )
        }
    }

        class Notary(
            val subject: UUID,
            val name: String,
            val roles: Set<ProfileId>,
            val token: String
        ) {
            fun bearerValue(): String {
                return String.format("Bearer %s", this.token)
            }

            fun knownUnauthorizedRoles(): Set<ProfileId> {
                val unauthorizedRoles: HashSet<ProfileId> = HashSet<ProfileId>(profileIds)
                unauthorizedRoles.removeAll(this.roles)
                return unauthorizedRoles
            }

            fun introspectionToken(): String {
                val rolesString = roles.map { "\"${it.id}\"" }.joinToString(",")
                val spacedRoles = roles.joinToString(" ")
                return String.format(
                    """
                    {
                      "sub": "$subject",
                      "iss": "mocked-idp",
                      "username": "$name",
                      "roles": [
                        "default-roles-notarization-realm",
                        "offline_access",
                        "uma_authorization",
                        $rolesString
                      ],
                      "exp": ${Instant.now().plusSeconds(300).epochSecond},
                      "typ": "Bearer",
                      "scope": "email profile $spacedRoles",
                      "client_id": "portal-client",
                      "token_type": "bearer",
                      "active": true
                    }
                   """
                )
            }
        }
}
