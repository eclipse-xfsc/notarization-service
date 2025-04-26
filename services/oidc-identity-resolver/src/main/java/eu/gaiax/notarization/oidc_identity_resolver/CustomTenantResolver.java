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

package eu.gaiax.notarization.oidc_identity_resolver;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;


/**
 * Related to the "quarkus.oidc.application-type" property. Usually, we want to use "web-app" that a business owner
 * is redirected to Keycloak when he is not authenticated yet. In the case, we retrieve a authorization header directly
 * (which is the case in load-tests and bdd-tests), we want skip the redirection to keycloak and want immediately land
 * on the login endpoint of the oidc-identity-resolver.
 *
 * see https://github.com/quarkusio/quarkus/issues/12297#issuecomment-698290318
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @ConfigProperty(name = "oidc-identity-resolver.webservice.enabled", defaultValue = "false")
    boolean enableWebservice;

    @Override
    public String resolve(RoutingContext context) {
        if (enableWebservice) {
            return context.request().getHeader("Authorization") != null ? "webservice" : null;
        } else {
            return null;
        }
    }

}
