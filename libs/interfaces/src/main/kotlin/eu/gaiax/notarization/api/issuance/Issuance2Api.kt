package eu.gaiax.notarization.api.issuance

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import eu.xfsc.not.api.util.JsonValueEnum
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.net.URI
import java.time.Instant

@OpenAPIDefinition(
    info = Info(
        title = "Endpoint exposed by the issuance2 service",
        version = "1.0",
    ),
    tags = [Tag(name = "Issuance2")],
)
@Path("api/v2/issuance")
interface Issuance2Api {
    @Operation(
        summary = "Init Issuance Service",
        description = "The service will create needed resources for the given pair of keyType and signatureAlgorithmType and return an issuerDid which will be used for the profile",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The request was successfully.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = ProfileIssuanceSpec::class),
                )],
            ),
            APIResponse(
                responseCode = "406",
                description = "The service doesn't provide expected methods.",
            ),
        ]
    )
    @Path("init-service")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun initService(req: ServiceInitRequest) : ProfileIssuanceSpec

    @Operation(
        summary = "Init Issuance Session",
        description = "Initiates a new session",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The session was initiated.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = IssuanceInitResponse::class),
                )],
            ),
        ]
    )
    @Path("session")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun initSession(req: IssuanceInitRequest): IssuanceInitResponse

    @Operation(
        summary = "Cancel Issuance Session",
        description = "Cancels the session",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The session was cancelled.",
            ),
        ]
    )
    @Path("session/{issuance-token}")
    @DELETE
    fun cancelSession(@PathParam("issuance-token") token: String)

    @Operation(
        summary = "Deletes old sessions",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
            ),
        ]
    )
    @Path("routines/pruneTimeoutSessions")
    @DELETE
    fun pruneTimeoutSessions()
}

@Path("api/v2/issuance")
interface Issuance2ApiAsync {
    @Operation(
        summary = "Init Issuance Service",
        description = "The service will create needed resources for the given pair of keyType and signatureAlgorithmType and return an issuerDid which will be used for the profile",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The request was successfully.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = ProfileIssuanceSpec::class),
                )],
            ),
            APIResponse(
                responseCode = "406",
                description = "The service doesn't provide expected methods.",
            ),
        ]
    )
    @Path("init-service")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun initService(req: ServiceInitRequest) : Uni<ProfileIssuanceSpec>

    @Operation(
        summary = "Init Issuance Session",
        description = "Initiates a new session",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The session was initiated.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = IssuanceInitResponse::class),
                )],
            ),
        ]
    )
    @Path("session")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun initSession(req: IssuanceInitRequest): Uni<IssuanceInitResponse>
}

@Schema(
    description = "Kind of key specified for creation.",
)
enum class KeyType(override val value: String) : JsonValueEnum {
    RSA("RSA"),
    SECP256k1("secp256k1"),
    BLS12381G1("Bls48581G1"),
    BLS12381G2("Bls48581G2"),
    BLS48581G1("Bls48581G1"),
    BLS48581G2("Bls48581G2"),
    ED25519("Ed25519"),
    X25519("X25519"),
    P_256("P-256"),
    P_384("P-384"),
    P_521("P-521"),
    ;
}

@Schema(
    description = "Kind of signature algorithm specified for creation.",
)
enum class SignatureType(override val value: String) : JsonValueEnum {
    // W3C 1.1
    ED25519SIGNATURE2018("Ed25519Signature2018"),
    ED25519SIGNATURE2020("Ed25519Signature2020"),
    RSASIGNATURE2018("RsaSignature2018"),
    JCSED25519SIGNATURE2020("JcsEd25519Signature2020"),
    ECDSASECP256K1SIGNATURE2019("EcdsaSecp256k1Signature2019"),
    ECDSAKOBLITZSIGNATURE2016("EcdsaKoblitzSignature2016"),
    JCSECDSASECP256K1SIGNATURE2019("JcsEcdsaSecp256k1Signature2019"),
    JSONWEBSIGNATURE2020("JsonWebSignature2020"),
    BBSBLSSIGNATURE2020("BbsBlsSignature2020"),
    BBSBLSSIGNATUREPROOF2020("BbsBlsSignatureProof2020"),
    // JWT / JWA
    RS256("RS256"), RS384("RS384"), RS512("RS512"),
    PS256("PS256"), PS384("PS384"), PS512("PS512"),
    ES256("ES256"), ES384("ES384"), ES512("ES512"),
    ES256K("ES256K"),
    EdDSA("EdDSA"),
    ;
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class ServiceInitRequest (
    val profileId: String,
    val keyType : KeyType,
    val signatureType: SignatureType,
)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class ProfileIssuanceSpec (
    val issuingDid: String,
    val revocatingDid: String?,
    val signatureType: SignatureType,
) {
    companion object {
        fun throwError() : Nothing{
            throw WebApplicationException(
                Response.status(406)
                    .entity(this)
                    .build()
            )
        }

    }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class IssuanceInitRequest(
    val profileId: String,
    val credentialData: JsonNode,
    val issuanceTimestamp: Instant,
    val holderDID: String?,
    val invitationURL: URI?,
    val successURL: URI,
    val failureURL: URI
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class IssuanceInitResponse(
    var offerUrl: URI,
    var cancelUrl: URI,
)
