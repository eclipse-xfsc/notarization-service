package eu.xfsc.not.oid4vp

import com.github.tomakehurst.wiremock.WireMockServer
import eu.gaiax.notarization.api.profile.TrainParameter
import eu.xfsc.not.oid4vp.domain.ResponseValidationErrorCode
import eu.xfsc.not.oid4vp.domain.ResponseValidationException
import eu.xfsc.not.oid4vp.domain.TrainDataProvider
import eu.xfsc.not.oid4vp.domain.TrainValidation
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.whenever

private val log = KotlinLogging.logger {}

@QuarkusTest
@QuarkusTestResource(TrainMockImp::class)
class TrainValidationTest {

    @TrainMock
    lateinit var trainMock: WireMockServer

    @Inject
    lateinit var trainValidation: TrainValidation

    @InjectMock
    lateinit var trainDataProvider: TrainDataProvider

    private fun setupTrainDataProvider(){
        whenever(trainDataProvider.getTrainParameter(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).then {
            TrainParameter().apply {
                trustSchemePointers = listOf(
                    "alice.trust.train1.xfsc.dev",
                )
                endpointTypes = emptyList()
            }
        }
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00015")
    fun deliversResultListForValidTrainAnswer(){
        setupTrainDataProvider()
        TrainMockImp.setStubValidAnswer(trainMock)

        if(trainValidation.needsTrainCheck()) {
            val result = trainValidation.checkDidTrust(
                    "profileId",
                    "taskName",
                    "A did"
                )
            assertThat(
                "result should contain one result at least",
                result?.resolvedResults?.isNotEmpty() ?: false
            )
        }
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00015")
    fun throwErrorOnEmptyTrainResult(){
        setupTrainDataProvider()
        TrainMockImp.setStubNoResults(trainMock)

        if(trainValidation.needsTrainCheck()) {
            try{
                trainValidation.checkDidTrust(
                    "profileId",
                    "taskName",
                    "A did"
                )
            } catch (e: ResponseValidationException){
                assertThat("Error code must match", e.failure == ResponseValidationErrorCode.InvalidTrainResult)
            }

        }
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00015")
    fun throwErrorOnNoEndpoints(){
        setupTrainDataProvider()
        TrainMockImp.setStubNoEndpoints(trainMock)

        if(trainValidation.needsTrainCheck()) {
            try{
                trainValidation.checkDidTrust(
                    "profileId",
                    "taskName",
                    "A did"
                )
            } catch (e: ResponseValidationException){
                assertThat("Error code must match", e.failure == ResponseValidationErrorCode.InvalidTrainTrust)
            }

        }
    }
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00015")
    fun throwErrorDidNotVerified(){
        setupTrainDataProvider()
        TrainMockImp.setStubDidUnverified(trainMock)

        if(trainValidation.needsTrainCheck()) {
            try{
                trainValidation.checkDidTrust(
                    "profileId",
                    "taskName",
                    "A did"
                )
            } catch (e: ResponseValidationException){
                assertThat("Error code must match", e.failure == ResponseValidationErrorCode.InvalidTrainTrust)
            }

        }
    }
    fun throwErrorOneDidNotVerified(){
        setupTrainDataProvider()
        TrainMockImp.setStubOneDidUnverified(trainMock)

        if(trainValidation.needsTrainCheck()) {
            try{
                trainValidation.checkDidTrust(
                    "profileId",
                    "taskName",
                    "A did"
                )
            } catch (e: ResponseValidationException){
                assertThat("Error code must match", e.failure == ResponseValidationErrorCode.InvalidTrainTrust)
            }

        }
    }
}
