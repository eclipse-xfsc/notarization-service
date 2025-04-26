package eu.gaiax.notarization.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;


public class CredentialOffer {

    public String credential_issuer;
    public List<String> credential_configuration_ids;
    public JsonNode grants;

}
