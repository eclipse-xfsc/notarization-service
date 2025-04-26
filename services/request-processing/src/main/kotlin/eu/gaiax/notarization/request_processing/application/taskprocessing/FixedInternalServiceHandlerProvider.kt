package eu.gaiax.notarization.request_processing.application.taskprocessing

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.api.profile.TaskDescription
import eu.gaiax.notarization.request_processing.application.DocumentStore
import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.entity.DocumentStoreDocument
import eu.gaiax.notarization.request_processing.domain.entity.Session
import eu.gaiax.notarization.request_processing.domain.entity.SessionTask
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.infrastructure.config.ExtensionServiceConfig
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import mu.KotlinLogging
import org.hibernate.reactive.mutiny.Mutiny
import java.io.IOException
import java.net.URI
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}

@ApplicationScoped
class FixedInternalServiceHandlerProvider(
    private val mapper: ObjectMapper,
    private val documentStore: DocumentStore,
    private val extensionServiceConfig: ExtensionServiceConfig,
): InternalServiceHandlerProvider {

    lateinit var uploadDocumentNames: Set<String>
    lateinit var uploadDocumentServiceNames: Set<String>

    fun onStartup(@Observes event: StartupEvent) {
        val uploadDocumentConfig = extensionServiceConfig.internalTasks().getOrNull()?.uploadDocuments()?.getOrNull()

        val isEnabled = uploadDocumentConfig != null && uploadDocumentConfig.enabled().orElse(false)

        uploadDocumentNames = if (isEnabled) {
            uploadDocumentConfig!!.names().orElse(setOf())
        } else {
            setOf()
        }

        uploadDocumentServiceNames = if (isEnabled) {
            uploadDocumentConfig!!.serviceNames().orElse(setOf())
        } else {
            setOf()
        }
    }

    override fun handleTask(task: TaskDescription): InternalServiceExtensionHandler? {
        if (uploadDocumentNames.contains(task.name)) {
            return UploadDocumentServiceHandler(mapper, documentStore)
        }
        val serviceName = task.serviceName
        if (serviceName != null && uploadDocumentServiceNames.contains(serviceName)) {
            return UploadDocumentServiceHandler(mapper, documentStore)
        }
        return null
    }
}

class UploadDocumentServiceHandler(
    private val mapper: ObjectMapper,
    private val documentStore: DocumentStore,
) : InternalServiceExtensionHandler {

    @WithTransaction
    override fun createStartUri(id: TaskId): Uni<URI?> {

        return documentStore.startUploadTask(id)
            .map { null }
    }

    @WithTransaction
    override fun cancelWork(id: TaskId): Uni<Void> {
        return documentStore.cancelTask(id)
    }

    @WithTransaction
    override fun finishWorkSuccess(sessionTask: SessionTask, data: JsonNode?): Uni<Void> {
        try {
            val reader = mapper.readerFor(object : TypeReference<List<DocumentStoreDocument>>() {})
            val lst = reader.readValue<List<DocumentStoreDocument>>(data!!)
            val session = sessionTask.session!!
            return Mutiny.fetch(
                session.documents
            )
                .chain { docs: MutableSet<Document>? ->
                    val unis = lst.map { d: DocumentStoreDocument ->
                        val doc = Document()
                        doc.id = d.id
                        doc.content = d.content
                        doc.title = d.title
                        doc.mimetype = d.mimetype
                        doc.extension = d.extension
                        doc.shortDescription = d.shortDescription
                        doc.longDescription = d.longDescription
                        doc.verificationReport = d.verificationReport
                        doc.hash = d.hash
                        doc.createdAt = d.createdAt
                        doc.lastModified = d.lastModified
                        doc.session = sessionTask.session
                        docs!!.add(doc)
                        doc.persist<Document>().replaceWithVoid()
                    }.toMutableList()
                    sessionTask.fulfilled = true
                    unis.add(sessionTask.persist<SessionTask>().replaceWithVoid())
                    unis.add(
                        session.persist<Session>()
                            .replaceWithVoid()
                    )
                    Multi.createFrom().iterable(unis).onItem().transformToUniAndConcatenate { it }.collect().asList()
                        .invoke { item ->
                            logger.info { "Completed upload task ${sessionTask.taskId} with total documents: ${item.size}" }
                        }.replaceWithVoid()
                }
        } catch (ex: IOException) {
            logger.warn(ex) { "" }
            sessionTask.fulfilled = false
            sessionTask.running = false
        }
        return sessionTask.persist<SessionTask>().replaceWithVoid()
    }

    @WithTransaction
    override fun finishWorkFail(sessionTask: SessionTask, data: JsonNode?): Uni<Void> {
        sessionTask.fulfilled = false
        sessionTask.running = false
        return sessionTask.persist<SessionTask>().replaceWithVoid()
    }
}
