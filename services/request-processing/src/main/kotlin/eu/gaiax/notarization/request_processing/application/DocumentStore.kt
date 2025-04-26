/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.request_processing.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import eu.gaiax.notarization.request_processing.application.domain.VerificationResult
import eu.gaiax.notarization.request_processing.application.taskprocessing.WorkExecutionEngine
import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.exception.*
import eu.gaiax.notarization.request_processing.domain.model.DocumentId
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestState
import eu.gaiax.notarization.request_processing.domain.model.taskprocessing.TaskId
import eu.gaiax.notarization.request_processing.domain.services.DownloadService
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.*
import eu.gaiax.notarization.request_processing.infrastructure.rest.resource.NotarizationRequestSubmissionResource.AccessPathParameters
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.pgclient.PgException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.PersistenceException
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.FilenameUtils
import org.hibernate.HibernateException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.function.Supplier

/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
class DocumentStore(
    private val objectMapper: ObjectMapper,
    private val downloadService: DownloadService,
    private val verificationService: VerificationService,
    private val workEngine: WorkExecutionEngine,
) {
    private val encoder = Base64.getUrlEncoder()
    private fun checkTokenAndState(dbSession: Session, params: AccessPathParameters) {
        if (dbSession.accessToken != params.asSessionInfo().token.token) {
            throw AuthorizationException("Not authorized", params.asSessionInfo().id)
        }
        if (!dbSession.state!!.isUploadDocumentAllowed) {
            throw InvalidRequestStateException(
                params.asSessionInfo().id,
                NotarizationRequestState.uploadDocumentsStates,
                dbSession.state!!
            )
        }
    }

    @WithTransaction
    fun startUploadTask(taskId: TaskId): Uni<Void?> {
        val ongoingTask = OngoingDocumentTask()
        ongoingTask.taskId = taskId.id
        return ongoingTask
            .persistAndFlush<OngoingDocumentTask>()
            .replaceWithVoid()
    }

    @WithTransaction
    fun cancelTask(taskId: TaskId): Uni<Void> {
        return OngoingDocumentTask.deleteById(taskId.id)
            .chain { _ -> DocumentStoreDocument.delete("taskId", taskId.id) }
            .replaceWithVoid()
    }

    @WithTransaction
    fun upload(document: DocumentUpload, taskId: TaskId, accessParams: AccessPathParameters): Uni<Void> {
        return Session.findById(accessParams.asSessionInfo().id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkTokenAndState(session!!, accessParams)
                OngoingDocumentTask.findById(taskId.id)
                    .onItem().ifNull().failWith { NotFoundException("Task") }
                    .onItem().ifNotNull()
                    .transformToUni{ ot: OngoingDocumentTask? ->
                        val doc = DocumentStoreDocument()
                        doc.id = document.id!!.id
                        doc.taskId = taskId.id
                        try {
                            doc.content = Files.readAllBytes(document.content!!.uploadedFile())
                        } catch (ex: IOException) {
                            throw InvalidFileException("The given file could not be processed", ex)
                        }
                        doc.title = document.title
                        doc.mimetype = document.content!!.contentType()
                        doc.extension = FilenameUtils.getExtension(document.content!!.fileName())
                        doc.title = document.title
                        doc.shortDescription = document.shortDescription
                        doc.longDescription = document.longDescription
                        verificationService.verify(doc.content)
                            .chain { verificationResult: VerificationResult ->
                                doc.hash = Hex.encodeHexString(verificationResult.digestResult)
                                doc.verificationReport = encoder.encodeToString(verificationResult.verificationReport)
                                doc.persistAndFlush<DocumentStoreDocument>()
                                    .onFailure(PersistenceException::class.java).transform { e: Throwable ->
                                        var cause = e.cause
                                        if (cause is HibernateException) {
                                            cause = cause.cause
                                            if (cause is PgException) {
                                                val code = cause.sqlState
                                                if ("23505" == code) {
                                                    return@transform BadParameterException("DocumentId exists.")
                                                }
                                            }
                                        }
                                        e
                                    }
                                    .replaceWithVoid()
                            }
                    }
            }
    }

    @Inject
    var vertx: Vertx? = null
    @WithTransaction
    fun uploadByLink(document: DocumentUploadByLink, taskId: TaskId, accessParams: AccessPathParameters): Uni<Void> {
        return Session.findById(accessParams.asSessionInfo().id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkTokenAndState(session!!, accessParams)
                OngoingDocumentTask.findById(taskId.id)
                    .onItem().ifNull().failWith { NotFoundException("Task") }
                    .onItem().ifNotNull()
                    .transformToUni { ot ->
                        val doc = DocumentStoreDocument()
                        doc.id = document.id!!.id
                        doc.taskId = taskId.id
                        doc.title = document.title
                        doc.mimetype = document.mimetype
                        doc.extension = document.extension
                        doc.shortDescription = document.shortDescription
                        doc.longDescription = document.longDescription
                        downloadService.download(document.location!!)
                            .onFailure().recoverWithNull()
                            .onItem().ifNull().failWith { BadParameterException("Downloadlink not valid") }
                            .chain<Buffer> { fileDownload: File ->
                                val f = vertx!!.fileSystem().readFile(fileDownload.path)
                                Uni.createFrom().completionStage<Buffer>(f.toCompletionStage())
                            }
                            .chain<VerificationResult?> { fileDownload: Buffer ->
                                doc.content = fileDownload.bytes
                                verificationService.verify(doc.content)
                            }
                            .chain { verificationResult: VerificationResult ->
                                doc.hash = Hex.encodeHexString(verificationResult.digestResult)
                                doc.verificationReport = encoder.encodeToString(verificationResult.verificationReport)
                                doc.persistAndFlush<DocumentStoreDocument>()
                                    .onFailure(PersistenceException::class.java).transform { e: Throwable ->
                                        var cause = e.cause
                                        if (cause is HibernateException) {
                                            cause = cause.cause
                                            if (cause is PgException) {
                                                val code = cause.sqlState
                                                if ("23505" == code) {
                                                    return@transform BadParameterException("DocumentId exists.")
                                                }
                                            }
                                        }
                                        e
                                    }
                                    .replaceWithVoid()
                            }
                    }
            }
    }

    @WithTransaction
    operator fun get(documentId: UUID, accessParams: AccessPathParameters): Uni<DocumentView> {
        return Session.findById(accessParams.asSessionInfo().id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkTokenAndState(session!!, accessParams)
                DocumentStoreDocument.findById(documentId)
                    .onItem().ifNull().failWith(Supplier<Throwable> { NotFoundException("Document") })
                    .map { storedDoc ->
                        val res = DocumentView()
                        res.id = DocumentId(storedDoc!!.id!!)
                        res.title = storedDoc.title
                        res.shortDescription = storedDoc.shortDescription
                        res.longDescription = storedDoc.longDescription
                        res
                    }
            }
    }

    @WithTransaction
    fun delete(documentId: UUID, accessParams: AccessPathParameters): Uni<Void> {
        return Session.findById(accessParams.asSessionInfo().id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkTokenAndState(session!!, accessParams)
                DocumentStoreDocument.deleteById(documentId).replaceWithVoid()
            }
    }

    @WithTransaction
    fun finish(taskId: TaskId, accessParams: AccessPathParameters): Uni<Void> {
        return Session.findById(accessParams.asSessionInfo().id.id)
            .onItem().ifNull().failWith { NotFoundException("Session") }
            .chain { session ->
                checkTokenAndState(session!!, accessParams)
                DocumentStoreDocument.find("taskId", taskId.id)
                    .list()
                    .chain { docs: List<DocumentStoreDocument> ->
                        val jNode = objectMapper.convertValue(docs, JsonNode::class.java)
                        workEngine.finishWorkSuccess(taskId, jNode)
                    }
            }
    }
}
