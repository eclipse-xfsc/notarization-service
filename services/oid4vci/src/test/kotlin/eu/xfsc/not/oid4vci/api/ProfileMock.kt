package eu.xfsc.not.oid4vci.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.xfsc.not.oid4vci.ProfileProvider
import org.mockito.kotlin.whenever


object ProfileMock {
    fun setupDummyProfile(profileMock: ProfileProvider) {
        whenever(profileMock.getAllProfiles()).then {
            listOf(
                Profile(
                    id = "foo",
                    name = "foo Profile",
                    description = "Foo Profile of a virtual credential",
                    aip = AipVersion.V2_0,
                    kind = CredentialKind.JsonLD,
                    isRevocable = true,
                    notaries = listOf(),
                    encryption = "none",
                    preIssuanceActions = ProfileTaskTree(),
                    postIssuanceActions = listOf(),
                    preconditionTasks = ProfileTaskTree(),
                    taskDescriptions = listOf(),
                    actionDescriptions = listOf(),
                    tasks = ProfileTaskTree(),
                    template = jacksonObjectMapper().readTree("""
                        {
                          "@context": [
                            "https://www.w3.org/2018/credentials/v1",
                            "https://www.w3.org/2018/credentials/examples/v1"
                          ],
                          "type": ["VerifiableCredential", "AlumniCredential"],
                          "credentialSubject": {
                            "alumniOf": {
                              "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                              "name": "Mars University"
                            }
                          }
                        }
                    """.trimIndent()),
                    validFor = null,
                    documentTemplate = null,
                    notaryRoles = setOf(),
                ),
            )
        }
    }

    fun setupEmptyProfile(profileMock: ProfileProvider) {
        whenever(profileMock.getAllProfiles()).then {
            listOf<Profile>()
        }
    }

    fun setupDefectiveProfile(profileMock: ProfileProvider) {
        whenever(profileMock.getAllProfiles()).then {
            listOf(
                Profile(
                    id = "foo",
                    name = "foo Profile",
                    description = "Foo Profile of a virtual credential",
                    aip = AipVersion.V2_0,
                    kind = CredentialKind.JsonLD,
                    isRevocable = true,
                    notaries = listOf(),
                    encryption = "none",
                    preIssuanceActions = ProfileTaskTree(),
                    postIssuanceActions = listOf(),
                    preconditionTasks = ProfileTaskTree(),
                    taskDescriptions = listOf(),
                    actionDescriptions = listOf(),
                    tasks = ProfileTaskTree(),
                    template = jacksonObjectMapper().readTree("""
                        {
                          "context": [
                            "https://www.w3.org/2018/credentials/v1",
                            "https://www.w3.org/2018/credentials/examples/v1"
                          ],
                          "type": ["VerifiableCredential", "AlumniCredential"],
                          "credentialSubject": {
                            "alumniOf": {
                              "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                              "name": "Mars University"
                            }
                          }
                        }
                    """.trimIndent()),
                    validFor = null,
                    documentTemplate = null,
                    notaryRoles = setOf(),
                ),
            )
        }
    }

    fun setupAnonCredProfile(profileMock: ProfileProvider) {
        whenever(profileMock.getAllProfiles()).then {
            listOf(
                Profile(
                    id = "foo",
                    name = "foo Profile",
                    description = "Foo Profile of a virtual credential",
                    aip = AipVersion.V1_0,
                    kind = CredentialKind.AnonCred,
                    isRevocable = true,
                    notaries = listOf(),
                    encryption = "none",
                    preIssuanceActions = ProfileTaskTree(),
                    postIssuanceActions = listOf(),
                    preconditionTasks = ProfileTaskTree(),
                    taskDescriptions = listOf(),
                    actionDescriptions = listOf(),
                    tasks = ProfileTaskTree(),
                    template = jacksonObjectMapper().readTree("""
                        {
                        }
                    """.trimIndent()),
                    validFor = null,
                    documentTemplate = null,
                    notaryRoles = setOf(),
                ),
            )
        }
    }

}
