package eu.gaiax.notarization.profile.infrastructure.config

import eu.gaiax.notarization.api.profile.AipVersion
import eu.gaiax.notarization.api.profile.CredentialKind

class ConflictingCredentialConfigurationException(profile: AipVersion, kind: CredentialKind, msg: String?) :
    Exception(msg) {
}
