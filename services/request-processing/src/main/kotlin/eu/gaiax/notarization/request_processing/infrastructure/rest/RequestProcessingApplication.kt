/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.gaiax.notarization.request_processing.infrastructure.rest

import eu.gaiax.notarization.request_processing.infrastructure.rest.mappers.problem_details.ProblemDetails
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Components
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType
import org.eclipse.microprofile.openapi.annotations.info.Contact
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.info.License
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 *
 * @author Neil Crossley
 */
@OpenAPIDefinition(
    tags = [Tag(
        name = Api.Tags.SUBMISSION,
        description = "Notarization request submission operations, as required for a requestor of a credential."
    ), Tag(
        name = Api.Tags.MANAGEMENT,
        description = "Notarization request management operations, as required for a notary."
    )],
    info = Info(
        title = "Notarization Request Processing API",
        version = "0.1.0",
        description = "This micro-service processes the submission and management of notarization requests.",
        contact = Contact(name = "GAIA-X", url = "https://www.gaia-x.eu/"),
        license = License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
    ),
    components = Components(
        securitySchemes = [SecurityScheme(
            securitySchemeName = "notary",
            type = SecuritySchemeType.OAUTH2,
            flows = OAuthFlows(
                authorizationCode = OAuthFlow(
                    authorizationUrl = "https://example.com/api/oauth/dialog",
                    tokenUrl = "https://example.com/api/oauth/token"
                )
            )
        )],
        responses = [APIResponse(
            name = Api.Response.CANNOT_REUSE_TOKEN,
            responseCode = "400",
            description = "Cannot Re-use Token",
            content = [Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = Schema(implementation = ProblemDetails::class)
            )]
        )]
    )
)
class RequestProcessingApplication : Application()
