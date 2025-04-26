package eu.gaiax.notarization.api.issuance

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import eu.xfsc.not.api.oid4vci.model.Proof
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


@OpenAPIDefinition(
    info = Info(
        title = "Endpoint exposed by the issuance service to the OID4VCI service",
        version = "1.0",
    ),
    tags = [Tag(name = "OID4VCI Issuance")],
)
@Path("/api/v2/oid-issuance/{issuance-token}")
interface OidIssuanceApi : OidIssuanceApiRaw

interface OidIssuanceApiRaw {

    @Operation(
        summary = "Verify Proof",
        description = "Verifies the proof of a credential request.",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The proof was validated.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = VerifyProofSuccess::class),
                )],
            ),
            APIResponse(
                responseCode = "400",
                description = "The proof was not validated.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = VerifyProofFailure::class),
                )],
            ),
        ]
    )

    @Path("verify-proof")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun verifyProof(verifyReq: VerifyProofRequest): VerifyProofSuccess

    @Operation(
        summary = "Issue Credential",
        description = "Issues a credential for the given profile and key.",
    )
    @APIResponses(
        value = [
            APIResponse(
                responseCode = "200",
                description = "The credential was issued.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = IssueCredentialSuccess::class),
                )],
            ),
            APIResponse(
                responseCode = "400",
                description = "The credential has not been issued.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = IssueCredentialFailure::class),
                )],
            ),
        ],
    )

    @Path("issue-credential")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun issueCredential(issueReq: IssueCredentialRequest): IssueCredentialSuccess
}


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class VerifyProofRequest (
    val profile: String,
    val challenge: String,
    val domain: String,
    val proof: Proof,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class VerifyProofSuccess (
    /**
     * The public key used in the proof.
     * This can be a JSON Web Key (JWK) or a DID URI, depending on the proof type.
     */
    val proofPubKey: JsonNode,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class VerifyProofFailure (
    val result: ProofVerificationResult,
    /**
     * The description of the verification error.
     * This can include further information than it is provided in the error code.
     */
    val description: String? = null,
)

fun VerifyProofFailure.throwError(): Nothing {
    throw WebApplicationException(
        Response.status(400)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(this)
            .build()
    )
}

enum class ProofVerificationResult {
    INVALID_PROFILE,
    INVALID_PROOF_TYPE,
    WRONG_NONCE,
    WRONG_KEY,
    KEY_UNRESOLVEABLE,
    INVALID_SIGNATURE,
    UNSUPPORTED_SIGNATURE,
    SIGNATURE_SYNTAX_ERROR,
    KEY_SYNTAX_ERROR,
    UNKNOWN_ERROR,
}


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class IssueCredentialRequest (
    val profile: String,
    val subjectPubKey: JsonNode,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class IssueCredentialSuccess (
    val credential: JsonNode,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class IssueCredentialFailure (
    val result: IssueCredentialResult,
    /**
     * The description of the error.
     * This can include further information than it is provided in the error code.
     */
    val description: String? = null,
)

fun IssueCredentialFailure.throwError(): Nothing {
    throw WebApplicationException(
        Response.status(400)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(this)
            .build()
    )
}

enum class IssueCredentialResult {
    INVALID_PROFILE,
    INVALID_KEY,
    UNKNOWN_ERROR,
}
