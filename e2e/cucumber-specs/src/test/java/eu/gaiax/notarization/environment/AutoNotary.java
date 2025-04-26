package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.environment.notary.RequestAcceptApi;
import eu.gaiax.notarization.environment.notary.RequestClaimApi;
import eu.gaiax.notarization.environment.notary.RequestFetchApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;


@ApplicationScoped
public class AutoNotary {

    @Inject
    PersonManagement personManagement;

    @Inject
    RequestManagement requestManagement;

    @Inject
    ProfileManagement profileManagement;

    @Inject
    RequestClaimApi requestClaimApi;

    @Inject
    RequestFetchApi requestFetchApi;

    @Inject
    RequestAcceptApi requestAcceptApi;

    @Inject
    Configuration config;

    public void findNotarizationRequest(String reqName) {
        // here we simulate an auto notary which is communicating with the notarization system and claims the submitted request
        personManagement.iAmAnOperator(config.profileIdAutoNotarization());
        requestClaimApi.claimNotarizationRequest(
                requestManagement.requestId(reqName),
                profileManagement.profileIdWithAutoNotarization().id());
    }

    public JsonObject fetchAvailableRequestData(String reqName) {
        var requestId = requestManagement.requestId(reqName);
        var response = requestFetchApi.fetchRequest(requestId, config.profileIdAutoNotarization());
        var reqData = response.extract().body().jsonPath().getObject("data", JsonObject.class);
        return Json.createReader(new StringReader(reqData.toString())).readObject();
    }

    public void acceptRequest(String reqName) {
        requestAcceptApi.acceptNotarizationRequest(
                requestManagement.requestId(reqName),
                profileManagement.profileIdWithAutoNotarization().id());
    }

}
