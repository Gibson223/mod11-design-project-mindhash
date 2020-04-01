import org.junit.Assert.*
import org.junit.Test
import org.junit.BeforeClass
import org.quokka.kotlin.internals.Database
import org.quokka.kotlin.internals.RecordingMeta
import kotlin.system.measureTimeMillis

class DatabaseTests {
    private val testMeta: RecordingMeta = Database.recordings.first()
    private val testStartFrame = testMeta.minFrame + (testMeta.maxFrame - testMeta.minFrame) / 3
    private val frameTimeSpan = 60
    private val maxTimeMust = 30
    private val maxTimeShould = 20

    companion object {
        @BeforeClass
        @JvmStatic
        fun warmupDatabase() {
            val testMeta = Database.recordings.first()
            val testStartFrame = testMeta.minFrame + (testMeta.maxFrame - testMeta.minFrame) / 3
            for (i in 0 until 10) {
                Database.getFrames(testMeta.id, testStartFrame, 50, framerate = 10)
            }
        }
    }

    @Test
    fun performanceMust() {
        val nFrames = frameTimeSpan * 5
        val millis = measureTimeMillis {
            Database.getFrames(testMeta.id, testStartFrame, nFrames, framerate = 5)
        }
        assertTrue(millis < maxTimeMust * 1000)
    }

    @Test
    fun performanceShould() {
        val nFrames = frameTimeSpan * 10
        val millis = measureTimeMillis {
            Database.getFrames(testMeta.id, testStartFrame, nFrames, framerate = 10)
        }
        assertTrue(millis < maxTimeShould * 1000)
    }
}