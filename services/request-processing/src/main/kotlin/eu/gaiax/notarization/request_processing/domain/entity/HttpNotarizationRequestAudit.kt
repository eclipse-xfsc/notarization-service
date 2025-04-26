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
package eu.gaiax.notarization.request_processing.domain.entity

import eu.gaiax.notarization.request_processing.domain.model.NotarizationRequestAction
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@Entity
class HttpNotarizationRequestAudit : PanacheEntityBase {

    companion object : PanacheCompanionBase<HttpNotarizationRequestAudit, UUID> {
    }

    @Id
    var id: UUID? = null

    @Convert(converter = URIConverter::class)
    var requestUri: URI? = null
    var sessionId: String? = null
    var notarizationId: String? = null
    var ipAddress: String? = null

    @Enumerated(EnumType.STRING)
    var action: NotarizationRequestAction? = null
    var httpStatus = 0
    var caller: String? = null

    @Convert(converter = StringConverter::class)
    var requestContent: String? = null
    var receivedAt: OffsetDateTime? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null
    var taskName: String? = null

}
