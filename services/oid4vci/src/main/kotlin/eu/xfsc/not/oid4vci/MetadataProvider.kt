package eu.xfsc.not.oid4vci

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.api.profile.ProfileServiceHttpInterface
import eu.xfsc.not.api.oid4vci.Oidc4VciApi
import eu.xfsc.not.api.oid4vci.model.*
import eu.xfsc.not.api.oid4vci.model.GrantTypes.PreAuthCodeName
import io.quarkus.cache.CacheResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.UriBuilder
import mu.KotlinLogging
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.lang.reflect.Method
import java.net.URI
import kotlin.reflect.jvm.javaMethod


private val logger = KotlinLogging.logger {}


@RegisterRestClient(configKey = "profile-api")
interface ProfileServiceClient : ProfileServiceHttpInterface

@ApplicationScoped
class OidcConfigProvider {

    @Inject
    lateinit var om: ObjectMapper
    @Inject
    lateinit var oidcConfig: Oid4vciConfig
    @Inject
    lateinit var profileProvider: ProfileProvider

    val baseUri: URI
        get() {
            return oidcConfig.issuerUrl()
        }

    private fun buildOidcPath(method: Method?): String {
        val ub = UriBuilder.fromUri(baseUri)
        return ub.path(method).build().toString()
    }

    fun buildVciConfig(): CredentialIssuerMetadata {
        // resolve profiles to credential definitions
        val credsSupported = buildSupportedCredentials()

        return CredentialIssuerMetadata(
            // core params
            credentialIssuer = baseUri.toString(),

            // endpoints
            credentialEndpoint = buildOidcPath(Oidc4VciApi::oidCredential.javaMethod),
            // TODO: activate when the endpoint is supported
            //deferredCredentialEndpoint = buildOidcPath(Oidc4VciApi::oidDeferredCredential.javaMethod),

            // operational parameters
            credentialResponseEncryption = getCredentialResponseEncryptionSupport(),

            // VC definitions
            credentialIdentifiersSupported = true,
            credentialConfigurationsSupported = credsSupported,

            // informational
            display = getIssuerDisplay(),
        )
    }

    fun getIssuerDisplay(): List<IssuerDisplay> {
        val rawVal: String = oidcConfig.issuerDisplay().orElseGet {
            """
                [{
                  "name": "XFSC Credential Issuer",
                  "locale": "en-US",
                  "logo": {
                    "uri": "${oidcConfig.issuerUrl().toString() + "/assets/xfsc-logo.png"}",
                    "alt_text": "XFSC Logo"
                  }
                }]
            """.trimIndent()
        }

        if (rawVal.isBlank()) {
            return emptyList()
        }

        return try {
            om.readValue(rawVal)
        } catch (e: Exception) {
            logger.error(e) { "Failed to marshal issuer display value. Fix the configuration." }
            emptyList()
        }

    }

    fun getCredentialResponseEncryptionSupport(): CredentialResponseEncryptionSupport? {
        return CredentialResponseEncryptionSupport(
            // https://www.rfc-editor.org/rfc/rfc7518#section-4.1
            algValuesSupported = listOf("RSA1_5", "RSA-OAEP", "RSA-OAEP-256", "ECDH-ES", "ECDH-ES+A128KW", "ECDH-ES+A192KW", "ECDH-ES+A256KW"),
            // https://www.rfc-editor.org/rfc/rfc7518#section-5.1
            encValuesSupported = listOf("A128CBC-HS256", "A192CBC-HS384", "A256CBC-HS512", "A128GCM", "A192GCM", "A256GCM"),
            encryptionRequired = false,
        )
    }

    fun buildOauthConfig(): OauthProviderMetadata {
        return OauthProviderMetadata(
            issuer = baseUri.toString(),
            tokenEndpoint = buildOidcPath(Oidc4VciApi::oidToken.javaMethod),
            jwksUri = buildOidcPath(WellKnownConfigApiImpl::oauthJwkSet.javaMethod),
            // empty as we have no authorization endpoint
            responseTypesSupported = listOf(),
            grantTypesSupported = listOf(PreAuthCodeName),
            preAuthorizedGrantAnonymousAccessSupported = true,
        )
    }

    fun buildOidcConfig(): OidcProviderMetadata {
        val oauthMd = buildOauthConfig()
        return OidcProviderMetadata(
            oauthProviderMetadata = oauthMd,
            subjectTypesSupported = listOf("public"),
            idTokenSigningAlgValuesSupported = listOf("none", "RS256", "ES256"),
        )
    }

    @CacheResult(cacheName = "supported-credentials")
    fun buildSupportedCredentials(): Map<String, CredentialConfigurationSupported> {
        val result = mutableMapOf<String, CredentialConfigurationSupported>()

        for (profile in profileProvider.getAllProfiles()) {
            val credConfExt = profile.extensions["oid4vci"]?.get("credential_config")

            val credConf = when (profile.kind) {
                CredentialKind.JsonLD -> buildLdpCredConf(profile, credConfExt)
                CredentialKind.SD_JWT -> buildSdJwtCredConf(profile, credConfExt)
                else -> null
            }

            if (credConf != null) {
                result[profile.id] = credConf
            }
        }

        return result
    }

    private fun buildLdpCredConf(profile: Profile, profileExt: JsonNode?): W3cLdpJsonLdCredentialConfigurationSupported {

        val context = profile.template.withArrayProperty("@context").mapNotNull { if (it.isTextual) it.asText() else null }
        val type = profile.template.withArrayProperty("type").mapNotNull { if (it.isTextual) it.asText() else null }

        return W3cLdpJsonLdCredentialConfigurationSupported(
            credentialDefinition = JsonLdCredentialDefinition(
                context = context,
                type = type,
            )
        ).also { credConf ->
            // TODO: retrieve key for profile to determine algorithm
            credConf.credentialSigningAlgValuesSupported = listOf(
                // ld-cryptosuite-registry
                "Ed25519Signature2018",
                "Ed25519Signature2020",
                //"RsaSignature2018",
                //"EcdsaSecp256k1Signature2019",
                //"EcdsaSecp256k1RecoverySignature2020",
                "JsonWebSignature2020",
                //"GpgSignature2020",
                //"JcsEd25519Signature2020",
                "BbsBlsSignature2020",
                // data integrity
                //"ecdsa-rdfc-2019",
                //"ecdsa-jcs-2019",
                //"ecdsa-sd-2023",
                //"eddsa-rdfc-2022",
            )
            //it.cryptographicBindingMethodsSupported = listOf("did:key")
            credConf.proofTypesSupported = mapOf(
                ProofTypeEnum.LDP_VP to ProofTypesSupported(proofSigningAlgValuesSupported = listOf(
                    // ld-cryptosuite-registry
                    "Ed25519Signature2018",
                    "Ed25519Signature2020",
                    //"RsaSignature2018",
                    //"EcdsaSecp256k1Signature2019",
                    //"EcdsaSecp256k1RecoverySignature2020",
                    "JsonWebSignature2020",
                    //"GpgSignature2020",
                    //"JcsEd25519Signature2020",
                    //"BbsBlsSignature2020",
                    "BbsBlsSignatureProof2020",
                    // data integrity
                    //"ecdsa-rdfc-2019",
                    //"ecdsa-jcs-2019",
                    //"ecdsa-sd-2023",
                    //"eddsa-rdfc-2022",
                ))
            )

            profileExt?.get("display")?.let {
                try {
                    credConf.display = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal display value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }

            profileExt?.get("credential_definition")?.let {
                try {
                    credConf.credentialDefinition = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal credential_definition value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }

            profileExt?.get("order")?.let {
                try {
                    credConf.order = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal order value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }
        }
    }

    private fun buildSdJwtCredConf(profile: Profile, profileExt: JsonNode?): SdJwtCredentialConfigurationSupported {
        val vct = profile.template["vct"]?.textValue()
            ?: throw IllegalStateException("Template of profile ${profile.name} does not contain a vct field.")

        return SdJwtCredentialConfigurationSupported(
            vct = vct,
        ).also { credConf ->
            val sigAlgs = listOf(
                "RS256", "RS384", "RS512",
                "ES256", "ES384", "ES512",
                "ES256K",
                "PS256", "PS384", "PS512",
                "EdDSA",
            )
            // TODO: retrieve key for profile to determine algorithm
            credConf.credentialSigningAlgValuesSupported = sigAlgs
            credConf.cryptographicBindingMethodsSupported = listOf("jwk")
            credConf.proofTypesSupported = mapOf(
                ProofTypeEnum.JWT to ProofTypesSupported(proofSigningAlgValuesSupported = sigAlgs)
            )

            profileExt?.get("display")?.let {
                try {
                    credConf.display = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal display value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }

            profileExt?.get("claims")?.let {
                try {
                    credConf.claims = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal claims value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }

            profileExt?.get("order")?.let {
                try {
                    credConf.order = om.treeToValue(it)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to marshal order value of SD-JWT Credential Config. Fix the profile (${profile.id}) to include the value." }
                }
            }
        }
    }
}


interface ProfileProvider {
    fun getAllProfiles(): List<Profile>
}

@ApplicationScoped
class ProfileProviderImpl : ProfileProvider {
    @Inject
    @RestClient
    lateinit var profileService: ProfileServiceClient

    override fun getAllProfiles(): List<Profile> {
        val profileIds = profileService.listProfileIdentifiers().await().indefinitely()
        val profiles = profileIds.mapNotNull {
            try {
                val profile = profileService.fetchProfile(it).await().indefinitely()
                profile
            } catch (e: Exception) {
                logger.warn { "Failed to fetch profile $it." }
                null
            }
        }
        return profiles
    }
}
