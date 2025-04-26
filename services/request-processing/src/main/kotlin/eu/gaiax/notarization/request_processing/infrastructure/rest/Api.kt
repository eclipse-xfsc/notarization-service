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
package eu.gaiax.notarization.request_processing.infrastructure.rest

/**
 *
 * @author Neil Crossley
 */
class Api {
    object Tags {
        const val SUBMISSION = "submission"
        const val DOCUMENT = "document"
        const val ROUTINES = "routines"
        const val MANAGEMENT = "management"
        const val FINISH_TASK = "finishTask"
        const val FINISH_NOTARIZATION_REQUEST = "finishNotarizationRequest"
    }

    object Path {
        const val PREFIX = "api"
        const val V1_PREFIX = PREFIX + "/v1"
        const val SUBMISSION = "submission"
        const val IDENTIFY = "identify"
        const val TASK = "task"
        const val ACTIONS = "actions"
        const val FINISH_TASK = "finishTask"
        const val FINISH_ACTION = "finishAction"
        const val FINISH_NOTARIZATION_REQUEST = "finishNotarizationRequest"
        const val ISSUE_TRIGGER = "triggerIssuance"
        const val SESSION_RESOURCE = V1_PREFIX + "/session"
        const val DOCUMENT_RESOURCE = V1_PREFIX + "/document"
        const val ROUTINES_RESOURCE = V1_PREFIX + "/routines"
        const val ROUTINES_RESOURCE_PRUNE_TERMINATED_SESS = "/deleteTerminated"
        const val ROUTINES_RESOURCE_PRUNE_TIMEOUT_SESS = "/deleteTimeout"
        const val ROUTINES_RESOURCE_PRUNE_SUBMISIION_TIMEOUT_SESS = "/deleteSubmitTimeout"
        const val ROUTINES_RESOURCE_PRUNE_AUDIT_LOGS = "/auditLogs/clean"
        const val FINISH_TASK_RESOURCE = V1_PREFIX + "/" + FINISH_TASK
        const val ISSUE_MANUALY = "/" + ISSUE_TRIGGER
        const val FINISH_TASK_RESOURCE_WITH_NONCE = V1_PREFIX + "/" + FINISH_TASK + "/" + Param.NONCE_PARAM
        const val FINISH_ACTION_RESOURCE_WITH_NONCE = V1_PREFIX + "/" + FINISH_ACTION + "/" + Param.NONCE_PARAM
        const val FINISH_NOTARIZATION_REQUEST_RESOURCE_WITH_NONCE =
            V1_PREFIX + "/" + FINISH_NOTARIZATION_REQUEST + "/" + Param.NONCE_PARAM
        const val SUCCESS = "success"
        const val FAIL = "fail"
        const val DOCUP_BYLINK = "/uploadByLink"
        const val DOCUP_CONTENT = "/upload"
        const val CREDENTIAL_AUGMENTATION = "/credentialAugmentation"
    }

    object Param {
        const val DOCUMENTID = "documentId"
        const val RELEASE_TOKEN = "releaseToken"
        const val RELEASE_TOKEN_PARAM = "{" + RELEASE_TOKEN + "}"
        const val NOTARIZATION_REQUEST_DOCUMENT_ID_PARAM = "{" + DOCUMENTID + "}"
        const val NOTARIZATION_REQUEST_ID = "notarizationRequestId"
        const val NOTARIZATION_REQUEST_ID_PARAM = "{" + NOTARIZATION_REQUEST_ID + "}"
        const val PROFILE_ID = "profileId"
        const val PROFILE_ID_PARAM = "{" + PROFILE_ID + "}"
        const val SESSION = "sessionId"
        const val TASKID = "taskId"
        const val SESSION_PARAM = "{" + SESSION + "}"
        const val TASKID_PARAM = "{" + TASKID + "}"
        const val TOKEN = "token"
        const val NONCE = "nonce"
        const val NONCE_PARAM = "{" + NONCE + "}"
    }

    object Header {
        const val ACCESS_TOKEN = "token"
    }

    object Role {
        const val NOTARY = "notary"
    }

    object Response {
        const val CANNOT_REUSE_TOKEN = "CannotReuseToken"
    }
}
