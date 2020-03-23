package org.quokka.kotlin.internals

import com.github.swrirobotics.bags.reader.BagReader
import com.github.swrirobotics.bags.reader.MessageHandler
import com.github.swrirobotics.bags.reader.exceptions.BagReaderException
import com.github.swrirobotics.bags.reader.exceptions.UninitializedFieldException
import com.github.swrirobotics.bags.reader.messages.serialization.ArrayType
import com.github.swrirobotics.bags.reader.messages.serialization.MessageType
import com.github.swrirobotics.bags.reader.records.Connection

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
}

/**
 * A data block containing information about the measurements of a LiDAR camera.
 * The most important properties are range and reflectivity.
 * @property range The range to the object which reflected the laser.
 * @property reflectivity How reflective the surface of the object is.
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
 *
 * See https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf section 3.4 for more information.
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

// Main function which takes filepath as argument and then computes the duration of the recording.
fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Provide a filename for a .bag file to be parsed.")
        return
    }

    // Parse all the files
    for (path in args) {
        println("# Parsing file '$path'")
        try {
            val file = BagReader.readFile(path)

            var min_time = ULong.MAX_VALUE
            var max_time = ULong.MIN_VALUE
            var last_frame = -1
            var counter = 0

            println("Reading data...")
            // Only parse the lidar packets
            file.forMessagesOnTopic("/os1_node/lidar_packets", object : MessageHandler {
                override fun process(message: MessageType, conn: Connection): Boolean {
                    try {
                        val bytes = message.getField<ArrayType>("buf").asBytes
                        val az = parseAzimuthBlocks(bytes)
                        /*
                            az.forEach {
                                println(
                                    "[Frame${it.frameId}] Timestamp: ${it.timestamp}" +
                                            ", MeasurementID: ${it.measurementId}, EncouderCount: ${it.encoderCount}"
                                )
                            }
                         */
                        // For each Azimuth block compare the timestamps
                        az.forEach {
                            if (last_frame != it.frameId.toInt()) {
                                last_frame = it.frameId.toInt()
                                counter++
                            }
                            if (it.timestamp < min_time) {
                                min_time = it.timestamp
                            }
                            if (it.timestamp > max_time) {
                                max_time = it.timestamp
                            }
                        }
                    } catch (e: UninitializedFieldException) {
                        println("Error, field 'buf' not initialized\n$e")
                    }
                    return true
                }
            })

            val interval = max_time - min_time
            println(
                "Measurements are between ${min_time}ns and ${max_time}ns\nDuration: ${interval}ns" +
                        ", ${interval / 1_000_000_000UL}s, ${interval / 60_000_000_000UL}min"
            )
            println("$counter number of frames")

        } catch (e: BagReaderException) {
            System.err.println("Error reading file '$path'\n$e")
        }


    }
}

/*
Helper functions to parse various data types. The values in the file are in little endian.
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

fun ByteArray.getUIntAtLE(idx: Int) =
    ((this[idx + 3].toUInt() and 0xFFu) shl 24) or
            ((this[idx + 2].toUInt() and 0xFFu) shl 16) or
            ((this[idx + 1].toUInt() and 0xFFu) shl 8) or
            (this[idx + 0].toUInt() and 0xFFu)

fun ByteArray.getUShortAtLE(idx: Int) =
    (((this[idx + 1].toUInt() and 0xFFu) shl 8) or
            (this[idx + 0].toUInt() and 0xFFu)).toUShort()
