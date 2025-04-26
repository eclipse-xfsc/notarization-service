package eu.gaiax.notarization.domain;

import jakarta.validation.constraints.NotNull;


/**
 *
 * @author Mike Prechtl
 */
public class OID4VPTaskStart {

    @NotNull
    public String walletBaseUri;

}
