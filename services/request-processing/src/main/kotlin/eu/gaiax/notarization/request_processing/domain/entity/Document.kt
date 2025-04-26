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


import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime
import java.util.*

/**
 *
 * @author Neil Crossley
 */
@Entity
class Document : PanacheEntityBase {

    companion object : PanacheCompanionBase<Document, UUID> {
    }

    @Id
    var id: UUID? = null

    var content: ByteArray? = null

    @ManyToOne
    @JoinColumn(name = "session_id")
    var session: Session? = null

    @Convert(converter = StringConverter::class)
    var title: String? = null

    @Convert(converter = StringConverter::class)
    var shortDescription: String? = null

    @Convert(converter = StringConverter::class)
    var longDescription: String? = null

    @Convert(converter = StringConverter::class)
    var mimetype: String? = null

    @Convert(converter = StringConverter::class)
    var extension: String? = null

    @Convert(converter = StringConverter::class)
    var verificationReport: String? = null

    var hash: String? = null

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    var lastModified: OffsetDateTime? = null

}
