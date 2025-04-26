package eu.xfsc.not.oid4vp.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import eu.gaiax.notarization.api.extensions.BeginTaskResponse
import eu.gaiax.notarization.api.extensions.ExtensionTaskServiceBlockingApi
import eu.xfsc.not.oid4vp.Oid4VpConfig
import eu.xfsc.not.oid4vp.domain.RequestObjectBuilder
import eu.xfsc.not.oid4vp.domain.RequestObjectRepo
import eu.xfsc.not.oid4vp.domain.TrainValidationResult
import eu.xfsc.not.vc.VcValidationResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.rest.client.RestClientBuilder
import java.net.URI
import kotlin.reflect.jvm.javaMethod

@Path("internal/oid4vp-task")
class Task : ExtensionTaskServiceBlockingApi {
    @Inject
    lateinit var reqObjBuilder: RequestObjectBuilder

    @Inject
    lateinit var reqObjRepo: RequestObjectRepo

    @Inject
    lateinit var conf: Oid4VpConfig

    @POST
    @Path("begin")
    override fun beginTask(
        @QueryParam("success") success: URI,
        @QueryParam("failure") failure: URI,
        @QueryParam("profileId") profileId: String,
        @QueryParam("taskName") taskName: String,
        data: JsonNode?
    ): BeginTaskResponse {
        val authReqUriBase: String = data?.get("walletBaseUri")?.asText()
            ?: throw IllegalArgumentException("Missing walletBaseUri in data")
        // TODO: invoke failure URI?

        val cancelBaseUri = UriBuilder.fromUri(conf.baseUrl())
            .path(Task::class.java)
            .path(Task::cancelTask.javaMethod)
            .build()
        val (authReqUri, cancelUri) = reqObjBuilder.buildAndPersist(
            profileId,
            taskName,
            authReqUriBase,
            success,
            failure,
            cancelBaseUri
        )

        return BeginTaskResponse(
            redirect = authReqUri,
            cancel = cancelUri
        )
    }

    @Path("cancel")
    @GET
    @Transactional
    fun cancelTask(@QueryParam("id") authReqId: String) {
        reqObjRepo.removeRequestObject(authReqId)
    }
}

@ApplicationScoped
class CallbackClientBuilder {
    fun buildClients(success: URI, failure: URI): Pair<SuccessClient, FailureClient> {
        val successClient = RestClientBuilder.newBuilder()
            .baseUri(success)
            .build(SuccessClient::class.java)
        val failureClient = RestClientBuilder.newBuilder()
            .baseUri(failure)
            .build(FailureClient::class.java)
        return Pair(successClient, failureClient)
    }
}

interface SuccessClient {
    @POST
    fun send(data: SuccessResponse)
}

interface FailureClient {
    @POST
    fun send(data: FailureResponse)
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class FailureResponse(
    val errorCode: String,
    val errorDescription: String?,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class ValidationResult(
    val validationResult: VcValidationResult,
    var trainValidationResult: TrainValidationResult?
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class SuccessResponse(
    val validationResults: List<ValidationResult>
)
