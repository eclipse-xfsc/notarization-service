package eu.gaiax.notarization.domain;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 *
 * @author Neil Crossley
 */
public record Profile(
	@NotNull String id) {

    @Override
    public String toString() {
        return id;
    }
}
