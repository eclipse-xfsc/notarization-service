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
package eu.gaiax.notarization.request_processing.domain.model

/**
 *
 * @author Neil Crossley
 */
enum class NotarizationRequestAction {
    CREATE_SESSION, UPDATE_CONTACT, FETCH, TIMEOUT, SUBMIT, UPLOAD_DOCUMENT, FETCH_DOCUMENT, DELETE_DOCUMENT, UPDATE, ASSIGN_DID, MARK_READY, MARK_UNREADY, REVOKE, CLAIM, ACCEPT, REJECT, TASK_START, TASK_CANCEL, TASK_FINISH_SUCCESS, TASK_FINISH_FAIL, ACTION_START, ACTION_CANCEL, ACTION_LIST, ACTION_FINISH_SUCCESS, ACTION_FINISH_FAIL, MANUAL_RELEASE, FETCH_IDENTITY, NOTARY_FETCH_ALL, NOTARY_FETCH_REQ, NOTARY_FETCH_DOC, NOTARY_DELETE, NOTARY_REVOKE, CREDENTIAL_AUGMENTATION_PUT, PRUNE_TERMINATED_SESSIONS, PRUNE_TIMEOUT_SESSIONS, PRUNE_SUBMISSIONTIMEOUT_SESSIONS, ISSUANCE_FINISH_SUCCESS, ISSUANCE_FINISH_FAIL;

    companion object {
        var RequestorActions = setOf(
            CREATE_SESSION,
            UPDATE_CONTACT,
            FETCH,
            SUBMIT,
            UPDATE,
            UPLOAD_DOCUMENT,
            FETCH_DOCUMENT,
            DELETE_DOCUMENT,
            ASSIGN_DID,
            MARK_READY,
            MARK_UNREADY,
            TASK_START,
            TASK_CANCEL,
            REVOKE
        )
        var NotaryActions = setOf(
            CLAIM,
            ACCEPT,
            REJECT,
            NOTARY_DELETE,
            NOTARY_REVOKE,
            ACTION_START,
            ACTION_CANCEL,
            ACTION_LIST,
            FETCH_IDENTITY,
            NOTARY_FETCH_ALL,
            NOTARY_FETCH_REQ,
            NOTARY_FETCH_DOC,
            CREDENTIAL_AUGMENTATION_PUT
        )
        var SystemActions =
            setOf(TIMEOUT, PRUNE_TERMINATED_SESSIONS, PRUNE_TIMEOUT_SESSIONS, PRUNE_SUBMISSIONTIMEOUT_SESSIONS)
        var CallbackActions =
            setOf(MANUAL_RELEASE, ISSUANCE_FINISH_SUCCESS, ISSUANCE_FINISH_FAIL, TASK_FINISH_SUCCESS, TASK_FINISH_FAIL, ACTION_FINISH_SUCCESS, ACTION_FINISH_FAIL)
        var NonAuditableContent = setOf(TASK_FINISH_SUCCESS, ACTION_FINISH_SUCCESS, UPLOAD_DOCUMENT, FETCH_DOCUMENT, CREDENTIAL_AUGMENTATION_PUT, UPDATE_CONTACT)
    }
}
