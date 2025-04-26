package eu.gaiax.notarization.request_processing.infrastructure.rest.dto

import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI

/**
 *
 * @author Florian Otto
 */
class InvitationUrlResponse(
    @get:Schema(description = "The invitation URL is present if the issuer began issuing the credential, but an invitation URL from the holder was not provided.")
    val inviteUrl: URI?,
    val issuerVersion: String,
)
