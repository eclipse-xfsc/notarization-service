package eu.xfsc.not.api.oid4vci

import jakarta.validation.constraints.Size
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.openapi.annotations.Operation

@Path("api/v1/oid4vci/offer")
interface Oid4VciOfferApi {

    @Operation(
        summary = "Create OID4VCI Credential Offer",
        description = "Requests credential offer (cf. OID4VCI Sec. 4).",
    )
    @Path("credential-offer")
    @POST
    fun createCredentialOffer(
        @QueryParam("profiles") @Size(min = 1) profiles: List<String>,
        @QueryParam("issueSession") issueSession: String,
        @QueryParam("callbackUrl") callbackUrl: String,
    ): String

}
