package org.quokka.kotlin.internals

/**
 * A single Azimuth block. It consists of meta data for the frame as well as 64 data blocks with measurements.
 * @property timestamp The record time in nanoseconds.
 * @property measurementId ID of the measurement.
 * @property frameId ID of the frame being captured.
 * @property encoderCount Value which represents the rotation of the camera
 * @property data An array of 64 measurements.
 *
 * See https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf section 3.4 for more information.
 */
data class Azimuth(
    val timestamp: ULong,
    val measurementId: UShort,
    val frameId: UShort,
    val encoderCount: UInt,
    val data: Array<DataBlock>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Azimuth

        if (timestamp != other.timestamp) return false
        if (measurementId != other.measurementId) return false
        if (frameId != other.frameId) return false
        if (encoderCount != other.encoderCount) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + measurementId.hashCode()
        result = 31 * result + frameId.hashCode()
        result = 31 * result + encoderCount.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * A data block containing information about the measurements of a LiDAR camera.
 * The most important properties are range and reflectivity.
 * @property range The range to the object which reflected the laser.
 * @property reflectivity How reflective the surface of the object is.
 * @property signalPhotons Unclear as to what this actually is.
 * @property ambientNoisePhotons Same as for signalPhotons.
 *
 * See https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf section 3.4 for more information.
 */
data class DataBlock(
    val range: UInt,
    val reflectivity: UShort,
    val signalPhotons: UShort,
    val ambientNoisePhotons: UShort
)

/**
 * Parse a single Azimuth block according to the specifications.
 * See https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf section 3.4 for more information.
 *
 * @param bytes The raw bytes in the form of an byte array.
 * @param offset The offset at which the parsing process should begin.
 *
 * @return An azimuth block containing the parsed data according to the specifications.
 */
fun parseAzimuth(bytes: ByteArray, offset: Int): Azimuth {
    // Parse the meta data
    val timestamp = bytes.getULongAtLE(offset + 0)
    val measurementId = bytes.getUShortAtLE(offset + 8)
    val frameId = bytes.getUShortAtLE(offset + 10)
    val encoderCount = bytes.getUIntAtLE(offset + 12)

    return Azimuth(timestamp, measurementId, frameId, encoderCount, Array(64) { i ->
        // The offset of the data block. The header size is 16 bytes while each data block is 12 bytes.
        val dataBlockOffset = offset + 16 + i * 12

        // Parse the data block. The 0xFFFFFu mask is used for the range because the first 12 bits are unused.
        val range = bytes.getUIntAtLE(dataBlockOffset) and 0xFFFFFu
        val reflectivity = bytes.getUShortAtLE(dataBlockOffset + 4)
        val signalPhotons = bytes.getUShortAtLE(dataBlockOffset + 6)
        val ambientNoisePhotons = bytes.getUShortAtLE(dataBlockOffset + 10)
        DataBlock(range, reflectivity, signalPhotons, ambientNoisePhotons)
    })
}

/**
 * Parse the Azimuth blocks contained in a single LiDAR data recording.
 * @param bytes The raw bytes of the data package. Must be exactly 12609 bytes long.
 *
 * See https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf section 3.4 for more information.
 */
fun parseAzimuthBlocks(bytes: ByteArray): Array<Azimuth> {
    // Check data size
    assert(bytes.size == 12609)

    // Parse the 16 blocks and form an array. Each block is 197 words long which equals 197 * 4 bytes.
    return Array(16) { i -> parseAzimuth(bytes, i * 197 * 4) }
}

/**
 * Extract an unsigned long in little endian from a byte array.
 *
 * @param idx The index at which the long is to parsed.
 */
fun ByteArray.getULongAtLE(idx: Int) =
    ((this[idx + 7].toULong() and 0xFFu) shl 56) or
            ((this[idx + 6].toULong() and 0xFFu) shl 48) or
            ((this[idx + 5].toULong() and 0xFFu) shl 40) or
            ((this[idx + 4].toULong() and 0xFFu) shl 32) or
            ((this[idx + 3].toULong() and 0xFFu) shl 24) or
            ((this[idx + 2].toULong() and 0xFFu) shl 16) or
            ((this[idx + 1].toULong() and 0xFFu) shl 8) or
            (this[idx + 0].toULong() and 0xFFu)

/**
 * Extract an unsigned int in little endian from a byte array.
 *
 * @param idx The index at which the int is to parsed.
 */
fun ByteArray.getUIntAtLE(idx: Int) =
    ((this[idx + 3].toUInt() and 0xFFu) shl 24) or
            ((this[idx + 2].toUInt() and 0xFFu) shl 16) or
            ((this[idx + 1].toUInt() and 0xFFu) shl 8) or
            (this[idx + 0].toUInt() and 0xFFu)

/**
 * Extract an unsigned short in little endian from a byte array.
 *
 * @param idx The index at which the short is to parsed.
 */
fun ByteArray.getUShortAtLE(idx: Int) =
    (((this[idx + 1].toUInt() and 0xFFu) shl 8) or
            (this[idx + 0].toUInt() and 0xFFu)).toUShort()
