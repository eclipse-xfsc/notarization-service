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
package eu.gaiax.notarization.request_processing.infrastructure.messaging

import com.fasterxml.jackson.annotation.JsonValue

/**
 *
 * @author Florian Otto
 */
enum class MsgType(private val value: String) {
    READY_FOR_REVIEW(Name.READY_FOR_REVIEW), REQUEST_REJECTED(Name.REQUEST_REJECTED),
    REQUEST_ACCEPTED_PENDING_DID(Name.REQUEST_ACCEPTED_PENDING_DID),
    REQUEST_ACCEPTED(Name.REQUEST_ACCEPTED),
    REQUEST_PRE_ACCEPTED(Name.REQUEST_PRE_ACCEPTED),
    REQUEST_DELETED(Name.REQUEST_DELETED), REQUEST_TERMINATED(Name.REQUEST_TERMINATED),
    EXTERNAL_TASK_STARTED(Name.EXTERNAL_TASK_STARTED),
    EXTERNAL_ACTION_STARTED(Name.EXTERNAL_ACTION_STARTED),
    CONTACT_UPDATE(Name.CONTACT_UPDATE);

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val READY_FOR_REVIEW = "READY_FOR_REVIEW"
        const val REQUEST_REJECTED = "REQUEST_REJECTED"
        const val REQUEST_ACCEPTED_PENDING_DID = "REQUEST_ACCEPTED_PENDING_DID"
        const val REQUEST_PRE_ACCEPTED = "REQUEST_PRE_ACCEPTED"
        const val REQUEST_ACCEPTED = "REQUEST_ACCEPTED"
        const val REQUEST_DELETED = "REQUEST_DELETED"
        const val REQUEST_TERMINATED = "REQUEST_TERMINATED"
        const val EXTERNAL_TASK_STARTED = "EXTERNAL_TASK_STARTED"
        const val EXTERNAL_ACTION_STARTED = "EXTERNAL_ACTION_STARTED"
        const val CONTACT_UPDATE = "CONTACT_UPDATE"
    }

    companion object {
        fun fromString(s: String?): MsgType? {
            return when (s) {
                Name.READY_FOR_REVIEW -> READY_FOR_REVIEW
                Name.REQUEST_REJECTED -> REQUEST_REJECTED
                Name.REQUEST_ACCEPTED_PENDING_DID -> REQUEST_ACCEPTED_PENDING_DID
                Name.REQUEST_PRE_ACCEPTED -> REQUEST_PRE_ACCEPTED
                Name.REQUEST_ACCEPTED -> REQUEST_ACCEPTED
                Name.REQUEST_DELETED -> REQUEST_DELETED
                Name.REQUEST_TERMINATED -> REQUEST_TERMINATED
                Name.EXTERNAL_TASK_STARTED -> EXTERNAL_TASK_STARTED
                Name.CONTACT_UPDATE -> CONTACT_UPDATE
                else -> null
            }
        }
    }
}
