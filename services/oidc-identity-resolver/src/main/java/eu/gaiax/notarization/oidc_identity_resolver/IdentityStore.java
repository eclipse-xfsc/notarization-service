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
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.notarization.oidc_identity_resolver.domain.entity.Session;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;


/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
public class IdentityStore {

    private final SecureRandom secureRandom;
    private final Logger logger;
    private final ObjectMapper mapper;


    public IdentityStore(
        Logger logger,
        ObjectMapper mapper
    ) {
        this.secureRandom = new SecureRandom();
        this.logger = logger;
        this.mapper = mapper;
    }

    @WithTransaction
    public Uni<Nonces> startIdentification(URI success, URI fail) {

        var session = new Session();
        session.id = UUID.randomUUID();
        session.successURI = success;
        session.failURI = fail;
        session.loginNonce = createNonce();
        session.cancelNonce = createNonce();

        return session.<Session>persistAndFlush()
            .map((s)->
                new Nonces(
                    s.loginNonce,
                    s.cancelNonce
                )
            );
    }

    @WithTransaction
    public Uni<Void> cancelIdentification(String nonce) {
        return Session.delete("cancelNonce", nonce)
            .replaceWithVoid();
    }

    @WithTransaction
    public Uni<Void> performLogin(String nonce, SecurityIdentity secId) {
        return Session.<Session>find("loginNonce", nonce)
            .firstResult()
            .onItem().ifNull().failWith(NotFoundException::new)
            .onItem().ifNotNull()
            .transformToUni((sess) -> {

                RestClientBuilder requestProcessingClient;

                var principal = secId.getPrincipal();
                JsonNode res;

                if(principal != null){
                    requestProcessingClient = RestClientBuilder.newBuilder()
                            .baseUri(sess.successURI);
                    res = mapper.convertValue(principal, JsonNode.class);
                } else {
                    requestProcessingClient =
                        RestClientBuilder.newBuilder()
                            .baseUri(sess.failURI);
                    res = mapper.convertValue("Error after authentication.", JsonNode.class);
                }
                return sess.delete()
                        .chain(()->{
                            return requestProcessingClient.build(NotarizationServiceIdentityCallback.class)
                                .finishTask(res);
                        });
                });

    }

    public String createNonce(){
        return urlSafeString(secureRandom, new byte[64]);
    }

    public static String urlSafeString(SecureRandom secureRandom, byte[] tokenBuffer) {
        secureRandom.nextBytes(tokenBuffer);
        return
            Base64
                .getUrlEncoder().withoutPadding()
                .encodeToString(tokenBuffer);
    }

}
