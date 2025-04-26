package eu.xfsc.not.oid4vci

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ellog.uuid.UUID
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.api.oid4vci.model.TokenTypes.Bearer
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.JsonString
import org.eclipse.microprofile.jwt.Claims
import org.eclipse.microprofile.jwt.JsonWebToken
import java.time.Instant


const val AccessTokenAud = "oid4vci:at"

@ApplicationScoped
class PreAuthTokenRequestService {

    @Inject
    lateinit var oidConfig: Oid4vciConfig
    @Inject
    lateinit var authCodeVal: PreAuthCodeValidator
    @Inject
    lateinit var atGen: AccessTokenGenerator
    @Inject
    lateinit var nonceManager: NonceManager

    fun processTokenRequest(tokenReq: Oid4VciTokenRequest): Oid4VciTokenResponse {
        val codeData = validateRequest(tokenReq)

        // prepare access token values
        val atLifetime = oidConfig.atLifetime()
        val expiresAt = Instant.now().plus(atLifetime)
        val jti = UUID.createTimeV7().toString()
        // issue token and nonce
        val accessTokenData = buildTokenData(codeData)
        val accessToken = atGen.createCode(accessTokenData, jti, expiresAt)
        val nonce = nonceManager.registerNonce(jti, expiresAt)
        //val authDetails: List<AuthorizationDetails> = buildAuthDetails(accessTokenData)

        return Oid4VciTokenResponse(
            oauthTokenResponse = OauthTokenResponse(
                tokenType = Bearer,
                accessToken = accessToken,
                expiresIn = atLifetime.seconds,
            ),
            cNonce = nonce,
            cNonceExpiresIn = atLifetime.seconds,
            //authorizationDetails = authDetails,
        )
    }

    private fun buildAuthDetails(codeData: AccessTokenData): List<AuthorizationDetails> {
        // build authorization details based on available profile
        return codeData.profiles.map {
            ReferencedAuthorizationDetails(it).apply {
                // set credential_identifiers to the same value as credential_configuration_id as our profiles have no notion of credential_identifiers
                // that means we pretend that it is the same, as in the credential request only the identifier can be given
                // see code using ReferencedCredentialRequest.credentialIdentifier to understand how the values are used
                credentialIdentifiers = listOf(it)
            }
        }
    }

    private fun buildTokenData(codeData: PreAuthCodeData): AccessTokenData {
        return AccessTokenData(
            callbackUrl = codeData.callbackUrl,
            issueSession = codeData.issueSession,
            profiles = codeData.profiles,
        )
    }

    private fun validateRequest(tokenReq: Oid4VciTokenRequest): PreAuthCodeData {
        // get preauth code and validate it
        val preAuthCodeStr = tokenReq.preAuthorizedCode
            ?: OauthErrorResponse(OauthErrorResponse.InvalidRequest, "preauth code is missing.").throwError()
        val preAuthCode = authCodeVal.validatePreauthCode(preAuthCodeStr)
        val codeData = preAuthCode.getPreAuthCodeData()

        // check if there is a transaction in the preAuthCode, then eval the transaction code
        val tokenTxCode = codeData.txCode
        if (tokenTxCode != null) {
            // txCode must be present
            val receivedTxCode = tokenReq.txCode
                ?: OauthErrorResponse(OauthErrorResponse.InvalidRequest, "Transaction code is missing.").throwError()
            if (receivedTxCode != tokenTxCode) {
                // txCode is valid
                OauthErrorResponse(
                    OauthErrorResponse.InvalidGrant, "Transaction code is invalid."
                ).throwError()
            }
        } else {
            // txCode is forbidden
            tokenReq.txCode?.let {
                OauthErrorResponse(OauthErrorResponse.InvalidRequest, "Transaction code is not expected.").throwError()
            }
        }

        return codeData
    }
}


@ApplicationScoped
class AccessTokenGenerator {

    @Inject lateinit var oidConfig: Oid4vciConfig

    fun createCode(atData: AccessTokenData, jti: String, expiresAt: Instant): String {
        val claims = Jwt.claims(atData.asJsonObject())
            .audience(AccessTokenAud)
            .claim(Claims.jti, jti)
            .expiresAt(expiresAt)

        if (oidConfig.encryptTokens()) {
            return claims.innerSign().encrypt()
        } else {
            return claims.sign()
        }
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
class AccessTokenData (
    val callbackUrl: String,
    val issueSession: String,
    val profiles: List<String>,
)

fun AccessTokenData.asJsonObject(om: ObjectMapper = jacksonObjectMapper()): Map<String, Any> {
    return om.convertValue(this)
}

fun JsonWebToken.getAcessTokenData(): AccessTokenData {
    // plain variant
    return AccessTokenData(
        callbackUrl = getClaim("callbackUrl"),
        issueSession = getClaim("issueSession"),
        profiles = getClaim<List<JsonString>>("profiles").map { it.string},
    )
}
