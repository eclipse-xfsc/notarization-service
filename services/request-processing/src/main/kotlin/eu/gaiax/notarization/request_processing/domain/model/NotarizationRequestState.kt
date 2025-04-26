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

import com.fasterxml.jackson.annotation.JsonValue
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
@Schema(
    description =
    """
      The states of a notarization request.
        * `created` - Created. Pre-condition tasks must be performed.
        * `submittable` - Proposed credential information must be submitted. All tasks may be performed.
        * `submittable` - Request must be marked ready. All tasks may be performed. Different credentials may be re-submitted.
        * `readyForReview` - Request must be claimed by a notary. Readiness may be removed.
        * `workInProgress` - Request is claimed and under review by a notary. Must be approved, rejected or deleted.
        * `preAccepted` - Request was accepted by a notary. Configured services must process the request before issuance may begin.
        * `accepted` - Request was accepted by a notary. The issuance process has been started.
        * `pendingDID` - Request was accepted by a notary. Missing credential holder details must be submitted to proceed.
        * `pendingRequestorRelease` - Request was accepted by a notary. Manual release trigger must be performed by requestor.
        * `issued` - The credentials have been issued. The request processing is terminated. Submitted data and files are deleted.
        * `terminated` - The request processing is terminated. Submitted data and files are deleted.

    """,
    readOnly = true,
    enumeration = [NotarizationRequestState.Name.CREATED,
        NotarizationRequestState.Name.SUBMITTABLE,
        NotarizationRequestState.Name.EDITABLE,
        NotarizationRequestState.Name.READY_FOR_REVIEW,
        NotarizationRequestState.Name.WORK_IN_PROGRESS,
        NotarizationRequestState.Name.PRE_ACCEPTED,
        NotarizationRequestState.Name.ACCEPTED,
        NotarizationRequestState.Name.PENDING_DID,
        NotarizationRequestState.Name.TERMINATED,
        NotarizationRequestState.Name.ISSUED,
        NotarizationRequestState.Name.PENDING_RQUESTOR_RELEASE]
)
enum class NotarizationRequestState(private val value: String) {
    CREATED(Name.CREATED),
    SUBMITTABLE(Name.SUBMITTABLE),
    EDITABLE(Name.EDITABLE),
    READY_FOR_REVIEW(Name.READY_FOR_REVIEW),
    WORK_IN_PROGRESS(Name.WORK_IN_PROGRESS),
    PRE_ACCEPTED(Name.PRE_ACCEPTED),
    ACCEPTED(Name.ACCEPTED),
    PENDING_DID(Name.PENDING_DID),
    ISSUED(Name.ISSUED),
    TERMINATED(Name.TERMINATED),
    PENDING_RQUESTOR_RELEASE(Name.PENDING_RQUESTOR_RELEASE);

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val CREATED = "created"
        const val SUBMITTABLE = "submittable"
        const val EDITABLE = "editable"
        const val READY_FOR_REVIEW = "readyForReview"
        const val WORK_IN_PROGRESS = "workInProgress"
        const val PRE_ACCEPTED = "preAccepted"
        const val ACCEPTED = "accepted"
        const val PENDING_DID = "pendingDID"
        const val ISSUED = "issued"
        const val TERMINATED = "terminated"
        const val PENDING_RQUESTOR_RELEASE = "pendingRequestorRelease"
    }

    val isTaskProcessingAllowed: Boolean
        get() = taskProcessingStates.contains(this)
    val isActionProcessingAllowed: Boolean
        get() = actionProcessingStates.contains(this)
    val isRevokeable: Boolean
        get() = revokeableStates.contains(this)
    val isUpdateable: Boolean
        get() = updateableStates.contains(this)
    val isMarkReadyAllowed: Boolean
        get() = markReadyStates.contains(this)
    val isMarkUnreadyAllowed: Boolean
        get() = markUnreadyStates.contains(this)
    val isAssignDidAllowed: Boolean
        get() = assignDidStates.contains(this)
    val isUploadDocumentAllowed: Boolean
        get() = updateableStates.contains(this)
    val isFetchableByNotary: Boolean
        get() = fetchByNotaryStates.contains(this)
    val isClaimableByNotary: Boolean
        get() = claimByNotaryStates.contains(this)
    val isRejectableByNotary: Boolean
        get() = rejectByNotaryStates.contains(this)
    val isDeleteableByNotary: Boolean
        get() = deleteByNotaryStates.contains(this)
    val isAcceptableByNotary: Boolean
        get() = acceptByNotaryStates.contains(this)
    val isGetIdentityByNotary: Boolean
        get() = fetchIdentityNotaryStates.contains(this)
    val isChangedOnWorkFulfillment: Boolean
        get() = fulfilledWorkChangesState.contains(this)

    companion object {
        val taskProcessingStates = setOf(CREATED, SUBMITTABLE, EDITABLE)
        val actionProcessingStates = setOf(WORK_IN_PROGRESS, PRE_ACCEPTED, ACCEPTED, ISSUED)
        val revokeableStates = setOf(CREATED, EDITABLE, READY_FOR_REVIEW)
        val updateableStates = setOf(EDITABLE)
        val markReadyStates = setOf(EDITABLE)
        val markUnreadyStates = setOf(READY_FOR_REVIEW)
        val uploadDocumentsStates = setOf(EDITABLE)
        val assignDidStates = setOf(EDITABLE, READY_FOR_REVIEW, WORK_IN_PROGRESS, PRE_ACCEPTED, ACCEPTED, PENDING_DID)
        val fetchByNotaryStates = setOf(READY_FOR_REVIEW, WORK_IN_PROGRESS, PRE_ACCEPTED, ACCEPTED, PENDING_DID)
        val claimByNotaryStates = setOf(READY_FOR_REVIEW)
        val rejectByNotaryStates = setOf(WORK_IN_PROGRESS)
        val deleteByNotaryStates = setOf(WORK_IN_PROGRESS)
        val acceptByNotaryStates = setOf(WORK_IN_PROGRESS)
        val fetchIdentityNotaryStates = setOf(WORK_IN_PROGRESS, PRE_ACCEPTED)
        val fulfilledWorkChangesState = setOf(CREATED, PRE_ACCEPTED)

        //States which can/cannot be deleted due to timeout
        val statesAffectedBySubmissionTimeout = setOf(CREATED, SUBMITTABLE)
        val statesAffectedBySessionTimeout = setOf(EDITABLE, PENDING_DID, PENDING_RQUESTOR_RELEASE)
    }
}
