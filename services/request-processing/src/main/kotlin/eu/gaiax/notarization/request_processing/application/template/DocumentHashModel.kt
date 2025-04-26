/*
 *
 */
package eu.gaiax.notarization.request_processing.application.template

import eu.gaiax.notarization.request_processing.domain.model.TemplateModel
import mu.KotlinLogging
import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex

private val logger = KotlinLogging.logger {}

/**
 *
 * @author Neil Crossley
 */
class DocumentHashModel(private val hash: ByteArray) : TemplateModel.DocumentHash {
    override val hex: String
        get() = try {
            Hex.encodeHexString(hash)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Could not get hex", ex)
            throw ex
        }
    override val base64: String
        get() = try {
            Base64.encodeBase64String(hash)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Could not get base64", ex)
            throw ex
        }
    override val base: String
        get() = try {
            Base64.encodeBase64String(hash)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Could not get base", ex)
            throw ex
        }
    override val base64URLSafe: String
        get() = Base64.encodeBase64URLSafeString(hash)

    companion object {
        fun fromHex(hex: String?): DocumentHashModel {
            return if (hex == null) {
                DocumentHashModel(ByteArray(0))
            } else try {
                DocumentHashModel(Hex.decodeHex(hex))
            } catch (ex: DecoderException) {
                logger.warn(ex) { "Could not handle hex value: $hex" }
                throw IllegalArgumentException("The given value could not be con", ex)
            }
        }
    }
}
