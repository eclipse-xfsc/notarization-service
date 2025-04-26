package eu.xfsc.not.oid4vp

import eu.xfsc.not.oid4vp.domain.TrainDataProvider
import eu.xfsc.not.oid4vp.domain.TrainValidation
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

class NoTrainConfigProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): MutableMap<String, String>? {
        return mutableMapOf( ("quarkus.rest-client.train_api.url" to "") )
    }
}

@QuarkusTest
@TestProfile(NoTrainConfigProfile::class)
class NoTrainConfigTest {
    @Inject
    lateinit var trainValidation: TrainValidation

    @InjectMock
    lateinit var trainDataProvider: TrainDataProvider

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00015")
    fun test() {
        assertThat("needs train check should be false ", trainValidation.needsTrainCheck() == false)
    }
}
