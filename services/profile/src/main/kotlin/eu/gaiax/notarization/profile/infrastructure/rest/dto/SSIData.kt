package eu.gaiax.notarization.profile.infrastructure.rest.dto

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.issuance.ProfileIssuanceSpec
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

data class SSIData(
    @get:Schema(
        type = SchemaType.OBJECT,
        anyOf = [
        Aip10IssuanceSpecification::class,
        Aip20IssuanceSpecification::class
    ])
    val v1: JsonNode?,
    @get:Schema(type = SchemaType.OBJECT, implementation = ProfileIssuanceSpec::class)
    val v2: JsonNode?
    ) {

}
