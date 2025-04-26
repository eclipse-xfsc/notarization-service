package eu.xfsc.not.ssi_issuance2.mock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.xfsc.not.ssi_issuance2.domain.ProfileProvider
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever


object ProfileMock {
    fun setUpFetchDids(profileMock: ProfileProvider){
        whenever(profileMock.fetchDids(anyString())).then{
            ProfileIssuanceSpec(
                issuingDid = "${TestHelper.ISSUER_DID}",
                revocatingDid = "${TestHelper.ISSUER_DID}",
                signatureType = SignatureType.ED25519SIGNATURE2020,
            )
        }
    }
    fun setUpFetchJsonLDProfile(profileMock: ProfileProvider) {
        whenever(profileMock.fetchProfile(anyString())).then {
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
                    "https://w3id.org/citizenship/v1",
                    "https://w3id.org/vc/status-list/2021/v1"
                  ],
                  "type": ["VerifiableCredential", "PermanentResidentCard"],
                  "credentialSubject": {
                    "type": "PermanentResident"
                  }
                }
                """.trimIndent()),
                validFor = null,
                documentTemplate = null,
                notaryRoles = setOf(),
            )
        }
    }

    fun setUpFetchAnonCredProfile(profileMock: ProfileProvider) {
        whenever(profileMock.fetchProfile(anyString())).then {
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
                template = jacksonObjectMapper().readTree(""" { } """.trimIndent()),
                validFor = null,
                documentTemplate = null,
                notaryRoles = setOf(),
            )
        }
    }


}
