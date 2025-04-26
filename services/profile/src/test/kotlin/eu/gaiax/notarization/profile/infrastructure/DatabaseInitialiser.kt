package eu.gaiax.notarization.profile.infrastructure

import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.runtime.StartupEvent
import io.quarkus.vertx.VertxContextSupport
import io.smallrye.config.Priorities
import io.smallrye.mutiny.Multi
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import mu.KotlinLogging
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class DatabaseInitialiser(
    @ConfigProperty(name = "quarkus.hibernate-orm.sql-load-script") val sqlFiles: Array<String>
) {

    companion object {
        var initialised = false
    }

    fun onStart(@Observes @Priority(Priorities.PLATFORM) ev: StartupEvent) {
        migrate()
    }
    fun migrate() {
        if (initialised) {
            return
        }
        initialised = true
        for (sqlFile in this.sqlFiles) {
            val path = Path.of(ClassLoader.getSystemResource(sqlFile).toURI())
            logger.debug { "Applying file $path" }
            val read: String = Files.readString(path)
            val statements = read.split(";").map { value -> value.substringBefore("--") }.filter { it.isNotBlank() }

            VertxContextSupport.subscribeAndAwait { ->
                Panache.withSession { ->
                    Panache.getSession().chain { session ->

                        Multi.createFrom().iterable(statements)
                            .onItem().transformToUniAndConcatenate { statement ->
                                logger.debug { "Applying statement: $statement" }

                                val query = session.createNativeQuery<Any>(statement)
                                query.executeUpdate().onFailure().invoke { throwable ->
                                    logger.warn(throwable) { "Error occurred applying statement: $statement" }
                                }
                            }.collect().asList().replaceWithVoid()
                    }
                }
            }

        }
    }

}
