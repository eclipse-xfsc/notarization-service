package eu.xfsc.not.oid4vp.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import ellog.uuid.UUID
import eu.gaiax.notarization.util.RandomGenerator
import eu.xfsc.not.api.util.fromString
import eu.xfsc.not.oid4vp.ClientKeystore
import eu.xfsc.not.oid4vp.KeyStore
import eu.xfsc.not.oid4vp.Oid4VpConfig
import eu.xfsc.not.oid4vp.model.*
import eu.xfsc.not.oid4vp.rest.Oid4VpImpl
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.scheduler.Scheduled
import io.smallrye.jwt.algorithm.SignatureAlgorithm
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.jvm.javaMethod


private val logger = KotlinLogging.logger {  }

@ApplicationScoped
class RequestObjectBuilder {

    @Inject
    lateinit var rand: RandomGenerator
    @Inject
    lateinit var conf: Oid4VpConfig
    @Inject
    @ClientKeystore
    lateinit var ks: KeyStore
    @Inject
    lateinit var om: ObjectMapper
    @Inject
    lateinit var reqObjRepo: RequestObjectRepo
    @Inject
    lateinit var clientMd: ClientMetadataService
    @Inject
    lateinit var ldpVerifier: LdpVerifier
    @Inject
    lateinit var ldpJwtVerifier: LdpJwtVerifier

    val baseUri: URI
        get() {
            return conf.baseUrl()
        }
    val clientId: String
        get() {
            return conf.client().clientId()
        }
    val clientIdScheme: ClientIdScheme
        get() {
            return fromString(conf.client().scheme()) ?: throw IllegalStateException("Invalid client ID scheme in configuration")
        }

    /**
     * Request Object (RFC 9101, Sec. 4) and persists for later retrieval
     */
    @Transactional
    fun buildAndPersist(profileId: String, taskName: String, authReqUriBase: String, successUri: URI, failureUri: URI, cancelBaseUri: URI): Pair<URI, URI> {
        // authreq_id must be non guessable, so don't use UUID
        val authReqId = rand.genNonce()

        val lifetime = conf.requestObjectLifetime()
        val expAt = Instant.now().plus(lifetime)
        val reqObj = buildAuthReq(authReqId)
        val jwt = buildJwt(reqObj, expAt)
        reqObjRepo.registerRequestObject(profileId, taskName, authReqId, reqObj, jwt, successUri, failureUri, expAt)

        val authReqUrl = buildJarAuthReq(authReqId, authReqUriBase)
        val cancelUri = UriBuilder.fromUri(cancelBaseUri)
            .queryParam("id", authReqId)
            .build()
        return authReqUrl to cancelUri
    }

    fun buildJarAuthReq(authReqId: String, authReqUriBase: String): URI {
        val reqObjUri = UriBuilder.fromUri(baseUri)
            .path(Oid4VpImpl::class.java)
            .path(Oid4VpImpl::getRequestObject.javaMethod)
            .queryParam("id", authReqId)
            .build()

        // build AuthenticationRequest
        val authReq = JarAuthRequest(
            requestUri = reqObjUri.toASCIIString(),
            clientId = clientId,
        )

        val authReqUri = UriBuilder.fromUri(authReqUriBase)
            .apply {
                val authReqQueryParams = authReq.toQueryParams()
                for (entry in authReqQueryParams) { queryParam(entry.key, entry.value) }
            }
            .build()
        return authReqUri
    }

    fun buildAuthReq(authReqId: String): AuthRequestObject {
        // build presentation definition
        val presDef = PresentationDefinition(
            id = UUID.createRandom().toString(),
            inputDescriptors = listOf(
                InputDescriptor(
                    id = UUID.createRandom().toString(),
                    format = mapOf(
                        ldpVerifier.supportedFormat(),
                        ldpJwtVerifier.supportedFormat(),
                    ),
                    constraints = ConstraintsObject(),
                )
            ),
            format = mapOf(
                ldpVerifier.supportedFormat(),
                ldpJwtVerifier.supportedFormat(),
            )
        )

        return AuthRequestObject(
            responseType = listOf(ResponseType.VP_TOKEN),
            clientIdScheme = clientIdScheme,
            clientId = clientId,
            nonce = rand.genNonce(),
            state = authReqId, // used to correlate the response with the DB entry
            clientMetadata = clientMd.buildMetadata(),
            responseMode = ResponseMode.DIRECT_POST,
            responseUri = UriBuilder.fromUri(baseUri)
                .path(Oid4VpImpl::class.java)
                .path(Oid4VpImpl::authResponse.javaMethod)
                .build().toString(),
            presentationDefinition = presDef,
        )
    }

    fun buildJwt(reqObj: AuthRequestObject, expAt: Instant): String {
        // OID4VP, Sec. 5.6: the aud Claim value depends on whether the recipient of the request can be identified by the Verifier or not
        // the aud Claim MUST equal to the issuer Claim value, when Dynamic Discovery is performed.
        // the aud Claim MUST be "https://self-issued.me/v2", when Static Discovery metadata is used.
        val aud = "https://self-issued.me/v2"

        val claims: Map<String, Any> = om.convertValue(reqObj)

        val (prKey, cert) = ks.getEntry()
        val jwt = Jwt.claims(claims)
            .audience(aud)
            .expiresAt(expAt)
            .jws()
            .chain(cert)
            .apply {
                getJwsAlgorithm(conf.client().jwsAlg())?.let {
                    algorithm(it)
                }
            }
            .sign(prKey)

        return jwt
    }

    private fun getJwsAlgorithm(algNameOpt: Optional<String>): SignatureAlgorithm? {
        return algNameOpt.getOrNull()?.let { algName ->
            val alg = SignatureAlgorithm.values().find { it.algorithm == algName }
            if (alg == null) {
                logger.error { "Invalid JWS algorithm in configuration: $algName" }
            }
            alg
        }
    }
}


@Entity
class RequestObject : PanacheEntityBase {
    @Id
    lateinit var authReqId: String
    lateinit var profileId: String
    lateinit var taskName: String
    @JdbcTypeCode(SqlTypes.JSON)
    lateinit var requestObject: AuthRequestObject
    lateinit var requestObjectJwt: String
    lateinit var exp: Instant
    lateinit var successUri: URI
    lateinit var failureUri: URI
}

@ApplicationScoped
class RequestObjectRepo : PanacheRepositoryBase<RequestObject, String> {
    fun registerRequestObject(profileId: String, taskName: String, reqId: String, reqObject: AuthRequestObject, reqObjJwt: String, successUri: URI, failureUri: URI, expiresAt: Instant) {
        val new = RequestObject().apply {
            this.authReqId = reqId
            this.profileId = profileId
            this.taskName = taskName
            this.requestObject = reqObject
            this.requestObjectJwt = reqObjJwt
            this.successUri = successUri
            this.failureUri = failureUri
            this.exp = expiresAt
        }
        new.persist()
    }

    private fun findByReqId(reqId: String): RequestObject {
        return findById(reqId)
            ?: throw WebApplicationException("No session available for the requested ID.", Response.Status.NOT_FOUND)
    }

    fun getRequestObjectJwt(reqId: String): String {
        return findByReqId(reqId).requestObjectJwt
    }

    fun getRequestObject(reqId: String): AuthRequestObject {
        return findByReqId(reqId).requestObject
    }

    fun getCallbacks(reqId: String): Pair<URI, URI> {
        return findByReqId(reqId).successUri to findByReqId(reqId).failureUri
    }

    fun getTaskReference(reqId: String): Pair<String, String> {
        return findByReqId(reqId).profileId to findByReqId(reqId).taskName
    }

    fun removeRequestObject(reqId: String) {
        deleteById(reqId)
    }

    fun removeExpiredObjects(now: Instant) {
        delete("exp < ?1", now)
    }
}


@ApplicationScoped
class RequestObjectCleanup {
    @Inject
    lateinit var objRepo: RequestObjectRepo
    @Scheduled(every = "15m")
    @Transactional
    fun removeStaleIdentifiers() {
        // make sure we don't delete anything while processing requests
        objRepo.removeExpiredObjects(Instant.now().minusSeconds(30))
    }
}
