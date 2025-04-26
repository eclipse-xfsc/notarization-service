package eu.xfsc.not.ssi_issuance2.infrastructure.restresource

import eu.gaiax.notarization.api.issuance.*
import eu.xfsc.not.ssi_issuance2.application.IssuanceService
import jakarta.inject.Inject
import jakarta.ws.rs.PathParam

class IssuanceResource : Issuance2Api {

    @Inject
    lateinit var issuanceService: IssuanceService

    override fun initService(req: ServiceInitRequest): ProfileIssuanceSpec {
        return issuanceService.initService(req)
    }

    override fun initSession(req: IssuanceInitRequest): IssuanceInitResponse {
        return issuanceService.initSession(req)
    }

    override fun cancelSession(@PathParam("issuance-token") token: String) {
        issuanceService.cancelSession(token)
    }

    override fun pruneTimeoutSessions() {
        issuanceService.pruneTimeoutSession()
    }

}
