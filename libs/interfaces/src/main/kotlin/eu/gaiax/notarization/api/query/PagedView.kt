package eu.gaiax.notarization.api.query

import eu.xfsc.not.api.util.JsonValueEnum
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.media.Schema

class PagedView<T, F> {
    var filter: F? = null
    @NotNull
    var index: Int = 0
    @NotNull
    var size: Int = 15
    @NotNull
    var pageCount: Int = 0
    @NotNull
    var total: Long = 0
    @NotNull
    var sort: SortDirection = SortDirection.Ascending

    @NotNull
    lateinit var items: List<T>
}

@Schema(
    description = "The sort direction.",
)
enum class SortDirection(override val value: String): JsonValueEnum {
    Ascending("Ascending"),
    Descending("Descending");
}
