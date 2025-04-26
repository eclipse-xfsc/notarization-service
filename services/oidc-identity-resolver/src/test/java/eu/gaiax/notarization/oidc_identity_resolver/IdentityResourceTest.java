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

import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import static io.restassured.RestAssured.given;
import java.net.URI;
import java.net.URISyntaxException;

import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junitpioneer.jupiter.ReportEntry;


@QuarkusTest
@DisabledIfEnvironmentVariable(named = "IN_CI_ENV", matches = "true")
public class IdentityResourceTest {

    public static final Logger logger = Logger.getLogger(IdentityResourceTest.class);
    public static final WireMockServer mockRequestSubmission = new WireMockServer(wireMockConfig().dynamicPort());

	@ConfigProperty(name = "redirect.login-success.url")
	String successUrl;

    @BeforeAll
    public static void setup(){
        mockRequestSubmission.addMockServiceRequestListener(
                (in, out) -> requestReceived(in, out));
        mockRequestSubmission.start();
    }
    @AfterAll
    public static void stop(){
        mockRequestSubmission.stop();
    }

    public void stubConfig(String urlPat){
        mockRequestSubmission.stubFor(post(urlMatching(urlPat))
            .willReturn(noContent()));
    }

    protected static void requestReceived(Request inRequest, Response inResponse) {
        logger.debugv(" WireMock stub identity sink request at URL: {0}", inRequest.getAbsoluteUrl());
        logger.debugv(" WireMock stub identity sink request headers: \n{0}", inRequest.getHeaders());
        logger.debugv(" WireMock stub identity sink request body: \n{0}", inRequest.getBodyAsString());
        logger.debugv(" WireMock stub identity sink response body: \n{0}", inResponse.getBodyAsString());
        logger.debugv(" WireMock stub identity sink response headers: \n{0}", inResponse.getHeaders());
    }

    KeycloakTestClient keycloakClient = new KeycloakTestClient();
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00002")
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00030")
    public void loginWorksAndSubmitsToken() throws URISyntaxException {

        var wireMockUrl = mockRequestSubmission.baseUrl();
        stubConfig("/.*success");

        var body = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .queryParam("success", wireMockUrl + "/success")
            .queryParam("failure", wireMockUrl)
            .when()
            .post("session")
            .then()
            .statusCode(200)
            .extract()
            .body()
            ;

        var redirectURI = new URI(body.jsonPath().get("redirect"));

        var token = keycloakClient.getAccessToken("alice");

        var startLoginResp = given()
            .redirects()
            .follow(false)
            .when()
            .get(redirectURI)
            .then()
            .statusCode(303)
            .extract();

        String nonce = startLoginResp.cookie("nonce");
        String loginLocation = startLoginResp.header("Location");

        given()
            .redirects()
            .follow(false)
            .auth().oauth2(token)
            .cookie("nonce", nonce)
            .when()
            .get(loginLocation)
            .then()
            .statusCode(303)
            .header("Location", successUrl);

        ServeEvent serveEvent = mockRequestSubmission.getAllServeEvents().get(0);
        var req = serveEvent.getRequest();
        assertThat(req.getBodyAsString(), containsString(token));

        var resp = serveEvent.getResponse();
        assertThat(resp.getStatus(), is(204));

    }

}
