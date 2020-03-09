package LidarData

import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.sql.*
import java.util.*
import kotlin.system.measureTimeMillis

const val DATABASE_URL = "jdbc:postgresql://nyx.student.utwente.nl/lidar"
const val CREATE_DB_QUERY = """
CREATE TABLE IF NOT EXISTS recording (id SERIAL PRIMARY KEY, title varchar(255),
 minframe integer DEFAULT 0,
 maxframe integer DEFAULT 0,
 maxz real DEFAULT 7,
 minz real DEFAULT -7
);
 CREATE TABLE IF NOT EXISTS frame (frameid integer, recid integer REFERENCES recording(id)
 ON DELETE CASCADE, points bytea, PRIMARY KEY (frameid, recid));
"""
const val DELETE_DB_QUERY = "DROP TABLE IF EXISTS frame CASCADE; DROP TABLE IF EXISTS recording CASCADE;"
const val INSERT_FRAME = "INSERT INTO frame (frameid, recid, points) VALUES (?, ?, ?);"
const val INSERT_RECORDING = "INSERT INTO recording (title) VALUES (?) RETURNING id;"
const val SELECT_RECORDINGS = """
SELECT MIN(frameid) as minframe, MAX(frameid) as maxframe, id, title, COUNT(frameid) as numberofframes FROM recording,
 frame WHERE recid = id GROUP BY id;
"""
const val SELECT_SINGLE_RECORDING = "SELECT * FROM recording WHERE id = ?;"
const val SELECT_POINTS = "SELECT frameid, points FROM frame WHERE frameid = ANY (?) AND recid = ? LIMIT ?;"
const val UPDATE_RECORDING_FRAMES = "UPDATE recording SET minframe = ?, maxframe = ?, maxz = ?, minz = ? WHERE id = ?;"
const val FLOAT_BYTE_SIZE = 4
const val FLOATS_PER_POINT = 3

/**
 * The database class is used to communicate with the backend database which provides lidar recordings.
 *
 * First connect to the database and then use the appropriate methods to insert/retrieve data.
 *
 * Most important methods: connect, recordings, getFrames, recordingFromFile
 *
 * Example for setting up the schema.
 * <pre>
 * {@code
 * val db = Database()
 * db.connect("nyx", "lidar")
 * db.initTables()
 * db.close()
 * }
 * </pre>
 *
 * Example for fetching 100 frames at a framerate of 10 fps from each recording in the database.
 * <pre>
 * {@code
 * val db = Database()
 * db.connect("nyx", "lidar")
 * db.recordings.forEach {
 *     println("$it")
 *     val fr = db.getFrames(it.id, 1638, 100, framerate = Framerate.TEN)
 *     fr.forEach { f ->
 *         println("[${f.frameId}]: ${f.coords.size} ${f.coords.take(3)}")
 *     }
 * }
 * db.close()
 * }
 * </pre>
 *
 * Example of uploading a file to the database as a recording.
 * <pre>
 * {@code
 * val db = Database()
 * db.connect("nyx", "lidar")
 * db.recordingFromFile(path = "/path/to/file.bag", title = "example title")
 * db.close()
 * }
 */
class Database {
    private lateinit var conn: Connection

    /**
     * Returns a list of all recordings and their meta data.
     * Refer to the RecordingMeta class for more info on fields and data about recordings.
     */
    val recordings: List<RecordingMeta>
        get() {
            val st = conn.prepareStatement(SELECT_RECORDINGS)
            val rs = st.executeQuery()
            val recs: MutableList<RecordingMeta> = mutableListOf()

            while (rs.next()) {
                recs.add(
                        RecordingMeta(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getInt("minframe"),
                                rs.getInt("maxframe"),
                                rs.getInt("numberofframes"),
                                rs.getFloat("maxz"),
                                rs.getFloat("minz")
                        )
                )
            }
            return recs
        }

    fun getRecording(id: Int): RecordingMeta? {
        val st = conn.prepareStatement(SELECT_SINGLE_RECORDING)
        st.setInt(1, id)
        val rs = st.executeQuery()
        var ret: RecordingMeta? = null
        if (rs.next()) {
            ret = RecordingMeta(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getInt("minframe"),
                    rs.getInt("maxframe"),
                    rs.getInt("maxframe") - rs.getInt("minframe"),
                    rs.getFloat("maxz"),
                    rs.getFloat("minz")
            )
        }
        rs.close()
        st.close()
        return ret
    }

    /**
     * Connect to the database and authorize.
     *
     * @param user
     * @param password
     */
    fun connect(user: String, password: String) {
        val props = Properties()
        props.setProperty("user", user)
        props.setProperty("password", password)
        props.setProperty("ssl", "false")
        conn = DriverManager.getConnection(DATABASE_URL, props)
    }

    /**
     * Close the connection to the database.
     */
    fun close() {
        conn.close()
    }

    /**
     * Construct the schema.
     */
    fun initTables() {
        val st = conn.prepareStatement(CREATE_DB_QUERY)
        st.executeUpdate()
        st.close()
    }

    /**
     * Deconstruct the schema.
     */
    fun destroyTables() {
        val st = conn.prepareStatement(DELETE_DB_QUERY)
        st.executeUpdate()
        st.close()
    }

    /**
     * Creates a new recording without any frames.
     *
     * @param title The title of the recording.
     * @return The id given to the recording for inserting frames later.
     */
    fun newRecording(title: String): Int {
        val st = conn.prepareStatement(INSERT_RECORDING)
        st.setString(1, title)
        val rs = st.executeQuery()
        var r = -1
        while (rs.next()) {
            r = rs.getInt(1)
        }
        rs?.close()
        st?.close()
        return r
    }

    /**
     * Insert an array of array of floats as a frame.
     *
     * @param frameId The id of the frame.
     * @param recordingId The id of the recording the frame should belong to.
     * @param points The raw point data. The array format has to be Double[3][].
     * @return Returns the minimum and maximum Z coordinate
     */
    fun insertRawPointsAsFrame(frameId: Int, recordingId: Int, points: Array<Array<Float>>) : Pair<Float, Float> {
        val st = conn.prepareStatement(INSERT_FRAME)
        st.setInt(1, frameId)
        st.setInt(2, recordingId)

        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        val bb = ByteBuffer.allocate(FLOAT_BYTE_SIZE * points.size * FLOATS_PER_POINT)
        points.forEach {
            if (it[2] < minZ)
                minZ = it[2]
            if (it[2] > maxZ)
                maxZ = it[2]
            bb.putFloat(it[0])
            bb.putFloat(it[1])
            bb.putFloat(it[2])
        }

        st.setBinaryStream(3, ByteArrayInputStream(bb.array()))

        st.executeUpdate()
        st.close()
        return Pair(minZ, maxZ)
    }


    /**
     * Record all data from a file and put it in the database with a title for the given recording.
     *
     * @param path Path to the .bag file with the data.
     * @param title Title of the recording in the database.
     * @param reader The reader which parses the lidar data. Defaults to the default reader.
     * @param filterFun A filter function which removes LidarCoords from the uploaded set.
     */
    fun recordingFromFile(
            path: String,
            title: String,
            reader: LidarReader = LidarReader(),
            filterFun: (LidarCoord) -> Boolean = { true }
    ) {
        // Create a new recording first
        val recId = newRecording(title)
        // Id of the frame that is being modified.
        var lastFrame = -1
        // The list which is used to construct the raw point data for a frame.
        var currPoints: MutableList<Array<Float>>? = null
        var minFrame = Integer.MAX_VALUE
        var maxFrame = Integer.MIN_VALUE
        var maxZ = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        // Iterate through Azimuth blocks and construct and insert frames into the database.
        reader.readAzimuthBlocks(path, { az ->
            val fid = az.frameId.toInt()

            if (fid > maxFrame)
                maxFrame = fid
            if (fid < minFrame)
                minFrame = fid

            if (fid > lastFrame) {
                // If the frameId changes a whole frame has been captured since the frames in the file are ordered
                if (currPoints != null) {
                    // If the last frame existed, insert it into the database.
                    println("Inserted frame $lastFrame with ${currPoints!!.size} points")
                    if (currPoints!!.isNotEmpty()) {
                        val bounds = insertRawPointsAsFrame(lastFrame, recId, currPoints!!.toTypedArray())
                        if (bounds.first < minZ)
                            minZ = bounds.first
                        if (bounds.second > maxZ)
                            maxZ = bounds.second
                    }
                }

                // Update the frame id that is being constructed and create a new list of coords.
                lastFrame = fid
                currPoints = reader.azimuthBlockToLidarCoords(az).filter(filterFun).map {
                    arrayOf(it.x, it.y, it.z)
                }.toMutableList()
            } else if (fid == lastFrame) {
                // If the current frame is being constructed then just add the points to that.
                currPoints!!.addAll(reader.azimuthBlockToLidarCoords(az).filter(filterFun).map {
                    arrayOf(it.x, it.y, it.z)
                })
            }
        })

        // Insert last frame
        if (currPoints != null) {
            val bounds = insertRawPointsAsFrame(lastFrame, recId, currPoints!!.toTypedArray())
            if (bounds.first < minZ)
                minZ = bounds.first
            if (bounds.second > maxZ)
                maxZ = bounds.second
        }

        val st = conn.prepareStatement(UPDATE_RECORDING_FRAMES)
        st.setInt(1, minFrame)
        st.setInt(2, maxFrame)
        st.setFloat(3, maxZ)
        st.setFloat(4, minZ)
        st.setInt(5, recId)
        st.executeUpdate()
        st.close()
    }

    /**
     * Retrieve a list of frames from the database with the selected filter criteria.
     * The list is at most as long as the numberOfFrames given.
     * If frames are missing in the data the list will be shorter and frames will be skipped.
     *
     * @param recordingId The id of the recording where the frames should be from.
     * @param startFrame The starting frame of the buffer.
     * @param numberOfFrames The number of frames to be captured.
     * @param framerate How many frames should be displayed in a second.
     * @return A list of the constructed LidarFrames.
     */
    fun getFrames(
            recordingId: Int,
            startFrame: Int,
            numberOfFrames: Int,
            framerate: Framerate = Framerate.TEN
    ): List<LidarFrame> {
        val spf = framerate.stepsPerFrame
        val frames = mutableListOf<LidarFrame>()

        val recording = getRecording(recordingId)
        if (recording == null) {
            return emptyList()
        }

        val stx = conn.prepareStatement(SELECT_POINTS)
        val frameIds = (startFrame until (startFrame + spf * numberOfFrames) step spf).toList().toTypedArray()
        stx.setArray(1, conn.createArrayOf("integer", frameIds))
        stx.setInt(2, recordingId)
        stx.setInt(3, numberOfFrames)
        val rsx = stx.executeQuery()
        while (rsx.next()) {
            val points = rsx.getBinaryStream("points").buffered()
            val buff = ByteArray(FLOAT_BYTE_SIZE * FLOATS_PER_POINT)
            val off = 0
            val frameList = mutableListOf<LidarCoord>()
            while (points.read(buff, off, buff.size) == buff.size) {
                val bb = ByteBuffer.wrap(buff)
                frameList.add(LidarCoord(bb.float, bb.float, bb.float))
            }

            frames.add(LidarFrame(
                    rsx.getInt("frameid"),
                    frameList.toList(),
                    maxZ = recording.maxZ,
                    minZ = recording.minZ
            ))
        }
        rsx.close()
        stx.close()
        //return frames.sortedBy { it.frameId }.toList()
        return frames
    }
}

/**
 * An enum which defines the possible framerates when retrieving frames from the database.
 *
 * @property stepsPerFrame The number of frames skipped for the chosen framerate.
 */
enum class Framerate(val stepsPerFrame: Int) {
    TEN(1),
    FIVE(2),
    TWO(5),
    ONE(10)
}

/**
 * Meta data belonging to a recording.
 *
 * @property id The unique ID in the table associated with the recording.
 * @property title The title of the recording.
 * @property minFrame The minimum frame value.
 * @property maxFrame The maximum frame value.
 * @property numberOfFrames The total number of frames.
 */
data class RecordingMeta(
        val id: Int,
        val title: String,
        val minFrame: Int,
        val maxFrame: Int,
        val numberOfFrames: Int,
        val maxZ: Float,
        val minZ: Float
)


fun main() {
    val db = Database()
    db.connect("lidar", "mindhash")
    db.initTables()

    //db.getFrames(2, 2500, 1).forEach {
    //    it.generatePly("/home/nyx/downloads/test3.ply")
    //}

    //for (i in 0 until 20) {
    //    val nFrames = 50
    //    val time = measureTimeMillis {
    //        db.getFrames(1, 2400 + nFrames * i, nFrames, framerate = Framerate.FIVE)
    //    }

    //    println("Time to take $nFrames frames: $time")
    //}

    // Create reading with default LidarReader
    db.recordingFromFile(
            path = "/home/nyx/downloads/2019-03-26-10-54-38.bag",
            title = "all the points"
            //,filterFun = { lc -> (sqrt(lc.x.pow(2f) + lc.y.pow(2f))) < 24 }
    )
    db.close()
}
