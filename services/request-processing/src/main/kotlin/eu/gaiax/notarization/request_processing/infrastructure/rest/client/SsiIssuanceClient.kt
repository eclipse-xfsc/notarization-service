package eu.gaiax.notarization.request_processing.infrastructure.rest.client

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.ApiVersion
import eu.gaiax.notarization.api.issuance.Issuance2Api
import eu.gaiax.notarization.api.issuance.Issuance2ApiAsync
import eu.gaiax.notarization.api.issuance.IssuanceInitRequest
import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import eu.gaiax.notarization.request_processing.domain.services.Interop
import eu.gaiax.notarization.request_processing.domain.services.IssuanceService
import eu.gaiax.notarization.request_processing.domain.services.IssuementProcessSummary
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import eu.gaiax.notarization.request_processing.infrastructure.rest.client.SsiIssuanceRestClient.IssuanceRequest
import eu.gaiax.notarization.request_processing.infrastructure.stringtemplate.ProfileTemplateRenderer
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.logging.Logger
import java.lang.UnsupportedOperationException
import java.net.URI
import java.security.SecureRandom
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class SsiIssuanceClient(
    val issuanceClient: Optional<SsiIssuanceRestClient>,
    val issuanceClient2: Optional<Issuance2ApiAsync>,
    val apiVersions: Set<ApiVersion>,
    val profileTemplateRenderer: ProfileTemplateRenderer,
    val mapper: ObjectMapper,
    val logger: Logger,
    @ConfigProperty(name = "notarization-processing.internal-url")
    var internalUrl: URI
): IssuanceService {
    lateinit var secureRandom: SecureRandom
    fun onStartup(@Observes ev: StartupEvent?) {
        secureRandom = SecureRandom()
    }

    override fun issue(request: NotarizationRequest, profile: Profile): Uni<List<IssuementProcessSummary>> {
        return request.loadDocuments()
            .flatMap { r: NotarizationRequest -> r.loadCredentialAugmentation() }
            .map { r: NotarizationRequest -> r.session!!.documents }
            .flatMap { docs: Set<Document>? ->

                Multi.createFrom().iterable(apiVersions.filter { it.supportsCredentialKind(profile.kind) })
                    .onItem().transformToUniAndConcatenate { apiVersion ->

                        val succesNoncedUrl = createSuccessURI()
                        val failedNoncedUrl = createFailURI()

                        if (apiVersion == ApiVersion.V1) {
                            val issueRequest = IssuanceRequest()
                            issueRequest.profileID = request.session!!.profileId!!
                            issueRequest.holderDID = request.did
                            issueRequest.invitationURL = request.requestorInvitationUrl
                            issueRequest.issuanceTimestamp = Instant.now()
                            issueRequest.successURL = succesNoncedUrl.uri.toString()
                            issueRequest.failureURL = failedNoncedUrl.uri.toString()
                            issueRequest.credentialData = determineCredentialData(request, profile, docs ?: setOf())

                            issuanceClient.get().sendIssuanceRequest(issueRequest)
                                .map { response ->
                                    IssuementProcessSummary(
                                        apiVersion = apiVersion,
                                        successToken = succesNoncedUrl.nonce,
                                        failToken =  failedNoncedUrl.nonce,
                                        successUri = succesNoncedUrl.uri.toString(),
                                        failUri = failedNoncedUrl.uri.toString(),
                                        ssiInvitationUrl = response.invitationURL?.toString(),
                                        cancelUri = null
                                    )
                                }
                        } else if (apiVersion == ApiVersion.V2) {
                            val credentialCandidate = mapper.readerForUpdating(determineCredentialData(request, profile, docs ?: setOf()))
                                .readValue<JsonNode>(profile.template)

                            val issueRequest = IssuanceInitRequest(
                                request.session!!.profileId!!.id,
                                credentialCandidate,
                                Instant.now(),
                                request.did,
                                request.requestorInvitationUrl?.let { URI.create(it) },
                                succesNoncedUrl.uri,
                                failedNoncedUrl.uri
                            )

                            issuanceClient2.get().initSession(issueRequest)
                                .map { response ->
                                    IssuementProcessSummary(
                                        apiVersion = apiVersion,
                                        successToken = succesNoncedUrl.nonce,
                                        failToken =  failedNoncedUrl.nonce,
                                        successUri = succesNoncedUrl.uri.toString(),
                                        failUri = failedNoncedUrl.uri.toString(),
                                        ssiInvitationUrl = response.offerUrl?.toString(),
                                        cancelUri = response.cancelUrl
                                    )
                                }
                        } else {
                            throw UnsupportedOperationException()
                        }
                    }.collect().asList()
            }
    }

    private fun createNonce(): String {
        return Interop.urlSafeString(secureRandom, ByteArray(64))
    }

    inner class NoncedUri(
        val uri: URI, val nonce: String
    )

    private fun createSuccessURI(): NoncedUri {
        val nonce = createNonce()
        return NoncedUri(
            UriBuilder.fromUri(internalUrl)
                .path(Api.Path.V1_PREFIX)
                .path(Api.Path.FINISH_NOTARIZATION_REQUEST)
                .path(nonce)
                .path(Api.Path.SUCCESS)
                .build(),
            nonce
        )
    }

    private fun createFailURI(): NoncedUri {
        val nonce = createNonce()
        return NoncedUri(
            UriBuilder.fromUri(internalUrl)
                .path(Api.Path.V1_PREFIX)
                .path(Api.Path.FINISH_NOTARIZATION_REQUEST)
                .path(nonce)
                .path(Api.Path.FAIL)
                .build(),
            nonce
        )
    }

    fun determineCredentialData(value: NotarizationRequest,
                                profile: Profile,
                                docs: Set<Document>): JsonNode {
        var requestorData: JsonNode;
        try {
            requestorData = if (value.data != null) { mapper.readTree(value.data) } else mapper.createObjectNode()
        } catch (ex: JsonProcessingException) {
            logger.errorv("An unexpected error occurred while parsing the data for the request {0}", value.id, ex);
            throw IllegalArgumentException(ex);
        }
        if (profile.kind == CredentialKind.JsonLD) {
            if (requestorData is ObjectNode) {
                requestorData.put("id", value.did);
            }
            val wrapper = mapper.createObjectNode();
            wrapper.set<ObjectNode>("credentialSubject", requestorData);
            requestorData = wrapper;
        }
        val documentTemplate = profile.documentTemplate
        if (! documentTemplate.isNullOrEmpty()) {

            val rendered: String = profileTemplateRenderer.render(profile, documentTemplate, docs, value);
            try {
                return mapper.readerForUpdating(requestorData)
                        .readTree(rendered);
            } catch (ex: JsonProcessingException) {
                logger.errorv("An unexpected error occurred while parsing the template data for the request {0}", value.id, ex);
                throw IllegalArgumentException(ex);
            }
        } else {
            return requestorData;
        }
    }
}
