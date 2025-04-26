package eu.xfsc.not.oid4vci

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.xfsc.not.api.oid4vci.Oid4VciOfferApi
import eu.xfsc.not.api.oid4vci.model.*
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo
import io.smallrye.jwt.auth.principal.JWTParser
import io.smallrye.jwt.auth.principal.ParseException
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.json.JsonString
import mu.KotlinLogging
import org.eclipse.microprofile.jwt.JsonWebToken


private val logger = KotlinLogging.logger {}

const val PreAuthCodeAud = "oid4vci:preauth"

class OfferApiImpl : Oid4VciOfferApi {

    @Inject lateinit var oidcConfigProv: OidcConfigProvider
    @Inject lateinit var codeGen: PreAuthCodeGenerator

    override fun createCredentialOffer(profiles: List<String>, issueSession: String, callbackUrl: String): String {
        val codeData = PreAuthCodeData(
            issueSession = issueSession,
            profiles = profiles,
            callbackUrl = callbackUrl,
        )
        val preauthCode = codeGen.createCode(codeData)

        val md = oidcConfigProv.buildVciConfig()
        val offer = CredentialOffer(
            credentialIssuer = md.credentialIssuer,
            credentialConfigurationIds = profiles,
            grants = OfferGrants(
                preAuthCodeGrantOffer = PreAuthCodeGrantOffer(preAuthorizedCode = preauthCode)
            )
        )

        val offerUri = offer.toOfferByValueUri()
        return offerUri
    }

}


@ApplicationScoped
class PreAuthCodeGenerator {

    @Inject lateinit var oidConfig: Oid4vciConfig

    fun createCode(codeData: PreAuthCodeData): String {
        val codeDataJson = codeData.asJsonObject()
        val claims = Jwt.claims(codeDataJson)
            .audience(PreAuthCodeAud)
            .expiresIn(oidConfig.codeLifetime())

        if (oidConfig.encryptTokens()) {
            return claims.innerSign().encrypt()
        } else {
            return claims.sign()
        }
    }
}

@ApplicationScoped
class PreAuthCodeValidator {

    @Inject lateinit var oidConfig: Oid4vciConfig
    @Inject lateinit var jwtParser: JWTParser
    @Inject lateinit var origAuthCtx: JWTAuthContextInfo
    @Inject lateinit var jtiCheck: JtiCheck

    fun validatePreauthCode(code: String, expectedAudience: Set<String> = setOf(PreAuthCodeAud)): JsonWebToken {
        try {
            // prepare context to check audience
            val authCtx = JWTAuthContextInfo(origAuthCtx).also {
                it.expectedAudience = expectedAudience
            }
            // decrypt, validate and parse the token
            val jwt = jwtParser.parse(code, authCtx)

            // check jti to prevent replays
            val jti = jwt.tokenID
            jtiCheck.checkJti(jti)
            jtiCheck.markJtiAsUsed(jti, jwt.expirationTime.toExpirationDate())

            return jwt
        } catch (e: ParseException) {
            logger.error(e) { "Failed to process decrypt preauth_code" }

            OauthErrorResponse(
                error = OauthErrorResponse.InvalidGrant,
                errorDescription = e.message
            ).throwError()
        }
    }

}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
@JsonInclude(Include.NON_ABSENT)
class PreAuthCodeData (
    val callbackUrl: String,
    val issueSession: String,
    val profiles: List<String>,
    val txCode: String? = null,
) {
    // make jsonb happy
//    constructor() : this("", "", emptyList())
}

fun PreAuthCodeData.asJsonObject(om: ObjectMapper = jacksonObjectMapper()): Map<String, Any> {
    return om.convertValue(this)
}

fun JsonWebToken.getPreAuthCodeData(): PreAuthCodeData {
    // TODO: improve code converting claims
    // the claims are in jakarta Json format and can not be easily converted with jackson
    // here are three methods to convert the data

    // jsonb variant
//    val jsonb = JsonbBuilder.create()
//    val claims: Map<String, Any> = this.claimNames.associate { it to this.getClaim(it) }
//    val ser = jsonb.toJson(claims)
//    return jsonb.fromJson(ser, PreAuthCodeData::class.java)

    // jackson variant
//    val jws = JsonWebSignature.fromCompactSerialization(this.rawToken) as JsonWebSignature
//    return om.readValue(jws.getUnverifiedPayload())

    // plain variant
    return PreAuthCodeData(
        callbackUrl = getClaim("callbackUrl"),
        issueSession = getClaim("issueSession"),
        profiles = getClaim<List<JsonString>>("profiles").map { it.string},
        txCode = getClaim("txCode"),
    )
}
