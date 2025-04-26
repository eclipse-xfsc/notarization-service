package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.profile.Profile
import eu.xfsc.not.ssi_issuance2.domain.RevocationService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import java.util.*

enum class LdpVcVersion(val schema: String) {
    W3CV1("https://www.w3.org/2018/credentials/v1"), // https://www.w3.org/TR/vc-data-model/
    W3CV2("https://www.w3.org/ns/credentials/v2"), // https://www.w3.org/TR/vc-data-model-2.0/
    ;

    fun context(): String {
        return schema
    }

    companion object {
        val contextByVersion = LdpVcVersion.entries.associateWith { it.schema }
        val versionByContext: Map<String, LdpVcVersion> = LdpVcVersion.entries.associateBy { it.schema }
    }
}

interface LdpCredentialBuilder {

    fun build(
        profile: Profile,
        issuerDid: String,
        issuanceDate: Instant,
        credData: ObjectNode,
        holderDid: String,
    ): Pair<ObjectNode, LdpVcVersion>
}

@ApplicationScoped
class LdpW3CCredentialBuilder: LdpCredentialBuilder {

    @RestClient
    private lateinit var revocationService: RevocationService

    @Inject
    lateinit var mapper: ObjectMapper

    override fun build(
        profile: Profile,
        issuerDid: String,
        issuanceDate: Instant,
        credData: ObjectNode,
        holderDid: String,
    ): Pair<ObjectNode, LdpVcVersion> {

        val credToIssue = credData.deepCopy()!!
        val credentialSubject = credToIssue.withObject("credentialSubject")
        val (ctx, version) = getContext(credToIssue)

        credToIssue.put("id", "urn:uuid:${UUID.randomUUID()}")
        credentialSubject.put("id", holderDid)
        credToIssue.replace("credentialSubject", credentialSubject)

        val currentIssuer = credToIssue.get("issuer")
        if (currentIssuer == null || currentIssuer.isTextual) {
            credToIssue.put("issuer", issuerDid)
        } else if (currentIssuer.isObject) {
            val issuer = currentIssuer as ObjectNode
            issuer.put("id", issuerDid)
        } else {
            throw IllegalArgumentException("The issuer of the credential has an unknown value.")
        }
        when (version) {
            LdpVcVersion.W3CV1 -> {
                credToIssue.put("issuanceDate", issuanceDate.toString())
            }
            LdpVcVersion.W3CV2 -> {
                credToIssue.put("validFrom", issuanceDate.toString())
            }
        }

        if (profile.isRevocable) {
            // TODO: use status list type depending on the used revocation list
            // add status list context if it is missing
            val slCtxVal = "https://w3id.org/vc/status-list/2021/v1"
            addIfMissing(ctx, slCtxVal)

            val statusEntry = revocationService.addStatusEntry(profile.id)
            credToIssue.replace("credentialStatus", statusEntry)
        }

        return credToIssue to version
    }

    private fun getContext(credToIssue: ObjectNode): Pair<ArrayNode, LdpVcVersion> {
        val ctx = credToIssue.get("@context")
        if (ctx == null) {
            throw IllegalArgumentException("A @context attribute is required to specify the version of the ")
        } else {
            when (ctx.nodeType) {
                JsonNodeType.STRING -> {
                    val schema = ctx.asText()!!
                    val schemaVersion = LdpVcVersion.versionByContext[schema]
                        ?: throw IllegalArgumentException("The @context of the credential has an textual value ${schema}.")
                    val result = mapper.createArrayNode()
                    result.add(schema)
                    credToIssue.replace("@context", result)
                    return Pair(result, schemaVersion)
                }

                JsonNodeType.ARRAY -> {
                    var schemaVersion: LdpVcVersion? = null
                    for (childNode in ctx) {
                        val currentValue = childNode.asText() ?: continue
                        val foundSchema = LdpVcVersion.versionByContext[currentValue]
                        if (foundSchema != null) {
                            schemaVersion = foundSchema
                            break;
                        }
                    }
                    if (schemaVersion == null) {
                        throw IllegalArgumentException("The @context of the credential did not identify a supported W3C version of LDP VC.")
                    }
                    return Pair(ctx as ArrayNode, schemaVersion)
                }

                else -> {
                    throw IllegalArgumentException("The @context of the credential has an unknown type ${ctx.nodeType}.")
                }
            }
        }
    }

    fun addIfMissing(node: ArrayNode, value: String) {
        if (node.find { it.isTextual && it.textValue() == value } == null) {
            node.add(value)
        }
    }
}
