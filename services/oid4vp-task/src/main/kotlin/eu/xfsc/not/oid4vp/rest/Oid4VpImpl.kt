package eu.xfsc.not.oid4vp.rest

import eu.xfsc.not.oid4vp.domain.RequestObjectRepo
import eu.xfsc.not.oid4vp.domain.ResponseValidation
import eu.xfsc.not.oid4vp.domain.ResponseValidationException
import eu.xfsc.not.oid4vp.model.AuthResponseRequest
import eu.xfsc.not.oid4vp.model.AuthResponseResponse
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import mu.KotlinLogging

private val logger = KotlinLogging.logger {  }

@Path("oid4vp")
class Oid4VpImpl {

    @Inject
    lateinit var reqObjRepo: RequestObjectRepo
    @Inject
    lateinit var respVal: ResponseValidation
    @Inject
    lateinit var cbBuilder: CallbackClientBuilder

    @Path("/auth-request")
    @GET
    @Produces("application/oauth-authz-req+jwt")
    @Transactional
    fun getRequestObject(@QueryParam("id") authReqId: String): String {
        var reqObj = reqObjRepo.getRequestObjectJwt(authReqId)
        return reqObj
    }

    @Path("/auth-response")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    @Transactional
    fun authResponse(@BeanParam authResponseReq: AuthResponseRequest): AuthResponseResponse {
        val authReqId = authResponseReq.state ?: ""
        var reqObj = reqObjRepo.getRequestObject(authReqId)
        val (success, failure) = reqObjRepo.getCallbacks(authReqId).let { (success, failure) ->
            cbBuilder.buildClients(success, failure)
        }

        try {
            // perform validations
            val (profileId, taskName) = reqObjRepo.getTaskReference(authReqId)
            respVal.validateAuthResponseRequest(reqObj, authResponseReq)

            val vpData = respVal.validateVp(profileId, taskName, reqObj, authResponseReq)
            success.send(vpData)

            return AuthResponseResponse()
        } catch (e: ResponseValidationException) {
            logger.debug(e) { "Failed to validate VP." }

            // call error handler
            failure.send(FailureResponse(
                errorCode = e.failure.value,
                errorDescription = e.message,
            ))

            throw WebApplicationException(e.message, Response.Status.BAD_REQUEST)
        } finally {
            // remove request object from DB once the process is concluded
            reqObjRepo.removeRequestObject(authReqId)
        }
    }
}

