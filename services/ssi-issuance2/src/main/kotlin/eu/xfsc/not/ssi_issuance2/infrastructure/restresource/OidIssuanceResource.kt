package eu.xfsc.not.ssi_issuance2.infrastructure.restresource

import eu.gaiax.notarization.api.issuance.*
import eu.xfsc.not.ssi_issuance2.application.OidIssuanceService
import jakarta.inject.Inject
import jakarta.ws.rs.PathParam

class OidIssuanceResource : OidIssuanceApi {

    @PathParam("issuance-token")
    lateinit var issuanceToken: String

    @Inject
    lateinit var oidIssuanceService: OidIssuanceService

    override fun verifyProof(verifyReq: VerifyProofRequest): VerifyProofSuccess {
        return oidIssuanceService.verifyProof(verifyReq, issuanceToken)
    }

    override fun issueCredential(issueReq: IssueCredentialRequest): IssueCredentialSuccess {
        return oidIssuanceService.issueCredential(issueReq, issuanceToken)
    }
}
