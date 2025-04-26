package eu.gaiax.notarization.api.profile

import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

@Schema(
    description = "A description for an action that is performed before or after the issuance of a verifiable credential, by the notary operator.",
    example =
        """
            {
                "name": "train-enrollment",
                "serviceName": "train-enrollment-proxy"
            }
        """
)
data class IssuanceAction(
    override val name: String,
    override val description: String?,
    override val serviceName: String?,
    override val serviceLocation: URI?,
    override val encryptAtRest: Boolean?
): WorkDescription(name, description = description, serviceName = serviceName, serviceLocation = serviceLocation, encryptAtRest = encryptAtRest) {
}
