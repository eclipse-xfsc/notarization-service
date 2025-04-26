package eu.gaiax.notarization.util.db

import io.quarkus.narayana.jta.QuarkusTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.infrastructure.Infrastructure
import io.smallrye.mutiny.uni
import java.util.concurrent.Executor


fun <T> dbBlocking(
    context: Executor = Infrastructure.getDefaultWorkerPool(),
    surroundingFunction: (block: () -> T) -> T = ::txRequired,
    block: () -> T,
): Uni<T> {
    return uni {
        surroundingFunction { block() }
    }.runSubscriptionOn(context)
}

suspend fun <T> dbBlockingResolved(
    context: Executor = Infrastructure.getDefaultWorkerPool(),
    surroundingFunction: (block: () -> T) -> T = ::txRequired,
    block: () -> T,
): T {
    return dbBlocking(context, surroundingFunction, block).awaitSuspending()
}

fun <T> txRequired(block: () -> T): T {
    return QuarkusTransaction.joiningExisting()
        .call(block)
}

fun <T> txRequiresNew(block: () -> T): T {
    return QuarkusTransaction.requiringNew()
        .call(block)
}
