package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.api.issuance.IssuanceInitRequest
import eu.gaiax.notarization.api.issuance.IssuanceInitResponse
import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import eu.gaiax.notarization.api.issuance.ServiceInitRequest
import eu.gaiax.notarization.api.profile.CredentialKind
import eu.xfsc.not.ssi_issuance2.domain.ProfileProvider
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Period

private val log = KotlinLogging.logger {}

@ApplicationScoped
class IssuanceService {

    @ConfigProperty(name = "issuance2.session.retention.period")
    var retentionPeriod: Period? = null

    @ConfigProperty(name = "issuance2.session.callFailOnTimeout")
    var callFailOnTimeout: Boolean? = null

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var callBackUrlBuilder: CallbackUrlBuilder

    @Inject
    lateinit var profileProvider: ProfileProvider

    @Inject
    lateinit var acaPyKeyManager: AcaPyKeyManager

    @Inject
    lateinit var localKeyManager: DbKeyManager

    @Inject
    lateinit var oidIssuance: OidIssuanceService

    @Inject
    lateinit var httpClient: SimpleHttpClient

    @Throws(WebApplicationException::class)
    fun initService(req: ServiceInitRequest): ProfileIssuanceSpec {
        val profile = profileProvider.fetchProfile(req.profileId) ?: run {
            log.error { "Profile (${req.profileId}) not found" }
            throw NotFoundException("Profile (${req.profileId}) not found")
        }

        val keyInit = when (profile.kind) {
            null, CredentialKind.AnonCred -> acaPyKeyManager
            CredentialKind.JsonLD, CredentialKind.SD_JWT -> localKeyManager
        }

        val issuerDid = keyInit.initKeypair(req.keyType)
        val revocationDid = if (profile?.isRevocable == true) {
            keyInit.initKeypair(req.keyType)
        } else null

        return ProfileIssuanceSpec(
            issuingDid = issuerDid,
            signatureType = req.signatureType,
            revocatingDid = revocationDid,
        )
    }

    @Transactional
    fun initSession(req: IssuanceInitRequest): IssuanceInitResponse {
        log.info { "Session init request received" }
        var session = sessionRepository.create(
            req.profileId,
            req.credentialData as ObjectNode,
            req.issuanceTimestamp,
            req.holderDID,
            req.successURL,
            req.failureURL
        )
        log.debug { "Sessiontoken: ${session.token}" }
        //if we allow other issuances as oid we have to use another resource
        val offerUrl = oidIssuance.getOffer(listOf(req.profileId), session.token)
        log.debug { "Got offer url: $offerUrl" }

        val cancelUrl = callBackUrlBuilder.buildCancelUrl(session.token)

        return IssuanceInitResponse(
            offerUrl,
            cancelUrl,
        )
    }

    @Transactional
    fun cancelSession(token: String) {
        log.warn { "Session was cancelled" }
        log.debug { "Session token was $token" }
        sessionRepository.deleteByToken(token)
    }

    @Transactional
    fun finishSessionSuccess(token: String) {
        sessionRepository.getByToken(token)?.let { sess ->
            try {
                httpClient.post(sess.successURL)
            } catch (e: Exception) {
                log.warn(e) { "An error happend during calling success url ${sess.successURL} - we will go on" }
            }
            sessionRepository.deleteByToken(token)
        }
    }

    @Transactional
    fun finishSessionFail(token: String, callFailUrl: Boolean) {
        sessionRepository.getByToken(token)?.let { sess ->
            if (callFailUrl) {
                try {
                    httpClient.post(sess.failureURL)
                } catch (e: Exception) {
                    log.warn(e) { "An error happend during calling failure url ${sess.failureURL} - we will go on" }
                }
            }
            sessionRepository.deleteByToken(token)
        }
    }

    @Transactional
    fun pruneTimeoutSession() {
        retentionPeriod?.let {period ->
            sessionRepository.getByPeriod(period).map { sessToDelete ->
                finishSessionFail(sessToDelete.token, callFailOnTimeout ?: false)
            }
        }
    }
}
