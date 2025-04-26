package eu.xfsc.not.oid4vp

import eu.xfsc.not.oid4vp.domain.RequestObjectBuilder
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.keys.resolvers.VerificationKeyResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import java.time.Duration
import java.time.Instant


@QuarkusTest
class TestRequestObjectBuilder {

    @Inject
    lateinit var rqBuilder: RequestObjectBuilder

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test build JWT`() {
        val authReqId = "authReqId"
        val clientId = "localhost"
        val lifetime = Duration.ofSeconds(60)
        val now = Instant.now()
        val expAt = now.plus(lifetime)
        val reqObj = rqBuilder.buildAuthReq(authReqId)

        assertEquals(clientId, reqObj.clientId)
        assertEquals("x509_san_dns", reqObj.clientIdScheme?.value)

        val jwtStr = rqBuilder.buildJwt(reqObj, expAt)

        // validate jwt and check its content
        val jwtConsumer = JwtConsumerBuilder()
            .setEnableRequireIntegrity()
            .setRequireExpirationTime()
            .setExpectedIssuer(clientId)
            .setExpectedAudience("https://self-issued.me/v2")
            .setVerificationKeyResolver(VerificationKeyResolver { jws, _ ->
                val x509 = jws.certificateChainHeaderValue.firstOrNull()
                x509?.let {
                    val san = it.subjectAlternativeNames
                    val applicableSan = san.filter {
                        it[0] == 2 && it[1] == clientId
                    }
                    // return key when there is an entry in the SAN that matches the client id
                    applicableSan.firstOrNull()?.let { x509?.publicKey }
                }
            })
            .build()
        val jwt = jwtConsumer.process(jwtStr)
        assertEquals(clientId, jwt.jwtClaims.getClaimValue("client_id"))
    }

}
