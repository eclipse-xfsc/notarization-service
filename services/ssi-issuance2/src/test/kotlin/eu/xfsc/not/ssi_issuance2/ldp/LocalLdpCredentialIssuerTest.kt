package eu.xfsc.not.ssi_issuance2.ldp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.xfsc.not.ssi_issuance2.application.DbKeyManager
import eu.xfsc.not.ssi_issuance2.application.LocalLdpCredentialIssuer
import eu.xfsc.not.ssi_issuance2.json.WithJsonField.Companion.matchesJsonText
import eu.xfsc.not.ssi_issuance2.json.WithJsonField.Companion.withJsonField
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junitpioneer.jupiter.ReportEntry
import java.time.Instant
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Stream

private val logger = KotlinLogging.logger { }

@QuarkusTest
class LocalLdpCredentialIssuerTest {

    @Inject
    lateinit var sut: LocalLdpCredentialIssuer

    @Inject
    lateinit var dbKeyManager: DbKeyManager

    @ParameterizedTest
    @MethodSource("supportedKeyTypes")
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canSignCredentialV1JsonWebSignature(keyType: KeyType) {
        val signatureType = SignatureType.JSONWEBSIGNATURE2020
        val inputDid = dbKeyManager.initKeypair(keyType)
        val issuanceDate = Instant.now().minus(2, ChronoUnit.DAYS)
        val holderDid = someDid()

        val result = sut.createCredential(
            profile = someProfile(),
            signatureType = signatureType,
            issuerDid = inputDid,
            subjectDid = holderDid,
            credentialData = LdpVcExamples.V1.example1,
            issuanceTimestamp = issuanceDate
        )

        assertThat(result, notNullValue())

        logger.info { result.toPrettyString() }

        assertThat(result, withJsonField("proof",
            allOf(
                withJsonField("proofPurpose", "assertionMethod"),
                withJsonField("type", signatureType.value),
                matchesJsonText("verificationMethod", startsWith(inputDid))
            )
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canSignCredentialV2JsonWebSignature() {
        val signatureType = SignatureType.JSONWEBSIGNATURE2020
        val inputDid = dbKeyManager.initKeypair(KeyType.ED25519)
        val issuanceDate = Instant.now().minus(2, ChronoUnit.DAYS)
        val holderDid = someDid()

        val result = sut.createCredential(
            profile = someProfile(),
            signatureType = signatureType,
            issuerDid = inputDid,
            subjectDid = holderDid,
            credentialData = LdpVcExamples.V2.example1,
            issuanceTimestamp = issuanceDate
        )

        assertThat(result, notNullValue())

        assertThat(result, withJsonField("proof",
            allOf(
                withJsonField("proofPurpose", "assertionMethod"),
                withJsonField("type", signatureType.value),
                matchesJsonText("verificationMethod", startsWith(inputDid))
            )
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canSignCredentialV1ED25519SIGNATURE2020() {
        val signatureType = SignatureType.ED25519SIGNATURE2020
        val inputDid = dbKeyManager.initKeypair(KeyType.ED25519)
        val issuanceDate = Instant.now().minus(2, ChronoUnit.DAYS)
        val holderDid = someDid()

        val result = sut.createCredential(
            profile = someProfile(),
            signatureType = signatureType,
            issuerDid = inputDid,
            subjectDid = holderDid,
            credentialData = LdpVcExamples.V1.example1,
            issuanceTimestamp = issuanceDate
        )

        assertThat(result, notNullValue())

        assertThat(result, withJsonField("proof",
            allOf(
                withJsonField("proofPurpose", "assertionMethod"),
                withJsonField("type", signatureType.value),
                matchesJsonText("verificationMethod", startsWith(inputDid))
            )
        ))
    }

    @ParameterizedTest
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    @MethodSource("algorithmsSignatureTypes")
    @Disabled(value = "This test includes unsupported combinations. Disabling to reduce test noise.")
    fun canRunEveryValueTogether(inputs: Pair<KeyType, SignatureType>) {
        logger.debug { "Running with values: ${inputs.first} and ${inputs.second}" }
        val signatureType = inputs.second
        val inputDid = dbKeyManager.initKeypair(inputs.first)
        val issuanceDate = Instant.now().minus(2, ChronoUnit.DAYS)
        val holderDid = someDid()

        val result = sut.createCredential(
            profile = someProfile(),
            signatureType = signatureType,
            issuerDid = inputDid,
            subjectDid = holderDid,
            credentialData = LdpVcExamples.V1.example1,
            issuanceTimestamp = issuanceDate
        )

        assertThat(result, notNullValue())

        assertThat(result, withJsonField("proof",
            allOf(
                withJsonField("proofPurpose", "assertionMethod"),
                withJsonField("type", signatureType.value),
                matchesJsonText("verificationMethod", startsWith(inputDid))
            )
        ))
    }

    fun someProfile(): Profile {
        return Profile(
            id = UUID.randomUUID().toString(),
            aip = null,
            CredentialKind.JsonLD,
            name = "Profile name ${UUID.randomUUID()}",
            description = "Profile description ${UUID.randomUUID()}",
            notaryRoles = setOf(),
            encryption = "dummy encryption key",
            notaries = listOf(),
            validFor = Period.ofYears(1),
            isRevocable = true,
            extensions = mapOf(),
            template = TextNode.valueOf("example"),
            taskDescriptions = listOf(),
            preIssuanceActions = ProfileTaskTree(null),
            documentTemplate = null,
            tasks = ProfileTaskTree(null),
            preconditionTasks = ProfileTaskTree(null),
            actionDescriptions = listOf(),
            postIssuanceActions = listOf()
        )
    }
    fun someDid(): String {
        return "did:some:${UUID.randomUUID()}"
    }

    companion object {
        @JvmStatic
        fun algorithmsSignatureTypes(): Stream<Pair<KeyType, SignatureType>> {
            val results = mutableListOf<Pair<KeyType, SignatureType>>()
            val credentialSummary = LocalLdpCredentialIssuer.credentialMechanisms
            for (keyType in credentialSummary.keyTypes) {
                for (signatureType in credentialSummary.signatureTypes) {
                    results.add(keyType to signatureType)
                }
            }
            return results.stream()
        }
        @JvmStatic
        fun supportedKeyTypes(): Stream<KeyType> {
            val results = setOf(
                KeyType.RSA,
                KeyType.P_256,
                KeyType.SECP256k1,
                KeyType.ED25519
            )
            return results.stream()
        }
    }
}
