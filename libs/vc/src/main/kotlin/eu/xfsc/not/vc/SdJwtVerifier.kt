package eu.xfsc.not.vc

import id.walt.sdjwt.JWTCryptoProvider
import id.walt.sdjwt.SDJwt
import id.walt.sdjwt.VerificationResult


/**
 * @author Mike Prechtl
 */
class SdJwtVerifier {

    fun verifySdJwt(undisclosedJwt: String, cryptoProvider: JWTCryptoProvider) : VerificationResult<SDJwt> {
        return SDJwt.verifyAndParse(undisclosedJwt, cryptoProvider)
    }
}
