package eu.xfsc.not.ssi_issuance2.ldp

import com.fasterxml.jackson.databind.node.TextNode
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.xfsc.not.ssi_issuance2.application.LdpVcVersion
import eu.xfsc.not.ssi_issuance2.application.LdpW3CCredentialBuilder
import eu.xfsc.not.ssi_issuance2.json.WithJsonField.Companion.withJsonField
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.time.Instant
import java.time.Period
import java.util.UUID


@QuarkusTest
class LdpW3CCredentialBuilderTest {

    @Inject
    lateinit var sut: LdpW3CCredentialBuilder

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canParseV1ExampleCredential() {
        val inputDid = someDid()
        val issuanceDate = Instant.now()
        val holderDid = someDid()

        val (result, version) = sut.build(
            someProfile(),
            issuerDid = inputDid,
            issuanceDate,
            LdpVcExamples.V1.example1,
            holderDid
        )

        assertThat(result, notNullValue())
        assertThat(version, `is`(LdpVcVersion.W3CV1))
        assertThat(result, allOf(
            withJsonField("issuer", inputDid),
            withJsonField("issuanceDate", issuanceDate.toString()),
            withJsonField("credentialSubject", withJsonField("id", holderDid))
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canTolerateStringContextInV1() {
        val inputDid = someDid()
        val issuanceDate = Instant.now()
        val inputCredential = LdpVcExamples.V1.example1
        inputCredential.replace("@context", TextNode.valueOf(LdpVcVersion.W3CV1.schema))
        val holderDid = someDid()

        val (result, version) = sut.build(
            someProfile(),
            issuerDid = inputDid,
            issuanceDate,
            inputCredential,
            holderDid
        )

        assertThat(result, notNullValue())
        assertThat(version, `is`(LdpVcVersion.W3CV1))
        assertThat(result, allOf(
            withJsonField("issuer", inputDid),
            withJsonField("issuanceDate", issuanceDate.toString()),
            withJsonField("credentialSubject", withJsonField("id", holderDid))
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canParseV2ExampleCredential() {
        val inputDid = someDid()
        val issuanceDate = Instant.now()
        val holderDid = someDid()

        val (result, version) = sut.build(
            someProfile(),
            issuerDid = inputDid,
            issuanceDate,
            LdpVcExamples.V2.example1,
            holderDid
        )

        assertThat(result, notNullValue())
        assertThat(version, `is`(LdpVcVersion.W3CV2))
        assertThat(result, allOf(
            withJsonField("issuer", inputDid),
            withJsonField("validFrom", issuanceDate.toString()),
            withJsonField("credentialSubject", withJsonField("id", holderDid))
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun canTolerateStringContextInV2() {
        val inputDid = someDid()
        val issuanceDate = Instant.now()
        val inputCredential = LdpVcExamples.V1.example1
        inputCredential.replace("@context", TextNode.valueOf(LdpVcVersion.W3CV2.schema))
        val holderDid = someDid()

        val (result, version) = sut.build(
            someProfile(),
            issuerDid = inputDid,
            issuanceDate,
            inputCredential,
            holderDid
        )

        assertThat(result, notNullValue())
        assertThat(version, `is`(LdpVcVersion.W3CV2))
        assertThat(result, allOf(
            withJsonField("issuer", inputDid),
            withJsonField("validFrom", issuanceDate.toString()),
            withJsonField("credentialSubject", withJsonField("id", holderDid))
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
}
