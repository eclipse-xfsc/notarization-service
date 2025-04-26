package eu.xfsc.not.oid4vp.domain

import io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import mu.KotlinLogging
import openapi.train.api.TrustedContentResolverApi
import openapi.train.model.ResolveRequest
import openapi.train.model.ResolvedDid
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

const val TRAIN_API_IDENTIFIER = "train_api"

private val log = KotlinLogging.logger {}


interface TrainValidation {
    fun needsTrainCheck(): Boolean
    @Throws(ResponseValidationException::class)
    fun checkDidTrust(profileId: String, taskName: String, issuer: String): TrainValidationResult?
}

@ApplicationScoped
class TrainValidationImpl : TrainValidation {

    @ConfigProperty(name = "quarkus.rest-client.${TRAIN_API_IDENTIFIER}.url")
    lateinit var trainEndpoint: Optional<String>

    @Inject
    lateinit var trainDataProvider: TrainDataProvider

    override fun needsTrainCheck(): Boolean {
        return trainEndpoint.isPresent
    }

    internal fun buildClient(): TrustedContentResolverApi {
        val trainClient = RestClientCDIDelegateBuilder.createDelegate(
            TrustedContentResolverApi::class.java, null, TRAIN_API_IDENTIFIER
        )
        return trainClient
    }

    @Throws(ResponseValidationException::class)
    override fun checkDidTrust(profileId: String, taskName: String, issuer: String): TrainValidationResult? {

        val trainApi = buildClient()
        val trainParams = trainDataProvider.getTrainParameter(profileId, taskName)

        if (trainParams == null ||
            trainParams.trustSchemePointers.isEmpty()
        ) {
            val msg = "No trustSchemePointers defined in task."
            log.error {msg}
            throw ResponseValidationException(
                ResponseValidationErrorCode.InvalidTrustPointers,
                msg
            )
        }

        val resolveRequest = ResolveRequest().apply {
            this.issuer = issuer
            this.trustSchemePointers = trainParams.trustSchemePointers
            this.endpointTypes = trainParams.endpointTypes
        }

        val resolveResponse = trainApi.resolveTrustList(resolveRequest)

        if (resolveResponse.resolvedResults == null || resolveResponse.resolvedResults.isEmpty()) {
            val msg = "Train validation had no results."
            log.error {msg}
            throw ResponseValidationException(
                ResponseValidationErrorCode.InvalidTrainResult,
                msg
            )
        }

        // check if at least on entry has didVerified and a list containing issuer
        resolveResponse.resolvedResults.filter {
            it.resolvedDoc.didVerified
        }.ifEmpty {
            val msg = "Not all resolved trust lists were valid and contained issuer."
            log.error {msg}
            throw ResponseValidationException(
                ResponseValidationErrorCode.InvalidTrainTrust,
                msg
            )
        }

        return TrainValidationResult(resolveResponse.resolvedResults)
    }
}

class TrainValidationResult(
    val resolvedResults: List<ResolvedDid>
)
