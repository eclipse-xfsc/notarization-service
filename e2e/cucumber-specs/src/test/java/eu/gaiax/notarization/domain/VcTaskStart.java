package eu.gaiax.notarization.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 *
 * @author Neil Crossley
 */
public class VcTaskStart {

    @NotNull
    public String invitationURL;

    @NotBlank
    public String holderDID;
}
