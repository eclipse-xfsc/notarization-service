package eu.xfsc.not.vc.status

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class StatusList2021Util (
    val bitstringBlockSize: Long = 8192,
    val bitstringMinBlocks: Long = 2,
) {
    fun determineBitstringNumBytes(curIdx: Long): Long {
        val blockSize = bitstringBlockSize
        val blockIdx = curIdx / blockSize
        val minBlocks = bitstringMinBlocks

        return if (blockIdx < minBlocks) {
            blockSize * minBlocks
        } else {
            (blockIdx + (blockIdx % minBlocks)) * blockSize
        }
    }

}


class StatusBitSet (
    val bitset: BitSet
) {
    fun encode(bitStringNumBytes: Int): String {
        val bitsetBytes = bitset.toByteArray()

        // safeguard in case we have more entries that anticipated (ORM cache fuckups?!?)
        val toEncode = if (bitsetBytes.size > bitStringNumBytes) {
            bitsetBytes
        } else {
            val targetBytes = ByteArray(bitStringNumBytes)
            bitsetBytes.copyInto(targetBytes)
            targetBytes
        }

        return encodeBytes(toEncode)
    }

    /**
     * Gets the status of the bit.
     * @return `false` if bit is not set, `true` otherwise
     */
    fun status(index: Int): Boolean? {
        try {
            return bitset[index]
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        fun decodeBitset(encoded: String): StatusBitSet {
            val unzipped = decodeBytes(encoded)
            return StatusBitSet(BitSet.valueOf(unzipped))
        }
    }
}


private fun encodeBytes(bitArray: ByteArray): String {
    // gzip data
    val zipped = with(ByteArrayOutputStream()) {
        GZIPOutputStream(this).use {
            it.write(bitArray)
        }
        this.toByteArray()
    }

    return Base64.getEncoder().encodeToString(zipped)
}

private fun decodeBytes(encoded: String): ByteArray {
    val rawBytes = Base64.getDecoder().decode(encoded)

    return ByteArrayInputStream(rawBytes).run {
        GZIPInputStream(this).use {
            it.readBytes()
        }
    }
}

