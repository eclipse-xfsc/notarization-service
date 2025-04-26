package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.*
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.xfsc.not.api.oid4vci.model.JwtProof
import eu.xfsc.not.api.oid4vci.model.LdpVpProof
import eu.xfsc.not.api.oid4vci.model.Proof
import eu.xfsc.not.api.oid4vci.model.ProofTypeEnum
import eu.xfsc.not.ssi_issuance2.domain.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.WebApplicationException
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI

private val log = KotlinLogging.logger {}

@ApplicationScoped
class OidIssuanceService {

    @Inject
    lateinit var issuanceService: IssuanceService

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var ldpVerifier: LdpProofVerifier
    @Inject
    lateinit var jwtVerifier: JwtProofVerifier

    @Inject
    lateinit var profileProvider: ProfileProvider

    @Inject
    lateinit var acyPyLdpIssuer: AcapyIssuer

    @Inject
    lateinit var localLdpIssuer: LocalLdpCredentialIssuer

    @RestClient
    lateinit var offerApi: Oid4VciOfferApiClient

    @Inject
    lateinit var callBackUrlBuilder: CallbackUrlBuilder

    @Inject
    lateinit var sdJwtCredentialIssuer: SDJwtCredentialIssuer


    fun getOffer(profiles: List<String>, token: String): URI {
        return URI.create(
            offerApi.createCredentialOffer(
                profiles,
                token,
                callBackUrlBuilder.buildOidIssuanceApiUrl(token).toString(),
            )
        )
    }

    @Throws(WebApplicationException::class)
    fun verifyProof(verifyReq: VerifyProofRequest, issuanceToken: String): VerifyProofSuccess {
        log.info { "Verify proof called" }
        val session = sessionRepository.getByToken(issuanceToken) ?: run {
            log.error { "Session not found" }
            log.debug { "Not found session token was $issuanceToken" }
            throw NotFoundException()
        }

        val profile = profileProvider.fetchProfile(session.profileId) ?: run {
            log.error { "Profile not found" }
            throw NotFoundException()
        }

        checkProofType(profile.kind, verifyReq.proof)
        val verifyResult = when(val proof = verifyReq.proof) {
            is LdpVpProof -> {
                ldpVerifier.verify(verifyReq.challenge, verifyReq.domain, proof.ldpVp, session.holderDid)
            }
            is JwtProof -> {
                jwtVerifier.verify(proof.jwt, "openid4vci-proof+jwt", verifyReq.challenge, verifyReq.domain, null, session.holderDid)
            }
            else -> {
                log.error { "Unsupported proof type supplied." }
                VerifyProofFailure(
                    result = ProofVerificationResult.UNKNOWN_ERROR,
                ).throwError()
            }
        }

        return when (verifyResult) {
            is VerifierValidResult -> {
                log.debug { "Proof verified." }
                VerifyProofSuccess(
                    proofPubKey = verifyResult.proofPubKey
                )
            }

            is VerifierFailedResult -> {
                log.debug { "Proof verification failed. Result was: ${verifyResult.result}; description: ${verifyResult.description}" }
                VerifyProofFailure(
                    result = verifyResult.result,
                    description = verifyResult.description
                ).throwError()
            }
        }
    }

    private fun checkProofType(kind: CredentialKind?, proof: Proof) {
        if (kind == CredentialKind.JsonLD && proof.proofType == ProofTypeEnum.LDP_VP) {
            return
        } else if (kind == CredentialKind.SD_JWT && proof.proofType == ProofTypeEnum.JWT) {
            return
        }
        VerifyProofFailure(
            result = ProofVerificationResult.INVALID_PROOF_TYPE,
            description = "Proof type does not match credential kind."
        ).throwError()
    }

    @Throws(WebApplicationException::class)
    fun issueCredential(issueReq: IssueCredentialRequest, issuanceToken: String): IssueCredentialSuccess {
        log.info { "Issuing credential." }
        val session = sessionRepository.getByToken(issuanceToken) ?: run {
            log.error { "Session not found" }
            log.debug { "Not found session token was $issuanceToken" }
            throw NotFoundException()
        }

        val profile = profileProvider.fetchProfile(session.profileId) ?: run {
            log.error { "Profile not found" }
            throw NotFoundException()
        }

        val issuanceData = profileProvider.fetchDids(session.profileId) ?: run {
            log.error { "Profile issuance data not found" }
            throw ServerErrorException("Profile issuance data not found" , 500)
        }

        //if there are implementations for different/additional kinds add them here
        val issuanceResult = when (profile.kind) {
            CredentialKind.JsonLD -> {
                val issuer = chooseIssuer(issuanceData.issuingDid)
                IssueCredentialSuccess(
                    issuer.createCredential(
                        profile,
                        issuanceData.issuingDid,
                        issuanceData.signatureType,
                        session.issuanceTimestamp,
                        issueReq.subjectPubKey.textValue(),
                        session.credentialData,
                    )
                )
            } CredentialKind.SD_JWT -> {
                IssueCredentialSuccess(
                    sdJwtCredentialIssuer.createCredential(
                        profile,
                        issuanceData.issuingDid,
                        issuanceData.signatureType,
                        session.issuanceTimestamp,
                        issueReq.subjectPubKey as ObjectNode,
                        session.credentialData,
                    )
                )
            } else -> {
                log.debug { "Unsupported credential type supplied." }
                issuanceService.finishSessionFail(session.token, true)
                IssueCredentialFailure(
                    result = IssueCredentialResult.UNKNOWN_ERROR,
                    description = "Unsupported credential type supplied.",
                ).throwError()
            }
        }
        issuanceService.finishSessionSuccess(session.token)
        return issuanceResult
    }

    private fun chooseIssuer(issuingDid: String): LdpCredentialIssuer {
        for (issuer in listOf(localLdpIssuer, acyPyLdpIssuer)) {
            if (issuer.canIssueForDid(issuingDid)) {
                return issuer
            }
        }
        IssueCredentialFailure(
            result = IssueCredentialResult.UNKNOWN_ERROR,
            description = "Unsupported issuing did",
        ).throwError()
    }
}
