package eu.gaiax.notarization.api.profile

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(
    description = "A placeholder for an actual enum that is ",
    type = SchemaType.OBJECT,
    hidden = true
)
enum class NoFilter {
}
