package eu.xfsc.not.ssi_issuance2

import eu.xfsc.not.ssi_issuance2.application.CryptoConverter
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

@QuarkusTest
class DbKeyManagerTest {

    @Inject
    lateinit var cryptoConv: CryptoConverter

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CP.NOTAR.E1.00040")
    fun `test Encryption`() {
        val testData = "Hełło Wörld!"
        val encData = cryptoConv.convertToDatabaseColumn(testData)
        val decData = cryptoConv.convertToEntityAttribute(encData)
        assertEquals(testData, decData)
        assertNotEquals(testData, encData)
    }

}
