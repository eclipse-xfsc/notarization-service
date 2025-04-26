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

import com.fasterxml.jackson.databind.JsonNode;
import eu.gaiax.notarization.api.extensions.BeginTaskResponse;
import eu.gaiax.notarization.api.extensions.ExtensionTaskServiceApi;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Path("/")
public class IdentityResource {

    public static final org.jboss.logging.Logger logger = Logger.getLogger(IdentityResource.class);

    public static final String START_LOGIN_RES = "start-login";
    public static final String START_LOGIN_RES_NONCE = START_LOGIN_RES + "/{nonce}";
    public static final String OIDC_LOGIN_RES = "login";
    public static final String OIDC_LOGOUT_RES = "logout";
    public static final String OIDC_CONTINUE_LOGIN_RES = "continue-login";
    public static final String OIDC_ERROR_PATH = "error";
    public static final String SESSION_RES = "session";
    public static final String SESSION_RES_NONCE = SESSION_RES + "/{nonce}";

    private IdentityStore store;
    private URI redirectLoginSuccess;
    private URI redirectLoginFailure;
    private boolean enableWebservice;

    @ConfigProperty(name = "oidc-identity-resolver.external.url")
    URI configuredExternalUrl;
    @Context
    SecurityIdentity secId;

    @Context UriInfo request;

    public IdentityResource(
            IdentityStore store,
            @ConfigProperty(name = "redirect.login-success.url") String redirectLoginSuccess,
            @ConfigProperty(name = "redirect.login-failure.url") String redirectLoginFailure,
            @ConfigProperty(name = "oidc-identity-resolver.webservice.enabled", defaultValue = "false")
            boolean enableWebservice) {
        this.store = store;
        this.redirectLoginSuccess = URI.create(redirectLoginSuccess);
        this.redirectLoginFailure = URI.create(redirectLoginFailure);
        this.enableWebservice = enableWebservice;
    }

    @Path(SESSION_RES)
    public BeginTaskResource beginIdentification(
    ) {
        return new BeginTaskResource(request, store, configuredExternalUrl);
    }

    static class BeginTaskResource implements ExtensionTaskServiceApi {

        private final UriInfo request;
        private final IdentityStore store;
        private final URI externalUrl;
        BeginTaskResource(UriInfo request, IdentityStore store, @ConfigProperty(name = "oidc-identity-resolver.external.url") URI externalUrl) {
            this.request = request;
            this.store = store;
            this.externalUrl = externalUrl;
        }

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @ResponseStatus(200)
        @APIResponse(responseCode = "200", description = "A redirect and a cancel URL.", content = @Content(schema = @Schema(implementation = BeginResponse.class)))
        @NotNull
        @Override
        public Uni<BeginTaskResponse> beginTask(
            @NotNull @QueryParam("success") URI success,
            @NotNull @QueryParam("failure") URI failure,
            @QueryParam("profileId") String profileId,
            @QueryParam("taskName") String taskName,
            @Nullable JsonNode data) {
            return store.startIdentification(success, failure)
                .onItem().ifNotNull().transform(nonces -> {

                    return new BeginTaskResponse(
                        uriBuilder(request)
                            .path(START_LOGIN_RES_NONCE)
                            .build(nonces.loginNonce()),
                        uriBuilder(request)
                            .path(SESSION_RES_NONCE)
                            .build(nonces.cancelNonce()));
                });
        }

        private UriBuilder uriBuilder(UriInfo uriInfo) {
            return UriBuilder.fromUri(externalUrl);
        }
    }

    @DELETE
    @Path(SESSION_RES_NONCE)
    public Uni<Void> cancelIdentification(
        @PathParam("nonce") String nonce
    ){
        return store.cancelIdentification(nonce);
    }

    @GET
    @Path(START_LOGIN_RES + "/" + "{nonce}")
    public Response startLogin(
        @PathParam("nonce") String nonce,
        @Context HttpServerRequest request
    ) throws URISyntaxException {
        NewCookie cookie = createNonceCookie(nonce);
        URI oidcLoginUri;

        if (secId.isAnonymous()) {
            logger.debugv("Beginning identification of unknown user");
            oidcLoginUri = UriBuilder.fromUri(configuredExternalUrl).path(OIDC_LOGIN_RES).build();
        } else {
            if (enableWebservice && request.getHeader("Authorization") != null) {
                logger.debugv("Beginning identification of user with bearer token");
                oidcLoginUri = UriBuilder.fromUri(configuredExternalUrl).path(OIDC_LOGIN_RES).build();
            } else {
                logger.debugv("Beginning identification of identified user by logging user out");
                oidcLoginUri = UriBuilder.fromUri(configuredExternalUrl).path(OIDC_LOGOUT_RES).build();
            }
        }
        return Response.seeOther(oidcLoginUri).cookie(cookie).build();
    }

    @GET
    @Path(OIDC_CONTINUE_LOGIN_RES)
    public Response continueLogin(@CookieParam("nonce") String nonce) throws URISyntaxException {
        if (nonce == null) {
            logger.warn("A user accessed the " + OIDC_CONTINUE_LOGIN_RES + " endpoint without a task cookie!");
            return this.oidcErrorResponse();
        }
        URI oidcLoginUri;
        if (secId.isAnonymous()) {
            oidcLoginUri = UriBuilder.fromUri(configuredExternalUrl).path(OIDC_LOGIN_RES).build();
        } else {
            oidcLoginUri = UriBuilder.fromUri(configuredExternalUrl).path(OIDC_LOGOUT_RES).build();
        }
        return Response.seeOther(oidcLoginUri).build();
    }

    @GET
    @Path(OIDC_LOGIN_RES)
    @Authenticated
    @SecurityRequirement(name = "oidc")
    public Uni<Response> oidcLogin(
        @CookieParam("nonce") String nonce
    ) throws URISyntaxException {
        return store.performLogin(nonce, secId)
                .map((Void v) -> Response.seeOther(this.redirectLoginSuccess).build());
    }

    @GET
    @Path(OIDC_ERROR_PATH)
    public Uni<Response> oidcError(
            @CookieParam("nonce") String nonce
    ) throws URISyntaxException {
        return store.cancelIdentification(nonce)
                .map((Void v) -> this.oidcErrorResponse());
    }

    private Response oidcErrorResponse() {
        final UriBuilder targetErrorUri = UriBuilder.fromUri(this.redirectLoginFailure);
        for (Map.Entry<String, List<String>> queryParameterEntry : request.getQueryParameters().entrySet()) {
            targetErrorUri.queryParam(queryParameterEntry.getKey(), queryParameterEntry.getValue().toArray());
        }
        return Response.seeOther(targetErrorUri.build())
                .build();
    }

    private NewCookie createNonceCookie(String nonce) {
        return new NewCookie.Builder("nonce")
            .value(nonce)
            .path("/")
            .secure(true)
            .httpOnly(true)
            .build();
    }
}
