package eu.gaiax.notarization.auto_notary.web.client

import com.fasterxml.jackson.databind.node.ObjectNode
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.faulttolerance.Bulkhead
import org.eclipse.microprofile.faulttolerance.CircuitBreaker
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 *
 * @author Neil Crossley
 */
@Path("/api/v1")
@RegisterRestClient(configKey = "requestprocessing-api")
@RegisterProvider(
    OidcClientRequestReactiveFilter::class
)
interface RequestProcessingRestClient {
    @GET
    @Path("/requests")
    @CircuitBreaker(requestVolumeThreshold = 4)
    @Timeout(value = 2500L, unit = ChronoUnit.MILLIS)
    fun fetchRequests(
        @RestQuery offset: Int?,
        @RestQuery limit: Int?,
        @RestQuery filter: String?
    ): Uni<PagedNotarizationRequestSummary>

    @POST
    @Path("/profiles/{profileId}/requests/{notarizationRequestId}/claim")
    @CircuitBreaker(requestVolumeThreshold = 10)
    @Timeout(value = 2000L, unit = ChronoUnit.MILLIS)
    @Bulkhead(value = 30, waitingTaskQueue = 50)
    fun claim(@RestPath profileId: String, @RestPath notarizationRequestId: String): Uni<Void>

    @GET
    @Path("/profiles/{profileId}/requests/{notarizationRequestId}/identity")
    @CircuitBreaker(requestVolumeThreshold = 10)
    @Timeout(value = 2000L, unit = ChronoUnit.MILLIS)
    @Bulkhead(value = 30, waitingTaskQueue = 50)
    fun fetchIdentities(@RestPath profileId: String, @RestPath notarizationRequestId: String): Uni<List<IdentityView>>

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/profiles/{profileId}/requests/{notarizationRequestId}/credentialAugmentation")
    @CircuitBreaker(requestVolumeThreshold = 5)
    @Timeout(value = 3000L, unit = ChronoUnit.MILLIS)
    @Bulkhead(value = 30, waitingTaskQueue = 50)
    fun putCredentialAugmentation(@RestPath profileId: String, @RestPath notarizationRequestId: String, credentialOverride: ObjectNode): Uni<Void>

    @POST
    @Path("/profiles/{profileId}/requests/{notarizationRequestId}/accept")
    @CircuitBreaker(requestVolumeThreshold = 5)
    @Timeout(value = 3000L, unit = ChronoUnit.MILLIS)
    @Bulkhead(value = 30, waitingTaskQueue = 50)
    fun accept(@RestPath profileId: String, @RestPath notarizationRequestId: String): Uni<Void>

    class PagedNotarizationRequestSummary(
        var pageCount: Int,
        var requestCount: Long,
        var notarizationRequests: List<NotarizationRequestSummary>?
    )

    data class NotarizationRequestSummary(
        var id: String,
        var profileId: String,
        var createdAt: OffsetDateTime,
        var lastModified: OffsetDateTime,
        var requestState: String,
        var holder: String?,
        var totalDocuments: Int,
        var rejectComment: String?
    )
    data class IdentityView(
        var data: String?,
        var algorithm: String?,
        var encryption: String?,
        var jwk: String?,
        var createdAt: OffsetDateTime?
    ) {

    }
}
