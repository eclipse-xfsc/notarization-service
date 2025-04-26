package eu.gaiax.notarization.request_processing.domain.exception

import eu.gaiax.notarization.request_processing.domain.model.ProfileId

class UnconfiguredProfileException(val profileId: ProfileId, message: String?, cause: Throwable?) : BusinessException(message, cause) {

    constructor(profileId: ProfileId, message: String?) : this(profileId, message, null) {
    }
}
