package eu.gaiax.notarization.domain;

import java.util.Optional;


/**
 *
 * @author Florian Otto
 */
public class AIP_1_0_Credential {
    public AIP_1_0_Credential(){}
    public class Attrs {
        public Attrs(){}
        public String givenName;
        public String familyName;
        public String birthDate;
        public Optional<String> evidenceDocument;
    }
    public String referent;
    public String schema_id;
    public String cred_def_id;
    public String rev_reg_id;
    public String cred_rev_id;
    public Attrs attrs;

    public String requestToken(){
        return this.attrs.givenName;
    }
}