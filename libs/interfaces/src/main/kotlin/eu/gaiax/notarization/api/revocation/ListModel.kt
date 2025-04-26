package eu.gaiax.notarization.api.revocation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import eu.gaiax.notarization.api.revocation.sl2021.CredentialStatus2021
import eu.gaiax.notarization.api.revocation.sl2021.ListCredentialSubject2021


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = CredentialStatus2021::class, name = "StatusList2021Entry"),
)
abstract class CredentialStatus (
    @JsonProperty("id")
    var statusId: String,
)

class ListCredentialRequestData (
    @JsonProperty("id")
    var listId: String? = null,
    var subject: ListCredentialSubject? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ListCredentialSubject2021::class, name = "StatusList2021"),
)
abstract class ListCredentialSubject (
    @JsonProperty("id")
    var credentialId: String? = null,
)
