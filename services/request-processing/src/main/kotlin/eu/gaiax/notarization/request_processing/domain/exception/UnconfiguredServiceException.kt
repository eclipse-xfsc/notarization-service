package eu.gaiax.notarization.request_processing.domain.exception

import eu.gaiax.notarization.request_processing.domain.model.ProfileId

class UnconfiguredServiceException(val profileId: ProfileId, val name: String?, message: String?, cause: Throwable?) : BusinessException(message, cause) {

    constructor(profileId: String, name: String?) : this(ProfileId(profileId), name, null, null) {
    }
}
