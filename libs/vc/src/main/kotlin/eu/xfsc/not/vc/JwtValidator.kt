package eu.xfsc.not.vc

import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory
import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.SignedJWT
import info.weboftrust.ldsignatures.adapter.JWSVerifierAdapter
import java.time.Duration
import java.time.Instant

class JwtValidator {
    fun validateOidJwtTokenProof(
        jwt: String,
        holder: String?,
        type: String? = "openid4vci-proof+jwt",
        issuer: String?,
        aud: String? = null,
        nonce: String? = null,
        validationDate: Instant = Instant.now(),
        maxAge: Duration? = Duration.ofSeconds(60),
    ): JwtProofValidationResult {
        val jwtObj = SignedJWT.parse(jwt)
        val claims = jwtObj.jwtClaimsSet

        // check created date
        val jwtNbf = claims.notBeforeTime?.toInstant()
        if (jwtNbf != null && jwtNbf.isAfter(validationDate)) {
            throw IllegalStateException("Proof creation date is in the future")
        }
        val jwtIat = claims.issueTime?.toInstant() ?: throw IllegalStateException("Proof issue time is missing")
        if (jwtIat.isAfter(validationDate)) {
            throw IllegalStateException("Proof creation date is in the future")
        }
        // check expiration if present
        val jwtExpires = claims.expirationTime?.toInstant()
        if (jwtExpires != null) {
            if (jwtExpires.isBefore(validationDate)) {
                throw IllegalStateException("Proof has expired")
            }
        } else if (maxAge != null) {
            // when no expiration is given, check that the proof is not older than a certain value
            if (Duration.between(jwtIat, validationDate).compareTo(maxAge) > 0) {
                throw IllegalStateException("Proof is too old")
            }
        }

        // check aud & nonce
        val jwtAud: List<String> = claims.audience ?: emptyList()
        if (aud != null && !jwtAud.contains(aud)) {
            throw IllegalStateException("Audience does not match")
        }
        val jwtNonce: String? = claims.getStringClaim("nonce")
        if (jwtNonce != nonce) {
            throw IllegalStateException("Nonce does not match")
        }

        val jwtIss: String? = claims.issuer
        if (issuer != null && issuer != jwtIss) {
            throw IllegalStateException("Issuer does not match")
        }

        // check type of JWT
        val jwtType = jwtObj.header.type?.type
        if (type != null && jwtType != type) {
            throw IllegalStateException("JWT type does not match")
        }

        val proofResult = validateJws(jwtObj, holder, true)
        return proofResult
    }

    fun validateJws(jws: JWSObject, holder: String?, requireKeyInJws: Boolean): JwtProofValidationResult {
        val alg = jws.header.algorithm

        val resolvedKey = try {
            findKeyInJws(jws, holder)
        } catch (e: Exception) {
            if (!requireKeyInJws && holder != null) {
                resolveKey(holder, null)
            } else {
                throw e
            }
        }

        // perform validation
        val keyVerifier = PublicKeyVerifierFactory.publicKeyVerifierForJWK(resolvedKey.key, alg.name)
        val jwsVerifier = JWSVerifierAdapter(keyVerifier, alg)
        val proofValid = jwsVerifier.verify(jws.header, jws.signingInput, jws.signature)

        if (proofValid) {
            return JwtProofValidationResult(did = resolvedKey.did, keyId = resolvedKey.verificationMethod, jwk = resolvedKey.key.toJson())
        } else {
            throw IllegalStateException("JWS proof is not valid")
        }
    }

    private fun findKeyInJws(jws: JWSObject, holder: String?): ResolvedKey {
        jws.header.jwk?.let {
            return jwkToResolvedPair(it)
        }

        jws.header.keyID?.let { kid ->
            // supplied holder or take from kid
            val holder2 = holder ?: kid.let {
                it.substringBefore("#")
            }
            return resolveKey(holder2, kid.takeIf { it.contains("#") })
        }

        throw IllegalArgumentException("No jwk or kid found in JWS header")
    }

}

open class JwtProofValidationResult(
    did: String,
    keyId: String,
    val jwk: String,
) : ProofValidationResult(did, keyId)
