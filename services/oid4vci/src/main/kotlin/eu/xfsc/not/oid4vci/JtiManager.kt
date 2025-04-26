package eu.xfsc.not.oid4vci

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.scheduler.Scheduled
import io.smallrye.jwt.auth.principal.ParseException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import java.time.Instant

/**
 * Interface for checking and marking jti (JWT Token ID) values.
 * A jti is used to prevent replay attacks.
 * Once marked, it must be kept in the data store until it's expiration date.
 */
interface JtiCheck {
    /**
     * Checks if the jti is already used.
     * In case it is used, it throws an exception.
     * @throws ParseException if the jti is already used
     */
    fun checkJti(jti: String): Unit
    fun markJtiAsUsed(jti: String, exp: Instant): Unit
}

fun Long.toExpirationDate(): Instant {
    return Instant.EPOCH.plusSeconds(this)
}

@ApplicationScoped
class DbJtiCheck : JtiCheck {
    @Inject
    lateinit var jtiRepo: JtiRepo
    @Transactional
    override fun checkJti(jti: String) {
        if (jtiRepo.hasJti(jti)) {
            throw ParseException("jti $jti already used")
        }
    }
    @Transactional
    override fun markJtiAsUsed(jti: String, exp: Instant) {
        jtiRepo.markJtiAsUsed(jti, exp)
    }
}

@Entity
class JtiBlocklist: PanacheEntityBase {
    @Id
    lateinit var jti: String
    lateinit var exp: Instant
}

@ApplicationScoped
class JtiRepo : PanacheRepositoryBase<JtiBlocklist, String> {

    fun hasJti(jti: String): Boolean {
        return findById(jti)?.let { true } ?: false
    }

    fun markJtiAsUsed(jti: String, exp: Instant) {
        val marker = JtiBlocklist().also {
            it.jti = jti
            it.exp = exp
        }
        persist(marker)
    }

    fun removeJtis(now: Instant) {
        delete("exp < ?1", now)
    }
}

@ApplicationScoped
class JtiCleanup {
    @Inject
    lateinit var jtiRepo: JtiRepo
    @Scheduled(every="15m", )
    @Transactional
    fun removeStaleIdentifiers() {
        // make sure we don't delete anything while processing requests
        jtiRepo.removeJtis(Instant.now().minusSeconds(30))
    }
}
