package eu.xfsc.not.ssi_issuance2.domain

import io.quarkiverse.openapi.generator.annotations.GeneratedMethod
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import openapi.acapy.api.auth.AuthenticationPropagationHeadersFactory
import openapi.acapy.api.auth.CompositeAuthenticationProvider
import openapi.acapy.model.PresentationVerificationResult
import openapi.acapy.model.VerifyPresentationRequest
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/vc")
@RegisterRestClient(configKey = "acapy_json")
@RegisterProvider(CompositeAuthenticationProvider::class)
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory::class)
@ApplicationScoped
interface FixedAcapyVerifyApi {
    /**
     * Verify a Presentation
     *
     * @param body
     */
    @POST
    @Path("/presentations/verify")
    @Produces("application/json")
    @GeneratedMethod("")
    fun vcPresentationsVerifyPost(
        body: VerifyPresentationRequest?
    ): PresentationVerificationResult?

}
