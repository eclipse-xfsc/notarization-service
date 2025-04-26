package eu.gaiax.notarization.api.revocation.sl2021

import com.fasterxml.jackson.annotation.JsonProperty
import eu.gaiax.notarization.api.revocation.CredentialStatus
import eu.gaiax.notarization.api.revocation.ListCredentialSubject


class CredentialStatus2021 (
    @JsonProperty("id")
    statusId: String,

    @JsonProperty("statusPurpose")
    var statusPurpose: String = "revocation",

    @JsonProperty("statusListIndex")
    var index: String? = null,

    @JsonProperty("statusListCredential")
    var listUrl: String? = null,

) : CredentialStatus(statusId)



class ListCredentialSubject2021 (
    @JsonProperty("id")
    credentialId: String,

    @JsonProperty("statusPurpose")
    var statusPurpose: String = "revocation",

    @JsonProperty("encodedList")
    var encodedList: String? = null,

) : ListCredentialSubject (credentialId)
