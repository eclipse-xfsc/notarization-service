/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.rest.dto

import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
class RejectRequest(
    @get:Schema(description = "The reason that the notarization request is rejected") val reason: String
)
