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
 ***************************************************************************/

package eu.gaiax.notarization.oidc_identity_resolver.domain.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import java.net.URI;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


/**
 *
 * @author Florian Otto
 */
@Entity(name = "Session")
public class Session extends PanacheEntityBase {

    @Id
    public UUID id;
    public String loginNonce;
    public String cancelNonce;


    public URI successURI;
    public URI failURI;

}
