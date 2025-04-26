package eu.gaiax.notarization.environment;

import java.net.URL;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "acceptance")
public interface Configuration {

    Notarization notarization();
    Profile profile();
    AcaPyHolder holder();
    AcaPyIssuer issuer();
    Revocation revocation();
    Ledger ledger();
    Train train();
    TrainExtern trainExtern();
    TSA tsa();
    Keycloak keycloak();

    String portalProfileId();
    String portalProfileDecryptionKey();
    String profileIdWithoutTasks();
    String profileIdWithIdentificationPrecondition();
    String profileIdWithComplianceCheck();
    String profileIdAip10();
    String profileIdTrainEnrollment();
    String profileIdOid4vci();

    String profileIdAutoNotarization();

    public static interface TSA {
        URL url();
    }
    public static interface Train {
        URL url();
    }
    public static interface TrainExtern {
        URL url();
        String frameworkName();
    }
    public static interface Ledger {
        URL url();
    }
    public static interface Revocation {
        URL url();
    }
    public static interface Notarization {
        URL url();
    }
    public static interface Profile {
        URL url();
    }
    public static interface AcaPyHolder {
        URL url();
    }
    public static interface AcaPyIssuer {
        URL url();
    }
    public static interface Keycloak {
        URL url();
        Admin admin();

        String realm();

        String clientId();
        String clientSecret();

        String trainClientId();
        String trainClientSecret();
        String trainRole();

        public static interface Admin {
            String realm();
            String username();
            String password();
            String clientId();
            String clientSecret();
        }
    }
}
