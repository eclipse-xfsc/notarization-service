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

import eu.gaiax.notarization.api.profile.TaskType
import eu.gaiax.notarization.request_processing.domain.model.WorkType
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.*

/**
 *
 *
 * @author Florian Otto
 */
@Entity
@Table(name = "session_task")
class SessionTask : PanacheEntityBase {

    companion object: PanacheCompanionBase<SessionTask, UUID> {
    }

    @Id
    var taskId: UUID? = null

    var name: String? = null

    var type: TaskType? = null

    @Enumerated(EnumType.STRING)
    var workType: WorkType? = null

    var fulfilled = false

    var running = false

    @CreationTimestamp
    var createdAt: OffsetDateTime? = null

    @ManyToOne
    var session: Session? = null
}
