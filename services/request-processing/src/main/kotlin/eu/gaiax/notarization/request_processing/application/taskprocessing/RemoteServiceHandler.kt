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
package eu.gaiax.notarization.request_processing.application.taskprocessing

import com.fasterxml.jackson.databind.JsonNode
import eu.gaiax.notarization.api.extensions.BeginTaskResponse
import eu.gaiax.notarization.request_processing.domain.entity.Session
import io.smallrye.mutiny.Uni
import java.net.URI

/**
 *
 * @author Neil Crossley
 */
typealias RemoteServiceHandler = (URI, URI, JsonNode?, Session) -> Uni<BeginTaskResponse>
