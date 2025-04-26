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

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestId
import eu.gaiax.notarization.request_processing.domain.model.ProfileId
import eu.gaiax.notarization.request_processing.domain.model.SessionId
import eu.gaiax.notarization.request_processing.domain.services.RequestNotificationService
import io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import java.net.URI
import java.time.ZonedDateTime

@ApplicationScoped
class FrontEndMessageService : RequestNotificationService {
    class EvtRequestorMessage(val id: String, val msg: MsgType, val payload: String?)
    class EvtNotaryMessage(val id: String, val msg: MsgType, val profileId: ProfileId)

    @Inject
    lateinit var config: AmqpConfig

    @Channel(outgoingOperatorRequestChanged)
    lateinit var operatorChannel: Emitter<JsonObject>

    @Channel(outgoingRequestorRequestChanged)
    lateinit var requestorChannel: Emitter<JsonObject>

    override fun onReadyForReview(request: NotarizationRequestId?, profile: ProfileId?) {
        if (request == null || profile == null) {
            return
        }
        sendToNotaryChannel(EvtNotaryMessage(request.id.toString(), MsgType.READY_FOR_REVIEW, profile))
    }

    override fun onRejected(identifier: SessionId?) {
        if (identifier == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_REJECTED, null))
    }

    override fun onPendingDid(identifier: SessionId?) {
        if (identifier == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_ACCEPTED_PENDING_DID, null))
    }

    override fun onPreAccepted(identifier: SessionId?) {
        if (identifier?.id == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_PRE_ACCEPTED, null))
    }

    override fun onAccepted(identifier: SessionId) {
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_ACCEPTED, null))
    }

    override fun onAccepted(identifier: SessionId, invite: String, discriminator: String) {
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_ACCEPTED, invite))
    }

    override fun onExternalTask(identifier: SessionId, redirect: URI?) {
        sendToRequestorChannel(
            EvtRequestorMessage(
                identifier.id,
                MsgType.EXTERNAL_TASK_STARTED,
                toString<URI?>(redirect)
            )
        )
    }

    override fun onExternalAction(identifier: SessionId, redirect: URI?) {
        sendToRequestorChannel(
            EvtRequestorMessage(
                identifier.id,
                MsgType.EXTERNAL_ACTION_STARTED,
                toString<URI?>(redirect)
            )
        )
    }

    override fun onContactUpdate(identifier: SessionId?, contact: String?) {
        if (identifier == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.CONTACT_UPDATE, contact))
    }

    override fun onDeleted(identifier: SessionId?) {
        if (identifier == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_DELETED, null))
    }

    override fun onTerminated(identifier: SessionId?) {
        if (identifier == null) {
            return
        }
        sendToRequestorChannel(EvtRequestorMessage(identifier.id, MsgType.REQUEST_TERMINATED, null))
    }

    private fun sendToRequestorChannel(evtMsg: EvtRequestorMessage) {
        val obj = JsonObject.of("id", evtMsg.id, "msg", evtMsg.msg.toString(), "payload", evtMsg.payload)
        val metadata = OutgoingCloudEventMetadata.builder<Any>()
            .withTimestamp(ZonedDateTime.now())
            .withExtension("type", evtMsg.msg.toString())
            .withType("%s.%s".format(config.cloudEventTypePrefix(), evtMsg.msg))
            .build()
        val msg = Message.of(obj, Metadata.of(metadata))
        requestorChannel.send(msg)
    }

    private fun sendToNotaryChannel(evtMsg: EvtNotaryMessage) {
        val obj = JsonObject.of("id", evtMsg.id, "msg", evtMsg.msg.toString(), "profileId", evtMsg.profileId.toString())
        val metadata = OutgoingCloudEventMetadata.builder<Any>()
            .withTimestamp(ZonedDateTime.now())
            .withExtension("profileId", evtMsg.profileId.id)
            .withExtension("type", evtMsg.msg.toString())
            .withType("%s.%s".format(config.cloudEventTypePrefix(), evtMsg.msg))
            .build()
        val msg = Message.of(obj, Metadata.of(metadata))
        operatorChannel.send(msg)
    }

    private fun <T> toString(item: T?): String? {
        return item?.toString()
    }

    companion object {
        const val onRequestorRequestChanged = "onRequestorRequestChanged"
        const val onOperatorRequestChanged = "onOperatorRequestChanged"
        const val outgoingRequestorRequestChanged = "requestor-request-changed"
        const val outgoingOperatorRequestChanged = "operator-request-changed"
    }
}
