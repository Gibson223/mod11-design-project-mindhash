package LidarData

import com.github.swrirobotics.bags.reader.BagFile
import com.github.swrirobotics.bags.reader.BagReader
import com.github.swrirobotics.bags.reader.MessageHandler
import com.github.swrirobotics.bags.reader.exceptions.BagReaderException
import com.github.swrirobotics.bags.reader.messages.serialization.ArrayType
import com.github.swrirobotics.bags.reader.messages.serialization.MessageType
import com.github.swrirobotics.bags.reader.records.Connection
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

/**
 * The LidarReader class provides an interface to read LiDAR data from a file and translate it to XYZ coordinates
 * relative to the camera's position.
 *
 * @property beamAzimuthAngles The Azimuth angles queried via TCP from the camera.
 * @property beamAltitudeAngles The Altitude angles queried via TCP from the camera.
 *
 * See section 3.3. of https://data.ouster.io/downloads/v1.12.0-sw-user-guide.pdf for more information.
 */
data class LidarReader(private val beamAzimuthAngles: Array<Double> = arrayOf(
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164,
        3.164, 1.055, -1.055, -3.164, 3.164, 1.055, -1.055, -3.164
), private val beamAltitudeAngles: Array<Double> = arrayOf(
        16.611, 16.084, 15.557, 15.029, 14.502, 13.975, 13.447, 12.920,
        12.393, 11.865, 11.338, 10.811, 10.283, 9.756, 9.229, 8.701,
        8.174, 7.646, 7.119, 6.592, 6.064, 5.537, 5.010, 4.482,
        3.955, 3.428, 2.900, 2.373, 1.846, 1.318, 0.791, 0.264,
        -0.264, -0.791, -1.318, -1.846, -2.373, -2.900, -3.428, -3.955,
        -4.482, -5.010, -5.537, -6.064, -6.592, -7.119, -7.646, -8.174,
        -8.701, -9.229, -9.756, -10.283, -10.811, -11.338, -11.865, -12.393,
        -12.920, -13.447, -13.975, -14.502, -15.029, -15.557, -16.084, -16.611
)) {
    init {
        // Verify that the size of the arrays is correct
        assert(beamAzimuthAngles.size == 64)
        assert(beamAltitudeAngles.size == 64)
    }

    private var file: BagFile? = null

    private fun initFile(path: String) {
        if (file?.path == FileSystems.getDefault().getPath(path)) {
            // File is already correct
            return
        } else {
            file = BagReader.readFile(path)
        }
    }

    /**
     * Opens a .bag file, reads its content, converts every azimuth block to LidarCoords and then applies the
     * provided function on those LidarCoords.
     *
     * @param path The path to the file to be opened.
     * @param f The function which is called on the LidarCoords resulting of the file.
     * @param cond Function which controls the parsing. Stops the parsing progress when it returns false.
     *
     * @throws IOException throws an IOException if the file could not be opened.
     */
    @Throws(IOException::class)
    fun readLidarCoords(path: String, f: (LidarCoord) -> Unit, cond: (LidarCoord) -> Boolean = { true }) {
        readAzimuthBlocks(path, { block ->
            azimuthBlockToLidarCoords(block).forEach(f)
        }, { block ->
            azimuthBlockToLidarCoords(block).all(cond)
        })
    }

    /**
     * Opens a .bag file and executes the provided function on each Azimuth block in the file.
     *
     * @param path The path to the file to be opened.
     * @param f Function to be executed on an Azimuth block.
     * @param cond Function which controls the parsing. Stops the parsing progress when it returns false.
     */
    @Throws(IOException::class)
    fun readAzimuthBlocks(path: String, f: (Azimuth) -> Unit, cond: (Azimuth) -> Boolean = { true }) {
        readLidarPackages(path, { blocks -> blocks.forEach(f) }, { blocks -> blocks.all(cond) })
    }

    /**
     * Opens a .bag file and executes the provided function on each lidar package containing 16 Azimuth blocks.
     *
     * @param path The path to the file to be opened.
     * @param f Function to be executed on an array of Azimuth blocks.
     * @param cond Function which controls the parsing. Stops the parsing progress when it returns false.
     */
    @Throws(IOException::class)
    fun readLidarPackages(path: String, f: (Array<Azimuth>) -> Unit, cond: (Array<Azimuth>) -> Boolean = { true }) {
        try {
            initFile(path)
            // Read all the LiDAR data packets
            file?.forMessagesOnTopic("/os1_node/lidar_packets", object : MessageHandler {
                override fun process(message: MessageType, conn: Connection): Boolean {
                    val bytes = message.getField<ArrayType>("buf").asBytes
                    // Parse all the azimuth blocks
                    val az = parseAzimuthBlocks(bytes)
                    // Call the function f on the array of azimuth blocks
                    f(az)
                    return cond(az)
                }
            })
        } catch (e: BagReaderException) {
            throw IOException("Could not open file '$path'")
        }
    }

    /**
     * Generates a list of all recorded frames present in a .bag file.
     *
     * @param path The path to the .bag file to be parsed.
     * @return A list of LidarFrames each containing a list of recorded points.
     */
    fun readLidarFrames(path: String): List<LidarFrame> {
        return readLidarFramesInterval(path, Int.MIN_VALUE, Int.MAX_VALUE)
    }

    /**
     * Generates a list of all recorded frames present in a .bag file in the given interval in nanoseconds.
     *
     * @param path The path to the .bag file to be parsed.
     * @param start Start time in nanoseconds.
     * @param end End time in nanoseconds.
     * @return A list of LidarFrames.
     */
    fun readLidarFramesInterval(path: String, start: Int, end: Int): List<LidarFrame> {
        val frames = HashMap<Int, LidarFrame>()
        readAzimuthBlocks(path, { az ->
            if (az.frameId.toInt() >= start && az.frameId.toInt() <= end) {
                // Create a new frame or use an existing one
                val frame = frames.getOrPut(az.frameId.toInt()) { LidarFrame(az.frameId.toInt()) }
                frame.coords.addAll(azimuthBlockToLidarCoords(az))
                if (frame.timestamp == 0UL) frame.timestamp = az.timestamp
            }
        }, cond = { az -> az.frameId.toInt() < end })
        return frames.values.sortedBy { it.frameId }
    }

    /**
     * Returns a list of LidarFrames within the given time bound in nanoseconds.
     *
     * @param path The path of the file to be read from.
     * @param start The start of the measurements in nanoseconds.
     * @param end The end of the measurements in nanoseconds.
     * @return A list of LidarFrames.
     */
    fun readLidarTimeInterval(path: String, start: ULong, end: ULong): List<LidarFrame> {
        val frames = HashMap<Int, LidarFrame>()

        readAzimuthBlocks(path, { az ->
            if (az.timestamp >= start && az.timestamp <= end) {
                // Create a new frame or use an existing one
                val frame = frames.getOrPut(az.frameId.toInt()) { LidarFrame(az.frameId.toInt()) }
                frame.coords.addAll(azimuthBlockToLidarCoords(az))
                if (frame.timestamp == 0UL) frame.timestamp = az.timestamp
            }
        })
        return frames.values.sortedBy { it.frameId }
    }

    /**
     * Generate a set of meta data for the given file.
     *
     * @param path The path to the file being read.
     * @return A meta data struct which contains information about number of frames and points.
     */
    fun metaData(path: String): LidarMetaData {
        var counter = 0
        var min = ULong.MAX_VALUE
        var max = ULong.MIN_VALUE
        var minFrame = Int.MAX_VALUE
        var maxFrame = Int.MIN_VALUE
        readAzimuthBlocks(path, { az ->
            if (az.timestamp < min) min = az.timestamp
            if (az.timestamp > max) max = az.timestamp
            if (az.frameId.toInt() < minFrame) minFrame = az.frameId.toInt()
            if (az.frameId.toInt() > maxFrame) maxFrame = az.frameId.toInt()
        })

        return LidarMetaData(counter, Pair(min, max), Pair(minFrame, maxFrame), file?.path.toString())
    }

    /**
     * Converts one Azimuth block into an array of LidarCoords.
     * If no measurement is taken the values will be (0.0, 0.0, 0.0) and those coordinates are automatically removed
     * from the returned list.
     *
     * @param az The Azimuth block to be converted.
     * @return
     */
    fun azimuthBlockToLidarCoords(az: Azimuth): List<LidarCoord> {
        var arr = Array(64) { i ->
            val range = az.data[i].range.toFloat() / 1000
            val encoderCount = az.encoderCount
            val theta = 2 * Math.PI * (encoderCount.toDouble() / 90112 + beamAzimuthAngles[i] / 360)
            val phi = 2 * Math.PI * (beamAltitudeAngles[i] / 360)

            LidarCoord(
                    (range * Math.cos(theta) * Math.cos(phi)).toFloat() + 0f,
                    (-1 * range * Math.sin(theta) * Math.cos(phi)).toFloat() + 0f,
                    (range * Math.sin(phi)).toFloat() + 0f
            )
        }

        return arr.filter { lc -> lc != LidarCoord.ZeroCoord }
    }
}

/**
 * Represents a single point recorded by a LiDAR camera.
 *
 * @property coords The xyz position in space relative to the camera.
 * @property timestamp When the data was recorded in ns.
 * @property frameId The ID of the frame this coordinate belongs to. A frame is a single 360 degrees rotation.
 */
data class LidarCoord(
        val x: Float,
        val y: Float,
        val z: Float
) {
    companion object {
        val ZeroCoord = LidarCoord(0f, 0f, 0f)
    }
}

/**
 * Represents a single frame captured. It contains all non zero coordinates and meta data about the frame.
 *
 * @property coords A list of all non zero points captured.
 * @property frameId The ID of the frame.
 * @property timeStart Capture time of the first point in nanoseconds.
 * @property timeEnd Capture time of the last point in nanoseconds.
 */
data class LidarFrame(
        val frameId: Int
) {
    val coords: MutableList<LidarCoord> = mutableListOf()
    var timestamp: ULong = 0UL

    fun generatePly(file: String) {
        val logFile = File(file)
        val data = ArrayList<String>()
        coords.forEach { lc ->
            data.add("${lc.x} ${lc.y} ${lc.z}")
        }

        val writer = logFile.bufferedWriter()
        writer.use { out ->
            out.append(
                    data.joinToString(
                            separator = "\n", prefix = "ply\nformat ascii 1.0\n" +
                            "element vertex ${data.size}\nproperty float x\n" +
                            "property float y\n" +
                            "property float z\nend_header\n"
                    )
            )
            out.newLine()
        }
    }
}

/**
 * Contains meta data about a set of lidar data.
 *
 * @property numberOfPoints The total number of LidarCoords.
 * @property timeInterval Start time of the measurements in nanoseconds.
 * @property frameInterval End time of the measurements in nanoseconds.
 * @property filePath Path to the file where the measurements got read from.
 */
data class LidarMetaData(
        val numberOfPoints: Int,
        val timeInterval: Pair<ULong, ULong>,
        val frameInterval: Pair<Int, Int>,
        val filePath: String
)

// Parse a file and puts the frames into individual ply files
fun main(args: Array<String>) {
    if (args.size < 4) {
        println("Usage: [Path to .bag file] [Path to target directory] [Startnig Frame] [Number of frames to extract]")
        return
    }

    val reader = LidarReader()

    val filePath = args[0]
    val baseDirectory = args[1]
    val baseFrame = Integer.parseInt(args[2])
    val numberOfFrames = Integer.parseInt(args[3])

    println("Reading frames in '$filePath'")

    var frames: List<LidarFrame>? = null
    var time = measureTimeMillis {
        frames = reader.readLidarFramesInterval(filePath, baseFrame, baseFrame + numberOfFrames - 1)
    }
    println("Time taken: ${time}ms")
    println("Number of frames in interval: ${frames?.size}")
    println("Total number of points: ${frames?.map({ f -> f.coords.size })?.sum()}")

    var c = 0
    frames?.forEach { f ->
        f.generatePly("${baseDirectory}/${c}.ply")
        c++
    }
}
