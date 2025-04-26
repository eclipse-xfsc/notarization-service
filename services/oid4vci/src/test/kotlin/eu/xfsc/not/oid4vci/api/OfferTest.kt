package eu.xfsc.not.oid4vci.api

import eu.xfsc.not.oid4vci.PreAuthCodeData
import eu.xfsc.not.oid4vci.PreAuthCodeGenerator
import eu.xfsc.not.oid4vci.PreAuthCodeValidator
import eu.xfsc.not.oid4vci.getPreAuthCodeData
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry


@QuarkusTest
class OfferTest {

    @Inject lateinit var authCodeGen: PreAuthCodeGenerator
    @Inject lateinit var authCodeVal: PreAuthCodeValidator

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00017")
    fun testSignValidateAuthCode_OK() {
        val codeData = PreAuthCodeData(
            issueSession = "session",
            profiles = listOf("profile1", "profile2"),
            callbackUrl = "http://callback.url/foo/bar"
        )
        val jwt = authCodeGen.createCode(codeData)
        val claims = authCodeVal.validatePreauthCode(jwt)
        val codeData2 = claims.getPreAuthCodeData()

        assertEquals(codeData.issueSession, codeData2.issueSession)
        assertIterableEquals(codeData.profiles, codeData2.profiles)
        assertEquals(codeData.callbackUrl, codeData2.callbackUrl)
    }

}
