package eu.gaiax.notarization.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;


/**
 *
 * @author Florian Otto
 */
public class Credential {
    public Credential(){}
    public class CredValue{
        public CredValue(){}
        public class Proof {
            public Proof(){}
            public String type;
            public String proofPurpose;
            public String verificationMethod;
            public String created;
            public String jws;
        }
        public class CredentialSubject {
            public CredentialSubject(){}
            public Object type;
            public String id;
            public String givenName;
            public String familyName;
            public String gender;
            public String image;
            public String residentSince;
            public String lprCategory;
            public String lprNumber;
            public String commuterClassification;
            public String birthCountry;
            public String birthDate;
            public Optional<String> evidenceDocument;
        }
        public class CredentialStatus{
            public CredentialStatus(){}
            public String id;
            public Object type;
            public String statusPurpose;
            public String statusListIndex;
            public String statusListCredential;
        }
        @JsonProperty("@context")
        public List<String> context;
        public List<String> type;
        public String issuer;
        public String issuanceDate;
        public String expirationDate;
        public CredentialSubject credentialSubject;
        public Proof proof;
        public CredentialStatus credentialStatus;
        public String name;
        public String description;
        public String identifier;
    }
    public List<String> contexts;
    public List<String> expanded_types;
    public List<String> schema_ids;
    public String issuer_id;
    public List<String> subject_ids;
    public List<String> proof_types;
    public CredValue cred_value;
    public Object cred_tags;
    public String record_id;

    public String requestToken(){
        return this.cred_value.credentialSubject.givenName;
    }

    public String getStatusListCredentialUri(){
        return this.cred_value.credentialStatus.statusListCredential;
    }
    public String getStatusListIndex(){
        return this.cred_value.credentialStatus.statusListIndex;
    }
}
