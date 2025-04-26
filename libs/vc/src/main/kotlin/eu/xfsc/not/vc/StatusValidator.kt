package eu.xfsc.not.vc

import com.danubetech.verifiablecredentials.VerifiableCredential
import com.danubetech.verifiablecredentials.credentialstatus.CredentialStatus
import com.danubetech.verifiablecredentials.credentialstatus.StatusList2021Entry
import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialKeywords
import eu.xfsc.not.vc.status.StatusBitSet
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration


class StatusValidator (
    private val client: HttpClient = HttpClient.newBuilder().also {
        it.followRedirects(HttpClient.Redirect.NORMAL)
    }.build()
) {

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun validateStatus(cs: CredentialStatus): RevocationValidationResult {
        return when (cs.type) {
            VerifiableCredentialKeywords.JSONLD_TERM_STATUS_LIST_2021_ENTRY -> {
                validateStatusList2021(StatusList2021Entry.fromJsonLDObject(cs))
            }
            else -> {
                throw IllegalArgumentException("Unsupported credential status type: ${cs.type}")
            }
        }
    }

    @Throws(IllegalStateException::class)
    fun validateStatusList2021(entry: StatusList2021Entry): RevocationValidationResult {
        val credStr = retrieveCredential(entry.statusListCredential)
        val vc = VerifiableCredential.fromJson(credStr)
        val vcResult = ProofValidator().validate(vc)
        val vcSub = vc.credentialSubject

        if (entry.statusPurpose != vcSub.claims["statusPurpose"]) {
            throw IllegalStateException("Status purpose mismatch")
        }
        val statusList = vcSub.claims["encodedList"]?.let {
            StatusBitSet.decodeBitset(it as String)
        }
        if (statusList == null) {
            throw IllegalStateException("Status list not found in credential subject")
        }
        val bitSet = statusList.status(entry.statusListIndex.toInt())

        return RevocationValidationResult(
            vcValidation = vcResult,
            credentialActive = bitSet?.not() ?: false,
            inList = bitSet != null,
        )
    }

    @Throws(IllegalStateException::class)
    fun retrieveCredential(credUri: URI): String? {
        val request = HttpRequest.newBuilder()
            .uri(credUri)
            .timeout(Duration.ofSeconds(60))
            .GET()
            .build()
        val response = client.send<String>(request, BodyHandlers.ofString())
        return when (response.statusCode()) {
            200 -> {
                response.body()
            }
            else -> {
                throw IllegalStateException("Status list not found")
            }
        }
    }

}

data class RevocationValidationResult(
    val vcValidation: VcValidationResult,
    val credentialActive: Boolean,
    val inList: Boolean,
) : ResultValidity {
    override val valid: Boolean
    get() = vcValidation.valid && credentialActive && inList
}
