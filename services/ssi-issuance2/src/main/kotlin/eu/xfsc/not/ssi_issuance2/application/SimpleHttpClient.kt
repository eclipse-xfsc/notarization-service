package eu.xfsc.not.ssi_issuance2.application

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.RestClientBuilder
import org.jboss.resteasy.reactive.ResponseStatus
import java.net.URI

interface Post {
    @POST
    @Path("")
    @ResponseStatus(200)
    fun post()
}

@ApplicationScoped
class SimpleHttpClient() {
    private val builder = RestClientBuilder.newBuilder()
    fun post(url: URI) {
        builder
            .baseUri(url)
            .build(Post::class.java)
            .post()
    }

}
