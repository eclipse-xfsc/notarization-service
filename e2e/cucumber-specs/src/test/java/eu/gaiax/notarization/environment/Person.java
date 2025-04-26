package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.domain.Role;
import io.restassured.specification.RequestSpecification;

public interface Person {
    
    public Role role();

    public void setAuthToken(String authToken);

    public RequestSpecification given();
}
