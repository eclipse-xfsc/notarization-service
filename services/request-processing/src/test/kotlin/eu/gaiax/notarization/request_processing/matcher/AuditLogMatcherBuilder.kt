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

import eu.gaiax.notarization.request_processing.domain.entity.HttpNotarizationRequestAudit
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import java.util.*
import java.util.function.IntFunction

/**
 *
 * @author Neil Crossley
 */
class AuditLogMatcherBuilder {
    private var action: Matcher<HttpNotarizationRequestAudit>? = null
    private var requestId: Matcher<HttpNotarizationRequestAudit>? = null
    private val matchers: MutableList<Matcher<HttpNotarizationRequestAudit>> = ArrayList()
    private var ipAddress: Matcher<HttpNotarizationRequestAudit>? = null
    private var httpStatus: Matcher<HttpNotarizationRequestAudit>? = null
    private var taskNameNotEmpty: Matcher<HttpNotarizationRequestAudit>? = null
    private var requestContext: Matcher<HttpNotarizationRequestAudit>? = null
    private var callerNotEmpty: Matcher<HttpNotarizationRequestAudit>? = null
    fun action(action: NotarizationRequestAction): AuditLogMatcherBuilder {
        this.action = FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, NotarizationRequestAction>(
            "action",
            Matchers.equalTo<NotarizationRequestAction>(action)
        )
        return this
    }

    fun requestId(id: UUID): AuditLogMatcherBuilder {
        requestId = FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, UUID>(
            "notarizationId",
            Matchers.equalTo<UUID>(id)
        )
        return this
    }

    fun requestId(id: NotarizationRequestId): AuditLogMatcherBuilder {
        return this.requestId(id.id)
    }

    fun with(matcher: Matcher<HttpNotarizationRequestAudit>): AuditLogMatcherBuilder {
        matchers.add(matcher)
        return this
    }

    fun ipAddress(ipAddress: String): AuditLogMatcherBuilder {
        this.ipAddress = FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, String>(
            "ipAddress",
            Matchers.equalTo<String>(ipAddress)
        )
        return this
    }

    fun httpStatus(httpStatus: Int): AuditLogMatcherBuilder {
        this.httpStatus = FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, Int>(
            "httpStatus",
            Matchers.equalTo<Int>(httpStatus)
        )
        return this
    }

    fun taskNameNotEmpty(): AuditLogMatcherBuilder {
        taskNameNotEmpty =
            FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, Any>("taskName", Matchers.notNullValue())
        return this
    }

    fun hasCaller(): AuditLogMatcherBuilder {
        callerNotEmpty =
            FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, Any>("caller", Matchers.notNullValue())
        return this
    }

    fun requestContent(requestContent: String): AuditLogMatcherBuilder {
        requestContext = FieldMatcher.Companion.hasField<HttpNotarizationRequestAudit, String>(
            "requestContent",
            Matchers.equalTo<String>(requestContent)
        )
        return this
    }

    fun matcher(): Matcher<HttpNotarizationRequestAudit> {
        val result = ArrayList<Matcher<in HttpNotarizationRequestAudit>>()
        result.addAll(matchers)
        if (requestId != null) {
            result.add(requestId!!)
        }
        if (action != null) {
            result.add(action!!)
        }
        if (ipAddress != null) {
            result.add(ipAddress!!)
        }
        if (httpStatus != null) {
            result.add(httpStatus!!)
        }
        if (taskNameNotEmpty != null) {
            result.add(taskNameNotEmpty!!)
        }
        if (requestContext != null) {
            result.add(requestContext!!)
        }
        if (callerNotEmpty != null) {
            result.add(callerNotEmpty!!)
        }
        return Matchers.allOf(result)
    }

    companion object {
        fun <T> newArray(type: Class<Array<T>?>, size: Int): Array<T>? {
            return type.cast(java.lang.reflect.Array.newInstance(type.componentType, size))
        }

        fun <T, R : T?> genericArray(arrayCreator: IntFunction<Array<T>?>): IntFunction<Array<R>?> {
            return IntFunction { size: Int -> arrayCreator.apply(size) as Array<R>? }
        }
    }
}
