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
package eu.gaiax.notarization.request_processing.matcher

import eu.gaiax.notarization.request_processing.Helper.Companion.withTransactionAsync
import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import io.smallrye.mutiny.TimeoutException
import io.smallrye.mutiny.Uni
import mu.KotlinLogging
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hibernate.reactive.mutiny.Mutiny
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
object AuditTrailMatcher {
    fun auditTrailFor(
        sessionId: String,
        action: NotarizationRequestAction,
        sessionFactory: Mutiny.SessionFactory
    ): List<HttpNotarizationRequestAudit> {
        return auditTrailFor(sessionId, action, 1, sessionFactory)
    }

    @JvmStatic
    fun auditTrailForNotarizationRequestID(
        notReqId: String,
        action: NotarizationRequestAction,
        total: Int,
        sessionFactory: Mutiny.SessionFactory
    ): List<HttpNotarizationRequestAudit> {
        val queryString = "from HttpNotarizationRequestAudit audit where notarizationId = :id"
        return try {
            selectAuditLogs(queryString, notReqId, action, total, sessionFactory)
                .await().atMost(Duration.ofMillis(400))
        } catch (ex: TimeoutException) {
            listOf()
        }
    }

    @JvmStatic
    fun auditTrailFor(
        sessionId: String,
        action: NotarizationRequestAction,
        total: Int,
        sessionFactory: Mutiny.SessionFactory
    ): List<HttpNotarizationRequestAudit> {
        val queryString = "from HttpNotarizationRequestAudit audit where sessionId = :id"
        return try {
            selectAuditLogs(queryString, sessionId, action, total, sessionFactory)
                .await().atMost(Duration.ofMillis(500))
        } catch (ex: TimeoutException) {
            logger.warn("Handling timeout in assertion! This most likely leads to incorrectly reported errors!")
            listOf()
        }
    }

    private fun selectAuditLogs(
        queryString: String,
        id: String,
        action: NotarizationRequestAction,
        total: Int,
        sessionFactory: Mutiny.SessionFactory
    ): Uni<List<HttpNotarizationRequestAudit>> {
        return withTransactionAsync(
            sessionFactory
        ) { dbSession: Mutiny.Session?, transaction: Mutiny.Transaction? ->
            logger.debug("Selecting audit logs")
            val query = dbSession!!.createSelectionQuery(queryString, HttpNotarizationRequestAudit::class.java)
            query.setParameter("id", id)
            query.resultList
        }
            .onItem().transformToUni { entries: List<HttpNotarizationRequestAudit> ->
                var count = 0
                for (entry in entries) {
                    if (entry.action === action) {
                        count++
                    }
                    if (count >= total) {
                        logger.debug("Found expected audit logs")
                        return@transformToUni Uni.createFrom().item<List<HttpNotarizationRequestAudit>>(entries)
                    }
                }
                logger.debug("Retry - select the audit logs again")
                selectAuditLogs(queryString, id, action, total, sessionFactory)
            }
    }

    @JvmStatic
    fun auditTrailFor(
        sessionId: SessionId,
        action: NotarizationRequestAction,
        count: Int,
        sessionFactory: Mutiny.SessionFactory
    ): List<HttpNotarizationRequestAudit> {
        return auditTrailFor(sessionId.id, action, count, sessionFactory)
    }

    @JvmStatic
    fun hasAuditEntries(vararg builders: AuditLogMatcherBuilder): Matcher<Iterable<HttpNotarizationRequestAudit>> {
        val result: MutableList<Matcher<HttpNotarizationRequestAudit>> = ArrayList()
        for (builder in builders) {
            result.add(builder.matcher())
        }
        return Matchers.hasItems(*result.toTypedArray())
    }

    fun auditEntry(): AuditLogMatcherBuilder {
        return AuditLogMatcherBuilder()
    }

    @JvmStatic
    fun auditLogMarkUnready(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.MARK_UNREADY)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditFetchDocument(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.FETCH_DOCUMENT)
            .httpStatus(200)
    }

    @JvmStatic
    fun auditUploadDocument(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.UPLOAD_DOCUMENT)
            .httpStatus(204)
    }

    fun auditDeleteDocument(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.DELETE_DOCUMENT)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditAssignDid(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.ASSIGN_DID)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogFetch(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.FETCH)
            .httpStatus(200)
    }

    @JvmStatic
    fun auditLogMarkReady(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.MARK_READY)
            .httpStatus(200)
    }

    @JvmStatic
    fun auditLogCreateSession(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.CREATE_SESSION)
            .httpStatus(201)
    }

    @JvmStatic
    fun auditLogSubmit(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.SUBMIT)
            .httpStatus(201)
    }

    @JvmStatic
    fun auditLogUpdate(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.UPDATE)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogRevoke(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.REVOKE)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogClaim(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.CLAIM)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogAccept(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.ACCEPT)
            .httpStatus(204)
            .hasCaller()
    }

    @JvmStatic
    fun auditLogReject(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.REJECT)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogNotaryDelete(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.NOTARY_DELETE)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogFetchIdentity(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.FETCH_IDENTITY)
            .httpStatus(200)
    }

    fun auditLogFinSuccess(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.ISSUANCE_FINISH_SUCCESS)
            .httpStatus(204)
    }

    fun auditLogFinFail(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.ISSUANCE_FINISH_FAIL)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogManualRel(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.MANUAL_RELEASE)
            .httpStatus(204)
    }

    @JvmStatic
    fun auditLogTaskStart(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.TASK_START)
            .httpStatus(201)
            .taskNameNotEmpty()
    }

    @JvmStatic
    fun auditLogTaskCancel(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.TASK_CANCEL)
            .httpStatus(204)
            .taskNameNotEmpty()
    }

    @JvmStatic
    fun auditLogTaskFinish(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.TASK_FINISH_SUCCESS)
            .httpStatus(204)
            .taskNameNotEmpty()
    }

    @JvmStatic
    fun auditLogTaskFail(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.TASK_FINISH_FAIL)
            .httpStatus(204)
            .taskNameNotEmpty()
    }

    @JvmStatic
    fun auditLogAssignCredentialOverride(): AuditLogMatcherBuilder {
        return auditEntry().action(NotarizationRequestAction.CREDENTIAL_AUGMENTATION_PUT)
            .httpStatus(204)
            .hasCaller()
    }
}
