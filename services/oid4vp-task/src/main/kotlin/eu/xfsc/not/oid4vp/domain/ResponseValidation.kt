package eu.xfsc.not.oid4vp.domain

import eu.xfsc.not.api.util.JsonValueEnum
import eu.xfsc.not.oid4vp.model.AuthRequestObject
import eu.xfsc.not.oid4vp.model.AuthResponseRequest
import eu.xfsc.not.oid4vp.rest.SuccessResponse
import eu.xfsc.not.oid4vp.rest.ValidationResult
import eu.xfsc.not.vc.VcValidationResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class ResponseValidation {

    @Inject
    lateinit var trainValidator: TrainValidation
    @Inject
    lateinit var ldpVerifier: LdpVerifier
    @Inject
    lateinit var ldpJwtVerifier: LdpJwtVerifier

    @Throws(ResponseValidationException::class)
    fun validateAuthResponseRequest(authReq: AuthRequestObject, authResponseReq: AuthResponseRequest) {
        // TODO: implement checking of presentation submission and vp type
    }


    @Throws(ResponseValidationException::class)
    fun validateVp(
        profileId: String,
        taskName: String,
        reqObj: AuthRequestObject,
        authReq: AuthResponseRequest
    ): SuccessResponse {

        val vpObj = authReq.vpToken
        val nonce = reqObj.nonce
        val clientId = reqObj.clientId

        // TODO: check format and decide which verifier to take based on that
        val ldpValidationResult = if (vpObj.isTextual) {
            ldpJwtVerifier.validateVp(vpObj, clientId, nonce)
        } else {
            ldpVerifier.validateVp(vpObj, clientId, nonce)
        }

        val resultList = ldpValidationResult.vcValidations.map { vcValidationResult: VcValidationResult ->
            var result = ValidationResult(
                validationResult = vcValidationResult,
                trainValidationResult = null
            )
            if (trainValidator.needsTrainCheck()) {
                val trainResult = trainValidator.checkDidTrust(profileId, taskName, vcValidationResult.issuerDid)
                result.trainValidationResult = trainResult
            }
            result
        }

        val resp = SuccessResponse(
            validationResults = resultList
        )
        return resp
    }

}

class ResponseValidationException (
    val failure: ResponseValidationErrorCode,
    message: String?,
    val validationException: Throwable? = null,
) : Exception(message)

enum class ResponseValidationErrorCode(override val value: String) : JsonValueEnum {
    InvalidScope("invalid_scope"),
    InvalidRequest("invalid_request"),
    InvalidClient("invalid_client"),
    VpFormatsNotSupported("vp_formats_not_supported"),
    InvalidPresentationDefinitionUri("invalid_presentation_definition_uri"),
    InvalidPresentationDefinitionReference("invalid_presentation_definition_reference"),

    InvalidVp("invalid_vp"),
    InvalidVpFormat("invalid_vp_format"),
    InvalidVpNonce("invalid_vp_nonce"),

    InvalidTrustPointers("invalid_trust_pointers"),
    InvalidTrainResult("invalid_train_result"),
    InvalidTrainTrust("invalid_train_trust")
}
