package eu.xfsc.not.oid4vci

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.gaiax.notarization.api.issuance.*
import eu.xfsc.not.api.oid4vci.model.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import org.eclipse.microprofile.jwt.JsonWebToken
import org.jose4j.jwa.AlgorithmConstraints
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.jwk.JsonWebKey
import java.time.Instant


private val logger = KotlinLogging.logger {}

interface CredentialIssueService {
    fun processCredentialRequest(accessToken: JsonWebToken, credentialReq: CredentialRequest): CredentialResponse
    fun encryptResponse(resp: CredentialResponse, respEnc: CredentialResponseEncryption?, om: ObjectMapper = jacksonObjectMapper()): Response
}


@ApplicationScoped
class SyncCredentialIssueService : CredentialIssueService {

    @Inject
    lateinit var mdProv: OidcConfigProvider
    @Inject
    lateinit var nonceManager: NonceManager
    @Inject
    lateinit var issClientBuilder: IssuanceClientBuilder

    override fun processCredentialRequest(accessToken: JsonWebToken, credentialReq: CredentialRequest): CredentialResponse {
        // check the things related purely to the jwt
        validateJwtBasics(accessToken)

        // only allow named profiles
        when (credentialReq) {
            is SpecifyingCredentialRequest -> {
                // check request
                val atData = validateSpecifyingRequest(accessToken, credentialReq)
                val issClient = issClientBuilder.buildClient(atData.callbackUrl)
                // check proof
                val profile = atData.profiles.first()
                val proofResp = checkProof(issClient, profile, credentialReq.proof, accessToken.tokenID)
                // issue the credential
                val credential = issueCredential(issClient, profile, proofResp.proofPubKey)

                // prepare response
                val resp = prepareResponse(credential, accessToken)
                return resp
            }

            is ReferencedCredentialRequest -> {
                // check request
                val atData = validateReferencedRequest(accessToken, credentialReq)
                val issClient = issClientBuilder.buildClient(atData.callbackUrl)
                // check proof
                val profile = credentialReq.credentialIdentifier
                val proofResp = checkProof(issClient, profile, credentialReq.proof, accessToken.tokenID)
                // issue the credential
                val credential = issueCredential(issClient, profile, proofResp.proofPubKey)

                // prepare response
                val resp = prepareResponse(credential, accessToken)
                return resp
            }

            else -> {
                OauthErrorResponse(CredentialRequestError.InvalidCredentialRequest, "Unsupported credential request type.").throwError()
            }
        }
    }

    private fun validateJwtBasics(accessToken: JsonWebToken) {
        val audClaim = accessToken.audience ?: setOf()
        if (! audClaim.contains(AccessTokenAud)) {
            OauthErrorResponse(OauthErrorResponse.InvalidRequest, "Invalid audience.").throwError()
        }
    }

    private fun validateSpecifyingRequest(accessToken: JsonWebToken, credentialReq: SpecifyingCredentialRequest): AccessTokenData {
        val atData = accessToken.getAcessTokenData()

        // check if format matches profile
        val format = credentialReq.ext.format

        val supportedCred = mdProv.buildSupportedCredentials()[atData.profiles.first()]
        supportedCred?.also {
            if (it.format != format.value) {
                OauthErrorResponse(CredentialRequestError.InvalidCredentialRequest, "Requested credential format is not allowed.").throwError()
            }
            // TODO: add further checks for the structure of the VC
        } ?: OauthErrorResponse(CredentialRequestError.UnsupportedCredentialType, "Requested credential_identifier is not allowed.").throwError()

        validateAbstractRequest(credentialReq)

        return atData
    }

    private fun validateReferencedRequest(accessToken: JsonWebToken, credentialReq: ReferencedCredentialRequest): AccessTokenData {
        val atData = accessToken.getAcessTokenData()

        // check if profile is allowed for this access token
        val profile = credentialReq.credentialIdentifier
        if (!atData.profiles.contains(profile)) {
            OauthErrorResponse(CredentialRequestError.UnsupportedCredentialType, "Requested credential_identifier is not allowed.").throwError()
        }

        validateAbstractRequest(credentialReq)

        return atData
    }

    private fun validateAbstractRequest(credentialReq: CredentialRequest) {
        // check metadata if encryption is required
        val encRequired = mdProv.getCredentialResponseEncryptionSupport()?.encryptionRequired ?: false
        if (encRequired && credentialReq.credentialResponseEncryption == null) {
            OauthErrorResponse(
                CredentialRequestError.InvalidEncryptionParameters,
                "Response encryption is required, but no credential_response_encryption parameter has been provided."
            ).throwError()
        }
    }

    private fun checkProof(issClient: OidIssuanceApiRaw, profile: String, proof: Proof?, jti: String): VerifyProofSuccess {
        val issuer = mdProv.oidcConfig.issuerUrl()
        val nonce = nonceManager.currentNonce(jti)
            ?: OauthErrorResponse(CredentialRequestError.InvalidCredentialRequest, "No nonce available for the request.").throwError()
        if (proof == null) {
            OauthErrorResponse(CredentialRequestError.InvalidCredentialRequest, "Proof is missing.").throwError()
        }

        try {
            val proofReq = VerifyProofRequest(profile, nonce, issuer.toString(), proof)
            val proofResp = issClient.verifyProof(proofReq)
            return proofResp
        } catch (e: VerifyProofException) {
            logger.debug { "Proof verification failed (${e.resp.result}): ${e.resp.description}" }

            when (e.resp.result) {
                ProofVerificationResult.INVALID_PROOF_TYPE,
                ProofVerificationResult.WRONG_KEY,
                ProofVerificationResult.WRONG_NONCE,
                ProofVerificationResult.KEY_SYNTAX_ERROR,
                ProofVerificationResult.KEY_UNRESOLVEABLE,
                ProofVerificationResult.SIGNATURE_SYNTAX_ERROR,
                ProofVerificationResult.INVALID_SIGNATURE -> {
                    OauthErrorResponse(
                        CredentialRequestError.InvalidProof,
                        e.resp.description ?: "Proof verification failed (${e.resp.result})."
                    ).throwError()
                }

                ProofVerificationResult.INVALID_PROFILE -> {
                    OauthErrorResponse(
                        CredentialRequestError.InvalidCredentialRequest,
                        e.resp.description ?: "Requested credential_identifier is not defined."
                    ).throwError()
                }

                ProofVerificationResult.UNSUPPORTED_SIGNATURE -> {
                    OauthErrorResponse(
                        CredentialRequestError.InvalidCredentialRequest,
                        e.resp.description ?: "Requested proof signature algorithm is not supported."
                    ).throwError()
                }

                ProofVerificationResult.UNKNOWN_ERROR -> {
                    throw WebApplicationException(
                        e.resp.description ?: "Proof verification failed (${e.resp.result}).",
                        e
                    )
                }
            }
        }
    }

    private fun issueCredential(issClient: OidIssuanceApiRaw, profile: String, subjectPubKey: JsonNode): JsonNode {
        try {
            val issResp = issClient.issueCredential(IssueCredentialRequest(profile, subjectPubKey))
            return issResp.credential
        } catch (e: IssueCredentialException) {
            logger.debug { "Credential issuance failed (${e.resp.result}): ${e.resp.description}" }

            when (e.resp.result) {
                IssueCredentialResult.INVALID_PROFILE -> {
                    OauthErrorResponse(
                        CredentialRequestError.InvalidCredentialRequest,
                        e.resp.description ?: "Requested credential_identifier is not defined."
                    ).throwError()
                }
                IssueCredentialResult.INVALID_KEY -> {
                    OauthErrorResponse(
                        CredentialRequestError.InvalidProof,
                        e.resp.description ?: "Requested subject public key is not valid."
                    ).throwError()
                }
                IssueCredentialResult.UNKNOWN_ERROR -> {
                    throw WebApplicationException(
                        e.resp.description ?: "Credential issuance failed (${e.resp.result}).", e
                    )
                }
            }
        }
    }

    private fun prepareResponse(credential: JsonNode, accessToken: JsonWebToken): CredentialResponseSync {
        val resp = CredentialResponseSync(
            credential = credential,
        )

        // obtain new nonce
        val newNonceValues = nonceManager.renewNonce(accessToken.tokenID)
        // check if it makes sense to give out a new nonce
        if (newNonceValues !== null && atNotExpired(accessToken)) {
            val (newNonce, nonceExpiresIn) = newNonceValues
            resp.also {
                it.cNonce = newNonce
                it.cNonceExpiresIn = nonceExpiresIn
            }
        }

        return resp
    }

    override fun encryptResponse(resp: CredentialResponse, respEnc: CredentialResponseEncryption?, om: ObjectMapper): Response {
        val encParams = mdProv.getCredentialResponseEncryptionSupport()

        return if (respEnc != null) {
            // check metadata if encryption is supported
            if (encParams == null) {
                OauthErrorResponse(
                    CredentialRequestError.InvalidEncryptionParameters,
                    "Response encryption is not supported."
                ).throwError()
            } else {
                val jwksJson = om.writeValueAsString(respEnc.jwk)
                val jwk = JsonWebKey.Factory.newJwk(jwksJson)
                val jwe = buildJwePrototype(encParams).also {
                    it.algorithmHeaderValue = respEnc.alg
                    it.encryptionMethodHeaderParameter = respEnc.enc
                    it.key = jwk.key
                    it.setPlaintext(om.writeValueAsString(resp))
                }.compactSerialization
                Response.ok(jwe, "application/jwt").build()
            }
        } else {
            // no encryption needed
            Response.ok(resp, MediaType.APPLICATION_JSON_TYPE).build()
        }
    }

    private fun buildJwePrototype(encParams: CredentialResponseEncryptionSupport): JsonWebEncryption {
        val jwe = JsonWebEncryption().also {
            val encAlgs = encParams.encValuesSupported.toTypedArray()
            val encConstraints = AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, *encAlgs)
            it.setContentEncryptionAlgorithmConstraints(encConstraints)

            val algAlgs = encParams.algValuesSupported.toTypedArray()
            val algConstraints = AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, *algAlgs)
            it.setAlgorithmConstraints(algConstraints)
        }
        return jwe
    }

    private fun atNotExpired(accessToken: JsonWebToken): Boolean {
        return accessToken.expirationTime.toExpirationDate().minusSeconds(5).isAfter(Instant.now())
    }
}
