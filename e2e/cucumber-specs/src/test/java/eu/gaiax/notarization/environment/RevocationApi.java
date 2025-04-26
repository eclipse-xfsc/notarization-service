
package eu.gaiax.notarization.environment;

import static io.restassured.RestAssured.given;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;
import java.util.zip.GZIPInputStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
public class RevocationApi {

    @Inject
    Configuration configuration;

    public boolean credentialRevoked(String listCredUrl, String index) {

        try {
            var encList = given()
                .when()
                .get(listCredUrl)
                .then()
                .extract()
                .jsonPath().getString("credentialSubject.encodedList")
                ;

            var bytes = Base64.getDecoder().decode(encList);
            var exp = new GZIPInputStream(new ByteArrayInputStream(bytes)).readAllBytes();
            var bitSet = BitSet.valueOf(exp);

            return bitSet.get(Integer.valueOf(index));

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void issueListCredential() {
        given()
                .when()
                .post(configuration.revocation().url().toString() + "/management/lists/issue-credentials")
                .then()
                .statusCode(204);
    }

}
