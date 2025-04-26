package eu.xfsc.not.oid4vp

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.xfsc.not.oid4vp.domain.ResponseValidation
import eu.xfsc.not.oid4vp.domain.ResponseValidationException
import eu.xfsc.not.oid4vp.domain.TrainValidation
import eu.xfsc.not.oid4vp.model.AuthRequestObject
import eu.xfsc.not.oid4vp.model.AuthResponseRequest
import eu.xfsc.not.oid4vp.model.PresentationDefinition
import eu.xfsc.not.oid4vp.model.PresentationSubmission
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junitpioneer.jupiter.ReportEntry
import org.mockito.kotlin.whenever

@QuarkusTest
class ValidationTest {
    @Inject
    lateinit var resVal: ResponseValidation

    @InjectMock
    lateinit var train: TrainValidation

    @BeforeEach
    fun setupTrainMock() {
        whenever(train.needsTrainCheck()).then { false }
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00018")
    fun `test LDP validation`() {
        val authReq = AuthResponseRequest().apply {
            val vpStr = javaClass.getResourceAsStream("/vp-good-1.json")
            this.vpToken = jacksonObjectMapper().readTree(vpStr)
            this.presentationSubmission = PresentationSubmission("id", "def", listOf())
        }
        val origClientId = "http://domain"
        val reqObj = AuthRequestObject(
            responseType = listOf(),
            clientId = origClientId,
            nonce = "ABC",
            presentationDefinition = PresentationDefinition("id", listOf(), format = mapOf()),
        )

        var result = resVal.validateVp("non-profile", "non-task", reqObj, authReq)
        result.run {
            assertEquals(1, validationResults.size)
            validationResults[0].run {
                assertNull(trainValidationResult)
                validationResult.run {
                    assertEquals("did:key:z6MkuJHTu9Vmh8fYYXAi741Q1uLh17xaf2n5QwqepGHB5fK8", issuerDid)
                    assertEquals("did:key:z6Mku4oiHCAFSRxhY2xWDwsSZ2J6BuggSAoeX5D79e3jMUrr", subjectDid)
                }
            }
        }

        // try with wrong client id
        reqObj.clientId = "other.example.com"
        assertThrows<ResponseValidationException> { resVal.validateVp("non-profile", "non-task", reqObj, authReq) }
        // try with wrong nonce
        reqObj.clientId = origClientId
        reqObj.nonce = "XYZ"
        assertThrows<ResponseValidationException> { resVal.validateVp("non-profile", "non-task", reqObj, authReq) }
    }
}
