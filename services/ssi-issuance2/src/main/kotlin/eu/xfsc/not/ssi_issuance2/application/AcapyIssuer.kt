package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.KeyType
import eu.gaiax.notarization.api.issuance.SignatureType
import eu.gaiax.notarization.api.profile.Profile
import eu.xfsc.not.ssi_issuance2.domain.CryptoMechanisms
import eu.xfsc.not.ssi_issuance2.domain.LdpCredentialIssuer
import eu.xfsc.not.ssi_issuance2.domain.RevocationService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import mu.KotlinLogging
import openapi.acapy.api.VcApiApi
import openapi.acapy.api.WalletApi
import openapi.acapy.model.DIDCreate
import openapi.acapy.model.DIDCreateOptions
import openapi.acapy.model.IssuanceOptions
import openapi.acapy.model.IssueCredentialRequest
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

private val supportedTypes = mapOf(
    KeyType.ED25519 to DIDCreateOptions.KeyTypeEnum.ED25519,
    KeyType.BLS12381G2 to DIDCreateOptions.KeyTypeEnum.BLS12381G2
)

@ApplicationScoped
class AcapyIssuer : LdpCredentialIssuer {

    @RestClient
    private lateinit var vcApi: VcApiApi

    override val credentialMechanisms = CryptoMechanisms(
        keyTypes = supportedTypes.keys,
        signatureTypes = setOf(
            SignatureType.ED25519SIGNATURE2018,
            SignatureType.ED25519SIGNATURE2020,
            SignatureType.BBSBLSSIGNATURE2020,
            SignatureType.BBSBLSSIGNATUREPROOF2020,
        ),
    )

    @Inject
    lateinit var ldpCredentialBuilder: LdpCredentialBuilder

    override fun createCredential(
        profile: Profile,
        issuerDid: String,
        signatureType: SignatureType,
        issuanceTimestamp: Instant,
        subjectDid: String,
        credentialData: ObjectNode,
    ): JsonNode {
        val (preparedCredentialData, version) = ldpCredentialBuilder.build(
            profile,
            issuerDid,
            issuanceTimestamp,
            credentialData,
            subjectDid,
        )
        if (version != LdpVcVersion.W3CV1) {
            log.warn { "Attempting to create credentials using AcaPy given credential data with unsupported schema: $version" }
        }
        log.debug { "Create cred with signature algorithm: ${signatureType.value}" }
        log.debug { "Create cred with data: ${preparedCredentialData}" }

        val opts = IssuanceOptions().apply {
            type(signatureType.value)
        }
        val issueReq = IssueCredentialRequest().apply {
            credential = preparedCredentialData
            options = opts
        }
        return vcApi.vcCredentialsIssuePost(issueReq).verifiableCredential
    }

    @Inject
    lateinit var acaPyKeyManager: AcaPyKeyManager

    override fun canIssueForDid(did: String): Boolean {
        return acaPyKeyManager.hasDid(did)
    }
}


@ApplicationScoped
class AcaPyKeyManager: KeyInit {

    @RestClient
    private lateinit var walletApi: WalletApi

    override fun initKeypair(keyType: KeyType): String {
        log.debug { "Init call for keyType: keyType" }
        val opts = DIDCreateOptions().apply {
            this.keyType = supportedTypes[keyType] ?: throw IllegalArgumentException("Unsupported key type: $keyType")
        }
        val req = DIDCreate().apply {
            method = "key"
            options = opts
        }

        return walletApi.walletDidCreatePost(req).result.did
    }

    override fun hasDid(did: String): Boolean {
        val didList = walletApi.walletDidGet(did, null, null, null, null)

        return didList?.results?.isNotEmpty() ?: false
    }
}
