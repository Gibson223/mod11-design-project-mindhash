package org.quokka.kotlin.internals

import LidarData.Database
import LidarData.LidarFrame
import LidarData.RecordingMeta
import org.quokka.kotlin.config.GlobalConfig
import java.lang.Exception
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class Buffer(val recordingId: Int) {

    // Meta data for the recording
    val recordingMeta: RecordingMeta

    // How many frames to fetch with the fetch function
    var currFrame: Int
    private val playQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val delQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val queryLock = ReentrantLock()


    init {
        val rec = Database.getRecording(recordingId)
        if (rec != null) {
            recordingMeta = rec
        } else {
            throw NoRecordingException("No recording found with id '$recordingId'")
        }

        currFrame = recordingMeta.minFrame

        // Update buffers to fill them up initially
        updateBuffers()
    }

    /**
     * Fetch a single frame from the buffer and forwards it by one frame.
     *
     * @return The next frame in the buffer.
     */
    fun nextFrame(): LidarFrame? {
        val frame = playQueue.poll()
        frame?.let {
            delQueue.offer(it)
        }

        thread {
            updateBuffers()
        }

        return frame
    }

    fun lastFrame(): LidarFrame? {
        val frame = delQueue.pollFirst()
        frame?.let {
            playQueue.offer(it)
        }
        return frame
    }

    private fun fullCleanBuffers() {
        delQueue.clear()
        playQueue.clear()
    }

    /**
     * Clean up the buffers and fit them to size with a query if needed.
     */
    @Synchronized
    private fun updateBuffers() {
        if (!queryLock.tryLock()) {
            println("Already querying")
            return
        }
        println(this)

        // Calculate number of frames per buffer
        val framePerBuffer = GlobalConfig.bufferSize * GlobalConfig.lidarFps.fps


        // Remove or add frames to the queue
        if (playQueue.size >= framePerBuffer) {
            for (i in 0 until playQueue.size - framePerBuffer) {
                playQueue.pollLast()
            }
        } else if (playQueue.size <= framePerBuffer * 0.8) {
            val lastId = playQueue.peekLast()?.frameId ?: recordingMeta.minFrame
            if (lastId < recordingMeta.maxFrame) {
                println("Fetching more frames")
                playQueue.addAll(Database.getFrames(
                        recordingId = recordingId,
                        startFrame = lastId,
                        numberOfFrames = framePerBuffer - playQueue.size,
                        framerate = GlobalConfig.lidarFps))
            }
        }

        // Clean up history buffer
        for (i in 0 until delQueue.size - framePerBuffer / 2) {
            delQueue.pollFirst()
        }

        queryLock.unlock()
    }

    /**
     * Set the currFrame to a percentage of the recording based on min and max frame.
     *
     * @param prec The percentage to which the recording should be set. Has to be between 0 and 1.
     */
    fun setPosition(prec: Double) {
    }

    override fun toString(): String {
        val s = "Buffer { recordingId=${recordingId}, playQueueSize=${playQueue.size}, delQueueSize=${delQueue.size}" +
                " ,framesPerBuffer=${GlobalConfig.lidarFps.fps * GlobalConfig.bufferSize}, lastFrameId=${playQueue.peekFirst()?.frameId}}"

        return s
    }
}

class NoRecordingException(msg: String) : Exception(msg)
