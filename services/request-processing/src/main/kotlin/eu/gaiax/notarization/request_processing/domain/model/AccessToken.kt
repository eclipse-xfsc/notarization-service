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
package eu.gaiax.notarization.request_processing.domain.model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import eu.gaiax.notarization.request_processing.domain.model.serialization.AccessTokenDeserializer
import eu.gaiax.notarization.request_processing.domain.model.serialization.AccessTokenSerializer
import eu.gaiax.notarization.request_processing.infrastructure.rest.Api
import jakarta.validation.constraints.NotNull
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

/**
 *
 * @author Neil Crossley
 */
@Schema(
    type = SchemaType.STRING, description = "A unique access token is generated for each notarization session."
            + " The requestor authenticates thier notarization request-specific REST API calls by setting the header '"
            + Api.Header.ACCESS_TOKEN + "' with the value of the access token for that request."
)
@JsonDeserialize(using = AccessTokenDeserializer::class)
@JsonSerialize(using = AccessTokenSerializer::class)
class AccessToken(val token: @NotNull String?)
