package eu.gaiax.notarization.environment;

import jakarta.enterprise.context.ApplicationScoped;

import io.restassured.response.ValidatableResponse;

@ApplicationScoped
public class HttpResponseManagement {
    public ValidatableResponse lastResponse;
}
