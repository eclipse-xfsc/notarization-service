package eu.gaiax.notarization.request_processing.management

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileTaskTree
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceClient
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.request_processing.infrastructure.stringtemplate.ProfileTemplateRenderer
import eu.gaiax.notarization.request_processing.matcher.WithJsonField.Companion.withJsonField
import eu.gaiax.notarization.request_processing.matcher.WithJsonField.Companion.withoutJsonField
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junitpioneer.jupiter.ReportEntry
import org.stringtemplate.v4.STGroupFile

private val logger = KotlinLogging.logger { }
private val mapper = jacksonObjectMapper()


class RenderCredentialTest {

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00032")
    fun canCreateSimpleJsonLdCredential() {

        val inputRequest = NotarizationRequest()
        val profile = someJsonLdProfile()

        val sut = ProfileTemplateRenderer()

        val result = sut.render(
            profile = profile,
            documentTemplate = profile.documentTemplate,
            setOf(),
            value = inputRequest
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00032")
    fun canCreateSimpleSdJwtCredential() {
        val inputRequest = NotarizationRequest()
        inputRequest.credentialAugmentation =
            """
                { "given_name": "Sarah" }
            """.trimIndent()
        val profile = someJsonLdProfile().copy(
            documentTemplate =
            """
                 { "givenName": <augmentation.given_name> }
                """.trimIndent()
        )

        val sut = ProfileTemplateRenderer()

        val result = sut.render(
            profile = profile,
            documentTemplate = profile.documentTemplate,
            setOf(),
            value = inputRequest
        )
        assertThat(asObjectNode(result), allOf(
            withJsonField("givenName", "Sarah")
        ))
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00032")
    fun canSupportDeepFields() {
        val inputRequest = NotarizationRequest()
        inputRequest.credentialAugmentation =
            """
                { "claims.claimmap.given_name": "Sarah" }
            """.trimIndent()
        val profile = someJsonLdProfile().copy(
            documentTemplate =
                """
                    { "givenName": <augmentation.claims.claimmap.given_name> }
                """.trimIndent()
        )
        val sut = ProfileTemplateRenderer()

        val result = sut.render(
            profile = profile,
            documentTemplate = profile.documentTemplate,
            setOf(),
            value = inputRequest
        )
        assertThat(asObjectNode(result), allOf(
            withJsonField("givenName", "Sarah")
        ))
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00032")
    fun canSupportConditions() {
        val inputRequest = NotarizationRequest()
        inputRequest.credentialAugmentation =
            """
                { "given_name": "Sarah", "hobby": "Reading" }
            """.trimIndent()
        val profile = someJsonLdProfile().copy(
            documentTemplate =
            """
                    { "givenName": <augmentation.given_name> <if(augmentation.family_name)>, "family_name": <augmentation.family_name><endif>,
                    <if(augmentation.hobby)>"hobby": <augmentation.hobby><endif> }
                """.trimIndent()
        )
        val sut = ProfileTemplateRenderer()

        val result = sut.render(
            profile = profile,
            documentTemplate = profile.documentTemplate,
            setOf(),
            value = inputRequest
        )
        assertThat(asObjectNode(result), allOf(
            withJsonField("givenName", "Sarah"),
            withJsonField("hobby", "Reading"),
            withoutJsonField("family_name")
        ))
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00032")
    fun canSupportIteration() {
        val inputRequest = NotarizationRequest()
        inputRequest.credentialAugmentation =
            """
                { "given_name": "Sarah", "hobby": "Reading" }
            """.trimIndent()
        val profile = someJsonLdProfile().copy(
            documentTemplate =
            """
                    { <["given_name", "hobby", "family_name"]:{ it |<if(augmentation.(it))>"<it>": <augmentation.(it)><endif>}; separator=","> }
                """.trimIndent()
        )
        val sut = ProfileTemplateRenderer()

        val result = sut.render(
            profile = profile,
            documentTemplate = profile.documentTemplate,
            setOf(),
            value = inputRequest
        )
        assertThat(asObjectNode(result), allOf(
            withJsonField("given_name", "Sarah"),
            withJsonField("hobby", "Reading"),
            withoutJsonField("family_name")
        ))
    }
    fun someJsonLdProfile(): Profile {

        return Profile(
            id = "some-profile-id",
            aip = AipVersion.V2_0,
            kind = CredentialKind.JsonLD,
            name = "some-profile-name",
            description = "description",
            notaryRoles = setOf(""),
            encryption = "invalid-value",
            notaries = listOf(),
            validFor = null,
            isRevocable = true,
            postIssuanceActions = listOf(),
            actionDescriptions = listOf(),
            preconditionTasks = ProfileTaskTree(),
            tasks = ProfileTaskTree(),
            documentTemplate = "",
            extensions = emptyMap(),
            preIssuanceActions = ProfileTaskTree(),
            taskDescriptions = listOf(),
            template = mapper.readTree("{}")
        )
    }
    fun someSdJwtProfile(): Profile {

        return Profile(
            id = "some-sd-jwt-profile-id",
            aip = null,
            kind = CredentialKind.SD_JWT,
            name = "some-sd-jwt-profile-name",
            description = "description",
            notaryRoles = setOf(""),
            encryption = "invalid-value",
            notaries = listOf(),
            validFor = null,
            isRevocable = true,
            postIssuanceActions = listOf(),
            actionDescriptions = listOf(),
            preconditionTasks = ProfileTaskTree(),
            tasks = ProfileTaskTree(),
            documentTemplate = null,
            extensions = emptyMap(),
            preIssuanceActions = ProfileTaskTree(),
            taskDescriptions = listOf(),
            template = mapper.readTree("{}")
        )
    }
    fun asObjectNode(value: String): ObjectNode {
        mapper.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
        logger.warn { "Wrapping value: \n$value" }
        return mapper.readValue(value, ObjectNode::class.java)
    }
}
