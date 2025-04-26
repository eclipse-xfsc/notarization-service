
package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.domain.AIP_1_0_Credential;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class CredentialHolder {
    public Optional<AIP_1_0_Credential> lastAIP10Credential;
}
