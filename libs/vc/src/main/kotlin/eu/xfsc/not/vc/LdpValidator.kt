package eu.xfsc.not.vc

import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory
import com.danubetech.keyformats.jose.JWK
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK
import com.danubetech.verifiablecredentials.VerifiableCredential
import com.danubetech.verifiablecredentials.VerifiablePresentation
import com.danubetech.verifiablecredentials.jwt.FromJwtConverter
import com.danubetech.verifiablecredentials.jwt.JwtVerifiableCredential
import com.danubetech.verifiablecredentials.jwt.JwtVerifiablePresentation
import com.danubetech.verifiablecredentials.validation.Validation
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.JWTClaimsSet
import foundation.identity.jsonld.JsonLDException
import foundation.identity.jsonld.JsonLDObject
import id.walt.crypto.keys.jwk.JWKKey
import id.walt.crypto.utils.JsonUtils.printAsJson
import id.walt.did.dids.registrar.dids.DidKeyCreateOptions
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.suites.SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.verifier.LdVerifierRegistry
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URI
import java.security.GeneralSecurityException
import java.time.Instant
import java.util.*


fun resolveToDidUri(urlOrDidUri: URI): String {
    if (urlOrDidUri.scheme == "did") {
        return urlOrDidUri.toString()
    } else {
        TODO("implement document retrieval and evaluation")
        // the document can be a controller document or a JWK
    }
}

data class ResolvedKey(
    val did: String,
    val verificationMethod: String,
    val key: JWK,
    val didDoc: DidDocument,

)
fun resolveKey(didUri: String, keyIdentifier: String?): ResolvedKey {
    val didDoc = DidHandler.resolve(didUri)
        .map { jacksonObjectMapper().readValue<DidDocument>(it.toJsonObject().printAsJson()) }
        .getOrThrow()
    val vm = didDoc.let {
        if (keyIdentifier != null) {
            it.verificationMethod.find { it.id == keyIdentifier }
        } else {
            it.verificationMethod.firstOrNull()
        }
    } ?: throw IllegalStateException("DID Document does not contain an entry for the verification method.")
    val key = vm.extractPublicKeyJwk().getOrThrow()
    return ResolvedKey(didUri, vm.id, key, didDoc)
}

fun jwkToResolvedPair(jwk: com.nimbusds.jose.jwk.JWK): ResolvedKey = runBlocking {
    val key = JWKKey.importJWK(jwk.toJSONString()).getOrThrow()
    val resolveResult = DidHandler.registerDidKey(key, DidKeyCreateOptions(keyType = key.keyType))
    resolveKey(resolveResult.did, null)
}


class ProofValidator (
    private val statusValidator: StatusValidator = StatusValidator()
) {

    /**
     * Validate the cryptographic proof.
     * @param parent
     * @param proof
     * @return
     * @throws IllegalArgumentException if the proof algorithm is not supported
     * @throws JsonLDException
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Throws(
        IllegalArgumentException::class,
        JsonLDException::class,
        GeneralSecurityException::class,
        IOException::class
    )
    private fun validateProof(parent: JsonLDObject?, proof: LdProof, domain: String? = null, challenge: String? = null, validationDate: Instant = Instant.now()): ProofValidationResult {
        // obtain verifier
        val type: String = proof.getType()
        val sigSuite: SignatureSuite = SignatureSuites.findSignatureSuiteByTerm(type)
            ?: throw IllegalArgumentException("Unsupported signature suite: $type")
        val ldVerifier = LdVerifierRegistry.getLdVerifierBySignatureSuite(sigSuite)

        // resolve did and get public key used in verification
        val keyIdentifier = proof.verificationMethod.toString()
        val didUri = keyIdentifier.substringBefore('#')
        val keyResult = resolveKey(didUri, keyIdentifier)

        // prepare verifier
//        val jwk = JWK.fromJson(key.exportJWK())
        val jwk = keyResult.key
        val keyTypeName = KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk)
        val keyAlgCandidate = sigSuite.findJwsAlgorithmsForKeyTypeName(keyTypeName)
        // use first method from the list. The only ones which need special handling are JWS and secp256k1
        // I suppose with JWS, we simply have to look at the jws value in the proof
        val keyAlg = keyAlgCandidate[0]
        val keyVerifier = PublicKeyVerifierFactory.publicKeyVerifierForJWK(jwk, keyAlg)
        ldVerifier.verifier = keyVerifier

        // check date
        if (proof.created.toInstant().isAfter(validationDate)) {
            throw IllegalStateException("Proof creation date is in the future")
        }

        // check domain & challenge
        if (proof.domain != domain) {
            throw IllegalStateException("Domain does not match")
        }
        if (proof.challenge != challenge) {
            throw IllegalStateException("Challenge does not match")
        }

        // perform validation
        val proofValid = ldVerifier.verify(parent, proof)
        if (proofValid) {
            return ProofValidationResult(did = didUri, keyId = keyIdentifier)
        } else {
            throw IllegalStateException("Proof is not valid")
        }
    }

    private fun validateJwtProof(proof: LdProof?, jws: JWSObject, claims: JWTClaimsSet, holder: String, domain: String? = null, challenge: String? = null, validationDate: Instant = Instant.now()): JwtProofValidationResult {
        // check created date
        val nbf: Date? = proof?.created ?: claims.notBeforeTime
        if (nbf == null) {
            throw IllegalStateException("Proof creation date is missing")
        }
        if (nbf.toInstant().isAfter(validationDate)) {
            throw IllegalStateException("Proof creation date is in the future")
        }
        val iat = claims.issueTime
        if (iat != null && iat.toInstant().isAfter(validationDate)) {
            throw IllegalStateException("Proof creation date is in the future")
        }
        // check expiration if present
        val expires: Date? = claims.expirationTime
        if (expires != null && expires.toInstant().isBefore(validationDate)) {
            throw IllegalStateException("Proof has expired")
        }

        // check domain & challenge
        val proofDomains: List<String> = proof?.domain?.let { listOf(it) } ?: claims.audience ?: emptyList()
        if (domain != null && !proofDomains.contains(domain)) {
            throw IllegalStateException("Domain does not match")
        }
        val proofChallenge: String? = proof?.challenge ?: claims.getStringClaim("nonce")
        if (proofChallenge != challenge) {
            throw IllegalStateException("Challenge does not match")
        }

        val proofResult = JwtValidator().validateJws(jws, holder, false)
        return proofResult
    }


    fun validate(vcJwt: JwtVerifiableCredential, validationDate: Instant = Instant.now()): VcValidationResult {
        val vc = FromJwtConverter.fromJwtVerifiableCredential(vcJwt)
        Validation.validate(vc)
        val issuer = vc.issuer
        val issuerDid = resolveToDidUri(issuer)

        val jws = vcJwt.jwsObject
        val claims = vcJwt.payload

        val proof: LdProof? = vc.ldProof
        val proofResult = validateJwtProof(
            proof,
            jws,
            claims,
            holder = issuerDid,
            validationDate = validationDate
        )

        return validateVcContent(vc, proofResult, validationDate)
    }

    @Throws(
        IllegalStateException::class,
        IllegalArgumentException::class,
        JsonLDException::class,
        GeneralSecurityException::class,
        IOException::class
    )
    fun validate(vc: VerifiableCredential, validationDate: Instant = Instant.now()): VcValidationResult {
        Validation.validate(vc)
        val proof = vc.ldProof
        val proofResult = validateProof(vc, proof, validationDate = validationDate)

        return validateVcContent(vc, proofResult, validationDate)
    }

    private fun validateVcContent(
        vc: VerifiableCredential,
        proofResult: ProofValidationResult,
        validationDate: Instant
    ): VcValidationResult {
        // check against issuer
        val issuerDid = resolveToDidUri(vc.issuer)
        if (issuerDid != proofResult.did) {
            throw IllegalStateException("VC issuer DID does not match the DID in the proof")
        }

        // check dates
        if (vc.issuanceDate.toInstant().isAfter(validationDate)) {
            throw IllegalStateException("Issuance date is in the future")
        }
        if ((vc.expirationDate?.toInstant()?:Instant.MAX).isBefore(validationDate)) {
            throw IllegalStateException("Credential has expired")
        }

        // check statuslist
        val statusResult = vc.credentialStatus?.let {
            statusValidator.validateStatus(it)
        }

        val subjectDid = resolveToDidUri(vc.credentialSubject.id)
        return VcValidationResult(proofValidation = proofResult, vc = vc, revocationValidation = statusResult, issuerDid = proofResult.did, subjectDid = subjectDid)
    }

    fun validate(vpJwt: JwtVerifiablePresentation, domain: String? = null, challenge: String? = null, validationDate: Instant = Instant.now(), requireVc: Boolean = true): VpValidationResult {
        val vp = FromJwtConverter.fromJwtVerifiablePresentation(vpJwt)
        Validation.validate(vp, requireVc)
        val holder = vp.holder
        val holderDid = resolveToDidUri(holder)

        val jws = vpJwt.jwsObject
        val claims = vpJwt.payload

        val proof: LdProof? = vp.ldProof
        val proofResult = validateJwtProof(
            proof,
            jws,
            claims,
            holder = holderDid,
            domain = domain,
            challenge = challenge,
            validationDate = validationDate
        )

        return validateVpContent(vp, proofResult, validationDate)
    }

    fun validate(vp: VerifiablePresentation, domain: String? = null, challenge: String? = null, validationDate: Instant = Instant.now(), requireVc: Boolean = true): VpValidationResult {
        Validation.validate(vp, requireVc)
        val proof = vp.ldProof
        val proofResult = validateProof(
            vp,
            proof,
            domain = domain,
            challenge = challenge,
            validationDate = validationDate
        )

        return validateVpContent(vp, proofResult, validationDate)
    }

    private fun validateVpContent(
        vp: VerifiablePresentation,
        proofResult: ProofValidationResult,
        validationDate: Instant
    ): VpValidationResult {
        // check vp holder against proof did
        val holderDid = resolveToDidUri(vp.holder)
        if (holderDid != proofResult.did) {
            throw IllegalStateException("VP holder DID does not match the DID in the proof")
        }

        val vcValidations: MutableList<VcValidationResult> = mutableListOf()
        for (vc in vp.verifiableCredentials) {
            val vcResult = validate(vc, validationDate)

            // check that the subject of the VC is the same as the issuer of the proof
            if (vcResult.subjectDid != proofResult.did) {
                throw IllegalStateException("VC subject does not match the DID in the proof")
            }

            vcValidations.add(vcResult)
        }

        for (vcJwtStr in vp.jwtVerifiableCredentialStrings) {
            val vcJwt = JwtVerifiableCredential.fromCompactSerialization(vcJwtStr)
            val vcResult = validate(vcJwt, validationDate)

            // check that the subject of the VC is the same as the issuer of the proof
            if (vcResult.subjectDid != proofResult.did) {
                throw IllegalStateException("VC subject does not match the DID in the proof")
            }

            vcValidations.add(vcResult)
        }

        return VpValidationResult(proofValidation = proofResult, vcValidations = vcValidations)

    }

}


/**
 * Allows to check for the overall validity of the result.
 */
interface ResultValidity {
    val valid: Boolean
}

open class ProofValidationResult(
    val did: String,
    val keyId: String,
) : ResultValidity {
    override val valid: Boolean
        get() = true
}

data class VcValidationResult(
    val proofValidation: ProofValidationResult,
    val revocationValidation: RevocationValidationResult? = null,
    val vc: VerifiableCredential,
    val issuerDid: String,
    val subjectDid: String,
) : ResultValidity {
    override val valid: Boolean
        get() = proofValidation.valid && revocationValidation?.valid ?: true
}

data class VpValidationResult(
    val proofValidation: ProofValidationResult,
    val vcValidations: List<VcValidationResult>,
) : ResultValidity {
    override val valid: Boolean
        get() = proofValidation.valid && vcValidations.all { it.valid }
}
