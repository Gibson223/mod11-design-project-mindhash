package org.quokka.kotlin.internals

import com.badlogic.gdx.Gdx
import java.lang.Exception
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class Buffer(val recordingId: Int) {
    companion object {
        // Number of frames to be queried per update
        const val FRAMES_PER_QUERY = 20

        // The maximum size of the buffer in seconds
        // TODO this should be part of the preferences
        const val BUFFER_SIZE_S = 40
    }

    /**
     * Use this object to get access to all the meta data of the current buffer.
     */
    val recordingMetaData: RecordingMeta

    /**
     * The current progress in the recording as a value between 0 and 1
     */
    val progress: Float
        get() = playQueue.peekFirst()?.frameId?.toFloat() ?: recordingMetaData.maxFrame.toFloat()

    /**
     * These variables are for meta data on the current buffer and are updated every time a frame is retrieved.
     */
    var lastFrameIndex: Int
    var futureBufferSize: Int
    var pastBufferSize: Int

    // How many frames to fetch with the fetch function
    @Volatile
    private var skipToFrameIndex: Int?
    private val framesPerBuffer: Int
        get() = BUFFER_SIZE_S * prefs.getInteger("LIDAR FPS")
    private val playQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val delQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val prefs = Gdx.app.getPreferences("My Preferences")

    /*
     * This lock prevents the updateBuffers() function from running simultaneously
     */
    private val queryLock = ReentrantLock()

    /*
     * This lock prevents the queues from being modified at the same time. For example when a user spams the skip
     * buttons.
     */
    private val skipLock = ReentrantLock()


    init {
        println("Initializing buffer with $framesPerBuffer max frames")
        val rec = Database.getRecording(recordingId)
        if (rec != null) {
            recordingMetaData = rec
        } else {
            throw NoRecordingException("No recording found with id '$recordingId'")
        }

        skipToFrameIndex = null
        lastFrameIndex = recordingMetaData.minFrame

        // Update buffers to fill them up initially
        updateBuffers()
        futureBufferSize = playQueue.size
        pastBufferSize = playQueue.size
    }

    /**
     * Fetch a single frame from the buffer and forwards it by one frame.
     *
     * @return The next frame in the buffer if it exists.
     */
    fun nextFrame(): LidarFrame? {
        try {
            skipLock.lock()

            val frame = playQueue.poll()
            frame?.let {
                updateMeta(frame)
                delQueue.offer(it)
            }

            thread {
                updateBuffers()
            }

            return frame
        } finally {
            skipLock.unlock()
        }
    }

    /**
     * Fetch a single frame from the buffer and reverts it by one frame.
     *
     * @return The previous frame in the buffer if it exists.
     */
    fun prevFrame(): LidarFrame? {
        try {
            skipLock.lock()
            val frame = delQueue.pollFirst()
            frame?.let {
                updateMeta(frame)
                playQueue.offer(it)
            }
            return frame

        } finally {
            skipLock.unlock()
        }
    }

    /**
     * Skip forward a certain amount.
     *
     * @param seconds The number of seconds to skip.
     */
    fun skipForward(seconds: Float) {
        try {
            skipLock.lock()

            val framesToSkip = (seconds * prefs.getInteger("LIDAR FPS")).toInt()
            val targetFrame = lastFrameIndex + framesToSkip
            val lastFrameAvailable = playQueue.peekLast()?.frameId

            if (lastFrameAvailable == null || targetFrame > lastFrameAvailable) {
                // Target frame is not in buffer
                println("Skipping to $targetFrame from $lastFrameIndex")
                skipTo(targetFrame)
                return
            }

            // Number of extra frames that have to be fetched
            for (i in 0 until framesToSkip) {
                val frame = playQueue.poll()
                if (frame != null) {
                    delQueue.offer(frame)
                } else {
                    break
                }
            }
        } finally {
            skipLock.unlock()
        }
    }

    /**
     * Skip backward a certain amount.
     *
     * @param seconds The number of seconds to skip.
     */
    fun skipBackward(seconds: Float) {
        try {
            skipLock.lock()
            val framesToSkip = (seconds * prefs.getInteger("LIDAR FPS")).toInt()
            val targetFrame = lastFrameIndex - framesToSkip
            val firstFrameAvailable = delQueue.peekFirst()?.frameId

            if (firstFrameAvailable == null || firstFrameAvailable > targetFrame) {
                // Target frame is not in buffer
                println("Skipping to $targetFrame from $lastFrameIndex")
                skipTo(targetFrame)
                return
            }

            for (i in 0 until framesToSkip) {
                // Get the latest frame
                val frame = delQueue.pollLast()
                if (frame != null) {
                    playQueue.offerFirst(frame)
                } else {
                    break
                }
            }
        } finally {
            skipLock.unlock()
        }
    }

    /**
     * Skip to a specific frame index and reload the buffer. This may take a while and blocks the thread.
     * If the frameIndex value is not in the bounds, stuff may break.
     *
     * @param frameIndex Which frame to skip to.
     */
    fun skipTo(frameIndex: Int) {
        skipToFrameIndex = frameIndex
        updateBuffers()
        updateMeta()
    }

    /**
     * Skip to a certain percentage indicated by a float between 0 and 1. If the value is below 0 or above 1 it will be
     * capped.
     *
     * @param percentage The percentage of the recording to which the progress should be set.
     */
    fun skipTo(percentage: Float) {
        var perc = percentage
        if (perc < 0f)
            perc = 0f
        if (perc > 1f)
            perc = 1f

        val totalFrames = recordingMetaData.maxFrame - recordingMetaData.minFrame
        skipTo((recordingMetaData.minFrame + totalFrames * perc).toInt())
    }

    override fun toString(): String {
        val s = "Buffer { recordingId=${recordingId}, playQueueSize=${playQueue.size}, delQueueSize=${delQueue.size}" +
                ", framesPerBuffer=${prefs.getInteger("LIDAR FPS") * BUFFER_SIZE_S}" +
                ", lastFrameId=${playQueue.peekFirst()?.frameId}}"

        return s
    }

    /**
     * Formats the frame id's of the current data in the buffer as a string.
     *
     * @return A string representing the current contents of the buffer.
     */
    fun bufferData(): String {
        val nextBuf = StringBuilder()
        val prevBuf = StringBuilder()
        try {
            queryLock.lock()
            nextBuf.append(playQueue.joinToString(prefix = "{", postfix = "}") {
                it.frameId.toString()
            })
            prevBuf.append(delQueue.joinToString(prefix = "{", postfix = "}") {
                it.frameId.toString()
            })
        } finally {
            queryLock.unlock()
        }
        return "Play Buffer: ${nextBuf}\nHistory Buffer: $prevBuf"
    }


    /*
     * Private functions
     */
    private fun updateMeta(frame: LidarFrame? = null) {
        var fr = frame
        if (fr == null) {
            fr = playQueue.peekLast() ?: delQueue.peekFirst()
        }

        fr?.let {
            lastFrameIndex = it.frameId
        }
        futureBufferSize = playQueue.size
        pastBufferSize = delQueue.size
    }

    private fun fullCleanBuffers() {
        delQueue.clear()
        playQueue.clear()
    }

    /**
     * Clean up the buffers and fit them to size with a query if needed.
     */
    private fun updateBuffers() {
        if (!queryLock.tryLock()) {
            return
        }

        clearBuffers()

        queryLock.unlock()
    }

    private fun clearBuffers() {
        // Calculate number of frames per buffer


        // Remove or add frames to the queue
        if (playQueue.size >= framesPerBuffer) {
            for (i in 0 until playQueue.size - framesPerBuffer) {
                playQueue.pollLast()
            }
        } else if (playQueue.size <= framesPerBuffer * 0.8) {
            var lastId = playQueue.peekLast()?.frameId ?: lastFrameIndex
            // If the skipToFrameIndex is not null, clean everything and go there
            skipToFrameIndex?.let {
                println("Skipping")
                lastId = it
                fullCleanBuffers()
                // Reset it back to null after skipping is done
                skipToFrameIndex = null
            }

            if (lastId < recordingMetaData.maxFrame) {
                var frames = emptyList<LidarFrame>()

                while (frames.isEmpty()) {
                    frames = Database.getFrames(
                            recordingId = recordingId,
                            startFrame = lastId + 1,
                            numberOfFrames = FRAMES_PER_QUERY,
                            framerateInt = prefs.getInteger("LIDAR FPS"))
                    lastId += FRAMES_PER_QUERY
                }

                try {
                    skipLock.lock()
                    playQueue.addAll(frames)
                } finally {
                    skipLock.unlock()
                }
            }
        }

        // Clean up history buffer
        for (i in 0 until delQueue.size - framesPerBuffer) {
            delQueue.pollFirst()
        }
    }
}

class NoRecordingException(msg: String) : Exception(msg)
