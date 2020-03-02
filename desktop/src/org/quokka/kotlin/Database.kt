package LidarData

import java.sql.*
import java.util.*
import kotlin.system.measureTimeMillis

const val DATABASE_URL = "jdbc:postgresql://localhost/lidar"
const val CREATE_DB_QUERY =
        "CREATE TABLE IF NOT EXISTS recording (id SERIAL PRIMARY KEY, title varchar(255)); CREATE TABLE IF NOT EXISTS frames (frameid integer, recid integer REFERENCES recording(id) ON DELETE CASCADE, points real[3][], PRIMARY KEY (frameid, recid));"
const val DELETE_DB_QUERY = "DROP TABLE IF EXISTS frames CASCADE; DROP TABLE IF EXISTS recording CASCADE;"
const val INSERT_FRAME = "INSERT INTO frames (frameid, recid, points) VALUES (?, ?, ?);"
const val INSERT_RECORDING = "INSERT INTO recording (title) VALUES (?) RETURNING id;"
const val SELECT_RECORDINGS =
        "SELECT MIN(frameid) as minframe, MAX(frameid) as maxframe, id, title, COUNT(frameid) as numberofframes FROM recording, frames WHERE recid = id GROUP BY id;"
const val SELECT_POINTS = "SELECT frameid, points FROM frames WHERE frameid = ANY (?) AND recid = ? LIMIT ?;"

/**
 * The database class is used to communicate with the backend database which provides lidar recordings.
 *
 * First connect to the database and then use the appropriate methods to insert/retrieve data.
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
class Database() {
    private var conn: Connection? = null

    /**
     * Returns a list of all recordings and their meta data.
     */
    val recordings: List<RecordingMeta>
        get() {
            val st = conn!!.prepareStatement(SELECT_RECORDINGS)
            val rs = st.executeQuery()
            val recs: MutableList<RecordingMeta> = mutableListOf()

            while (rs.next()) {
                recs.add(
                        RecordingMeta(
                                id = rs.getInt("id"), title = rs.getString("title"),
                                minFrame = rs.getInt("minframe"), maxFrame = rs.getInt("maxframe"),
                                numberOfFrames = rs.getInt("numberofframes")
                        )
                )
            }
            return recs
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
        conn?.close()
    }

    /**
     * Construct the schema.
     */
    fun initTables() {
        val st = conn!!.prepareStatement(CREATE_DB_QUERY)
        st.executeUpdate()
        st.close()
    }

    /**
     * Deconstruct the schema.
     */
    fun destroyTables() {
        val st = conn!!.prepareStatement(DELETE_DB_QUERY)
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
        val st = conn!!.prepareStatement(INSERT_RECORDING)
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
     * Inserts a LidarFrame as a frame to the given recording.
     *
     * @param frame The lidar frame which contains all necessary data.
     * @param recordingId The id of the recording the frame is associated with.
     */
    fun insertFrame(frame: LidarFrame, recordingId: Int) {
        val st = conn!!.prepareStatement(INSERT_FRAME)
        st.setInt(1, frame.frameId)
        st.setInt(2, recordingId)
        val points =
                frame.coords.map { lc -> arrayOf(lc.x, lc.y, lc.z) }.toTypedArray()
        st.setArray(3, conn!!.createArrayOf("float4", points))

        st.executeUpdate()
        st.close()
    }

    /**
     * Insert an array of array of doubles as a frame.
     *
     * @param frameId The id of the frame.
     * @param recordingId The id of the recording the frame should belong to.
     * @param points The raw point data. The array format has to be Double[3][].
     */
    fun insertPointsAsFrame(frameId: Int, recordingId: Int, points: Array<Array<Float>>) {
        val st = conn!!.prepareStatement(INSERT_FRAME)
        st.setInt(1, frameId)
        st.setInt(2, recordingId)
        st.setArray(3, conn!!.createArrayOf("float4", points))

        st.executeUpdate()
        st.close()
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
            reader: LidarReader = LidarReader.DefaultReader(),
            filterFun: (LidarCoord) -> Boolean = { true }
    ) {
        // Create a new recording first
        val recId = newRecording(title)
        // Id of the frame that is being modified.
        var lastFrame = -1
        // The list which is used to construct the raw point data for a frame.
        var currPoints: MutableList<Array<Float>>? = null
        // Iterate through Azimuth blocks and construct and insert frames into the database.
        reader.readAzimuthBlocks(path, { az ->
            val fid = az.frameId.toInt()

            if (fid > lastFrame) {
                // If the frameId changes a whole frame has been captured since the frames in the file are ordered
                if (currPoints != null) {
                    // If the last frame existed, insert it into the database.
                    println("Inserted frame $lastFrame with ${currPoints!!.size} points")
                    if (!currPoints!!.isEmpty()) {
                        insertPointsAsFrame(lastFrame, recId, currPoints!!.toTypedArray())
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
            insertPointsAsFrame(lastFrame, recId, currPoints!!.toTypedArray())
        }
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

        val stx = conn!!.prepareStatement(SELECT_POINTS)
        val frameIds = (startFrame until (startFrame + spf * numberOfFrames) step spf).toList().toTypedArray()
        stx.setArray(1, conn!!.createArrayOf("integer", frameIds))
        stx.setInt(2, recordingId)
        stx.setInt(3, numberOfFrames)
        val rsx = stx.executeQuery()
        while (rsx.next()) {
            val f = LidarFrame(rsx.getInt("frameid"))
            frames.add(f)
            (rsx.getArray("points").array as Array<Array<Float>>).forEach { a ->
                f.coords.add(LidarCoord(a[0], a[1], a[2]))
            }
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
data class RecordingMeta(val id: Int, val title: String, val minFrame: Int, val maxFrame: Int, val numberOfFrames: Int)


fun main() {
    val db = Database()
    db.connect("nyx", "lidar")
    db.initTables()

    for (i in 0 until 20) {
        val nFrames = 50
        val time = measureTimeMillis {
            db.getFrames(3, 2400 + nFrames * i, nFrames, framerate = Framerate.FIVE)
        }

        println("Time to take $nFrames frames: $time")
    }

    // Create reading with default LidarReader
    //db.recordingFromFile(
    //    path = "/home/nyx/downloads/2019-03-26-10-54-38.bag",
    //    title = "every point within a 32m radius"
    //    ,filterFun = { lc -> (Math.sqrt(lc.coords.first.pow(2.0) + lc.coords.second.pow(2.0))) < 32 }
    //)
    db.close()
}
