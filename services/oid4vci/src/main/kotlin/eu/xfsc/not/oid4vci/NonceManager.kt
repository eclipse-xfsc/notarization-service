package eu.xfsc.not.oid4vci

import eu.gaiax.notarization.util.RandomGenerator
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import java.time.Instant

interface NonceManager {
    fun registerNonce(jti: String, expiresAt: Instant): String
    fun currentNonce(jti: String): String?
    fun renewNonce(jti: String): Pair<String, Long>?
}

@ApplicationScoped
class DbNonceManager : NonceManager {
    @Inject
    lateinit var random: RandomGenerator
    @Inject
    lateinit var nonceRepo: NonceRepo

    @Transactional
    override fun registerNonce(jti: String, expiresAt: Instant): String {
        val nonce = random.genNonce()
        nonceRepo.upsertNonce(jti, nonce, expiresAt)
        return nonce
    }

    @Transactional
    override fun currentNonce(jti: String): String? {
        return nonceRepo.findById(jti)?.let { it.nonce }
    }

    @Transactional
    override fun renewNonce(jti: String): Pair<String, Long>? {
        return nonceRepo.findById(jti)?.let {
            val newNonce = random.genNonce()
            it.nonce = newNonce
            val expiresIn = it.exp.epochSecond - Instant.now().epochSecond
            return Pair(newNonce, expiresIn)
        }
    }
}


@Entity
class ActiveNonce : PanacheEntityBase {
    @Id
    lateinit var atJti: String
    lateinit var nonce: String
    lateinit var exp: Instant
}

@ApplicationScoped
class NonceRepo : PanacheRepositoryBase<ActiveNonce, String> {
    fun upsertNonce(jti: String, nonce: String, expiresAt: Instant) {
        val existing = findById(jti)
        if (existing != null) {
            existing.nonce = nonce
            existing.exp = expiresAt
        } else {
            val new = ActiveNonce()
            new.atJti = jti
            new.nonce = nonce
            new.exp = expiresAt
            new.persist()
        }
    }

    fun removeNonces(now: Instant) {
        delete("exp < ?1", now)
    }
}


@ApplicationScoped
class NonceCleanup {
    @Inject
    lateinit var nonceRepo: NonceRepo
    @Scheduled(every="15m", )
    @Transactional
    fun removeStaleIdentifiers() {
        // make sure we don't delete anything while processing requests
        nonceRepo.removeNonces(Instant.now().minusSeconds(30))
    }
}
