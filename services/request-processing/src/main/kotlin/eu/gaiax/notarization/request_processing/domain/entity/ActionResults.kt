package eu.gaiax.notarization.request_processing.domain.entity

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime
import java.util.*

@Entity
@Table(name = "session_task")
class ActionResults : PanacheEntityBase {

    companion object: PanacheCompanionBase<ActionResults, UUID> {
    }

    @Id
    @Column(name = "task_results_id")
    lateinit var taskResultsId: UUID

    @Column(name = "task_name")
    lateinit var taskName: String

    var content: String? = null

    @CreationTimestamp
    @Column(name = "created_at")
    var createdAt: OffsetDateTime? = null
}
