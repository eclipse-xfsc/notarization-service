/****************************************************************************
 * Copyright 2024 ecsec GmbH
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
 ***************************************************************************/

package eu.xfsc.not.train.enrollment.rest.model

import com.fasterxml.jackson.databind.JsonNode
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.net.URI


/**
 * @author Mike Prechtl
 */
data class StartEnrollmentRequest(
    @get:Schema(description = "The TRAIN Service Provider Entry", type = SchemaType.OBJECT)
    var tspJson: JsonNode,
    var trustListEndpoint: URI?,
    var frameworkName: String?
)
