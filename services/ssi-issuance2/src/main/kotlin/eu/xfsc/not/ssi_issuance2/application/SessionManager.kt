package eu.xfsc.not.ssi_issuance2.application

import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.util.RandomGenerator
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.ws.rs.NotFoundException
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.Period
import java.util.*


@Entity(name = "Session")
class Session : PanacheEntityBase {

    @Id
    lateinit var id: UUID

    lateinit var token: String

    lateinit var profileId: String

    var holderDid : String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    lateinit var credentialData: ObjectNode

    lateinit var issuanceTimestamp: Instant
    lateinit var successURL: URI
    lateinit var failureURL: URI

    @CreationTimestamp
    lateinit var created: OffsetDateTime

}



@ApplicationScoped
class SessionRepository : PanacheRepositoryBase<Session, UUID> {

    @Inject
    lateinit var randomGen : RandomGenerator

    fun create(
        profileId: String,
        credentialData: ObjectNode,
        issuanceTimestamp: Instant,
        holderDid: String?,
        successURL: URI,
        failureURL: URI
    ): Session {
        return Session().apply {
            id = UUID.randomUUID()
            token = randomGen.genNonce()
            this.profileId = profileId
            this.credentialData = credentialData
            this.issuanceTimestamp = issuanceTimestamp
            this.holderDid = holderDid
            this.successURL = successURL
            this.failureURL = failureURL
            persistAndFlush()
        }
    }

    fun getByToken(token: String): Session? {
        return find("token", token).firstResult()
    }

    fun deleteByToken(token: String) {
        val count = delete("token", token)
        if (count == 0L) {
            throw NotFoundException()
        }
    }

    fun getByPeriod(retentionPeriod: Period): List<Session> {
        return find(
            "created < ?1",
            OffsetDateTime.now().minus(retentionPeriod)
        ).list()
    }

}
