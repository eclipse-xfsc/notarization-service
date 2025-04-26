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

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;


/**
 *
 * @author Mike Prechtl
 */
@SecuritySchemes(value = {
	@SecurityScheme(
		securitySchemeName = "oidc",
		type = SecuritySchemeType.OPENIDCONNECT,
		openIdConnectUrl = "https://idp.example.com/auth/realms/custom_realm/.well-known/openid-configuration"
	)
})
public class OIDCIdentityResolverApp extends Application {

}
