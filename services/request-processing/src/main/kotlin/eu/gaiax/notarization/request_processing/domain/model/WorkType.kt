package eu.gaiax.notarization.request_processing.domain.model

import com.fasterxml.jackson.annotation.JsonValue
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    description = "The types of work performed by extension services.",
    enumeration = [WorkType.Name.Task, WorkType.Name.Action]
)
enum class WorkType(private val value: String) {
    Task(WorkType.Name.Task), Action(WorkType.Name.Action);

    @JsonValue
    override fun toString(): String {
        return value
    }

    object Name {
        const val Task = "Task"
        const val Action = "Action"
    }
}
