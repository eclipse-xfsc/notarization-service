package eu.gaiax.notarization.auto_notary.application

import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.auto_notary.application.model.ApprovalResult
import eu.gaiax.notarization.auto_notary.application.model.ApprovalResults
import eu.gaiax.notarization.auto_notary.application.model.ApprovalStatus
import eu.gaiax.notarization.auto_notary.web.client.RequestProcessingRestClient
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.interfaces.ECPrivateKey
import java.security.spec.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrElse

val logger = KotlinLogging.logger { }

/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class AutoApprovingService {
    @RestClient
    lateinit var requestProcessingClient: RequestProcessingRestClient

    @Inject
    lateinit var augmentationConfig: IdentityAugmentationConfig

    @Inject
    lateinit var objectMapper: ObjectMapper

    fun approveAvailableRequests(): Uni<ApprovalResults> {
        val availableStatus = "available"
        return claimAndAccept(availableStatus)
    }

    private fun claimAndAccept(availableStatus: String): Uni<ApprovalResults> {
        return pageAll(availableStatus)
            .onItem().transformToUniAndMerge { request: RequestProcessingRestClient.NotarizationRequestSummary? ->
                this.claimAndAccept(request)
            }
            .collect()
            .asList()
            .map { items -> this.summarize(items) }
    }

    private fun summarize(items: List<ApprovalResult>): ApprovalResults {
        val uniqueItems = items.toSet()
        var success: Long = 0
        var failure: Long = 0
        for (item in uniqueItems) {
            when (item.status) {
                ApprovalStatus.Success -> success += 1
                ApprovalStatus.Failure -> failure += 1
                else -> throw IllegalArgumentException("Unknown approval result")
            }
        }
        return ApprovalResults(success, failure)
    }

    private fun pageAll(status: String): Multi<RequestProcessingRestClient.NotarizationRequestSummary?> {
        val previousStatus = AtomicLong(0L)
        return Multi.createBy().repeating().uni(
            Supplier { requestProcessingClient.fetchRequests(0, PAGE_SIZE, status) })
            .whilst { page: RequestProcessingRestClient.PagedNotarizationRequestSummary? ->
                val requestCount = page!!.requestCount
                hasRequests(page) && requestCount != previousStatus.getAndSet(requestCount)
            }.onItem()
            .transformToMultiAndMerge { response: RequestProcessingRestClient.PagedNotarizationRequestSummary? ->
                logger.debug { "Paging a total of ${response?.notarizationRequests?.size ?: 0} requests" }
                Multi.createFrom().iterable(
                    response!!.notarizationRequests
                )
            }
    }

    private fun hasRequests(page: RequestProcessingRestClient.PagedNotarizationRequestSummary?): Boolean {
        return page != null && page.requestCount > 0 && page.notarizationRequests != null && page.notarizationRequests!!.isNotEmpty()
    }

    fun claimAndAccept(request: RequestProcessingRestClient.NotarizationRequestSummary?): Uni<ApprovalResult> {
        if (request == null) {
            return Uni.createFrom().item(ApprovalResult(null, ApprovalStatus.Failure))
        }
        val profileId = request.profileId
        val id = request.id
        return claimAndAccept(profileId, id)
    }

    fun claimAndAccept(profileId: String?, id: String?): Uni<ApprovalResult> {
        return if (profileId == null || id == null) {
            Uni.createFrom().item(ApprovalResult(id, ApprovalStatus.Failure))
        } else {
            logger.debug { "claimAndAccept: Claiming $id for profile $profileId" }
            requestProcessingClient.claim(profileId, id)
                .onItem().transformToUni { _ ->
                    if (augmentationConfig.enabled() && augmentationConfig.decryptionKey().isPresent) {
                        logger.debug { "claimAndAccept: Augmenting $id for profile $profileId" }
                        requestProcessingClient.fetchIdentities(profileId, id)
                            .chain {  identities ->
                                val key: ECPrivateKey = createECPrivateKey(augmentationConfig.decryptionKey().get())

                                val jwtConsumer = JwtConsumerBuilder()
                                    .setDecryptionKey(key)
                                    .setDisableRequireSignature()
                                    .build()

                                val claims = jwtConsumer.processToClaims(identities.first().data)
                                val flattenedClaims = claims.flattenClaims()
                                val result = objectMapper.createObjectNode()!!
                                val targetClaims = augmentationConfig.claims().getOrElse { flattenedClaims.keys }
                                for (targetClaim in targetClaims) {
                                    val foundClaim = flattenedClaims["${augmentationConfig.ignorePrefix()}$targetClaim"]
                                    if (!foundClaim.isNullOrEmpty()) {
                                        result.put(targetClaim, foundClaim.first { it != null }.toString())
                                    }
                                }
                                logger.debug { "claimAndAccept: Assigning augmentation $id for profile $profileId" }
                                requestProcessingClient.putCredentialAugmentation(profileId, id, result)
                            }
                    }
                    else {
                        Uni.createFrom().nullItem()
                    }

                }
                .onItem().transformToUni { _ ->
                    logger.debug { "claimAndAccept: Accepting $id for profile $profileId" }
                    requestProcessingClient.accept(profileId, id)
                }
                .replaceWith {
                    logger.debug { "claimAndAccept: Accepted $id for profile $profileId" }
                    ApprovalResult(id, ApprovalStatus.Success)
                }
                .onFailure().recoverWithItem { t: Throwable? ->
                    logger.warn(t) { "Could not claim request $id!" }
                    ApprovalResult(id, ApprovalStatus.Failure)
                }
        }
    }

    fun approveOwnRequests(): Uni<ApprovalResults> {
        val availableStatus = "ownClaimed"
        return pageAll(availableStatus)
            .onItem().transformToUniAndMerge { request: RequestProcessingRestClient.NotarizationRequestSummary? ->
                this.claimAndAccept(request)
            }
            .collect()
            .asList().map { items: List<ApprovalResult> -> this.summarize(items) }
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidParameterSpecException::class,
        InvalidKeySpecException::class
    )
    private fun createECPrivateKey(rawBase64UrlEncodedKey: String): ECPrivateKey {
        val decodedKey = Base64.getUrlDecoder().decode(rawBase64UrlEncodedKey)
        val kf = KeyFactory.getInstance("EC")
        val parameters = AlgorithmParameters.getInstance("EC")
        parameters.init(ECGenParameterSpec("secp384r1"))
        val ecParameters =
            parameters.getParameterSpec(
                ECParameterSpec::class.java
            )
        val privateSpec =
            ECPrivateKeySpec(BigInteger(1, decodedKey), ecParameters)
        return kf.generatePrivate(privateSpec) as ECPrivateKey
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
