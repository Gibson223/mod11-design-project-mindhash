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

    /**
     * Use this object to get access to all the meta data of the current buffer.
     */
    val recordingMetaData: RecordingMeta

    /**
     * The current progress in the recording as a value between 0 and 1
     */
    val progress: Float
        get() = playQueue.peekFirst()?.frameId?.toFloat() ?: recordingMetaData.maxFrame.toFloat()

    // How many frames to fetch with the fetch function
    private var currFrame: Int
    private val framesPerBuffer: Int
        get() = GlobalConfig.bufferSize * GlobalConfig.lidarFps.fps
    private val playQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val delQueue = ConcurrentLinkedDeque<LidarFrame>()
    private val queryLock = ReentrantLock()


    init {
        val rec = Database.getRecording(recordingId)
        if (rec != null) {
            recordingMetaData = rec
        } else {
            throw NoRecordingException("No recording found with id '$recordingId'")
        }

        currFrame = recordingMetaData.minFrame

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

    fun prevFrame(): LidarFrame? {
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
            val lastId = playQueue.peekLast()?.frameId ?: currFrame
            if (lastId < recordingMetaData.maxFrame) {
                playQueue.addAll(Database.getFrames(
                        recordingId = recordingId,
                        startFrame = lastId,
                        numberOfFrames = framesPerBuffer - playQueue.size,
                        framerate = GlobalConfig.lidarFps))
            }
        }

        // Clean up history buffer
        for (i in 0 until delQueue.size - framesPerBuffer / 2) {
            delQueue.pollFirst()
        }
    }

    /**
     * Skip forward a certain amount.
     *
     * @param seconds The number of seconds to skip.
     */
    fun skipForward(seconds: Float) {
        try {
            queryLock.lock()
            val framesToSkip = (seconds * GlobalConfig.lidarFps.fps).toInt()
            val targetFrame = progress.toInt() + framesToSkip
            val lastFrameAvailable = playQueue.peekLast()?.frameId ?: delQueue.peekLast().frameId

            if (targetFrame > lastFrameAvailable) {
                skipTo(targetFrame)
                return
            }

            // Number of extra frames that have to be fetched
            var extraFrames = 0
            for (i in 0 until framesToSkip) {
                val frame = playQueue.poll()
                if (frame != null) {
                    delQueue.offer(frame)
                } else {
                    extraFrames = framesToSkip - i - 1
                    break
                }
            }

            if (extraFrames > 0) {

                val lastId = playQueue.peekLast()?.frameId ?: currFrame
                if (lastId < recordingMetaData.maxFrame) {
                    playQueue.addAll(Database.getFrames(
                            recordingId = recordingId,
                            startFrame = lastId,
                            numberOfFrames = extraFrames,
                            framerate = GlobalConfig.lidarFps))
                }
            }

            clearBuffers()
        } finally {
            queryLock.unlock()
        }
    }

    /**
     * Skip backward a certain amount.
     *
     * @param seconds The number of seconds to skip.
     */
    fun skipBackward(seconds: Float) {
        try {
            queryLock.lock()

            val framesToSkip = (seconds * GlobalConfig.lidarFps.fps).toInt()
            val targetFrame = progress.toInt() - framesToSkip
            val firstFrameAvailable = delQueue.peekLast()?.frameId ?: playQueue.peekFirst().frameId

            if (firstFrameAvailable - targetFrame > framesPerBuffer) {
                // The number of frames in the next buffer are not in the current history buffer
                // So just skip there
                skipTo(targetFrame)
                return
            }

            var extraFrames = 0
            for (i in 0 until framesToSkip) {
                // Get the latest frame
                val frame = delQueue.pollLast()
                if (frame != null) {
                    playQueue.offerFirst(frame)
                } else {
                    extraFrames = framesToSkip - i - 1
                    break
                }
            }
            // Append extra frames that were not in the buffer
            if (extraFrames > 0) {
                val id = playQueue.peekFirst()?.frameId ?: currFrame
                if (id < recordingMetaData.maxFrame) {
                    Database.getFrames(
                            recordingId = recordingId,
                            startFrame = id - extraFrames,
                            numberOfFrames = extraFrames,
                            framerate = GlobalConfig.lidarFps)
                            .reversed()
                            .forEach { playQueue.offerFirst(it) }
                }
            }

            // Clean up buffers
            clearBuffers()

        } finally {
            queryLock.unlock()
        }
    }

    /**
     * Skip to a specific frame index and reload the buffer. This may take a while and blocks the thread.
     * If the frameIndex value is not in the bounds, stuff may break.
     *
     * @param frameIndex Which frame to skip to.
     */
    fun skipTo(frameIndex: Int) {
        fullCleanBuffers()
        currFrame = frameIndex
        updateBuffers()
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
                " ,framesPerBuffer=${GlobalConfig.lidarFps.fps * GlobalConfig.bufferSize}, lastFrameId=${playQueue.peekFirst()?.frameId}}"

        return s
    }

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
        return "Play Buffer: ${nextBuf}\nHistory Buffer: ${prevBuf}"
    }
}

class NoRecordingException(msg: String) : Exception(msg)

fun main() {
    println("Initialized")
    val buff = Buffer(1)
    println("MetaData: ${buff.recordingMetaData}")
    println(buff.bufferData())

    println("Skip to 50%")
    buff.skipTo(0.5f)
    println(buff.bufferData())

    println("Fetch next frame")
    buff.nextFrame()
    println(buff.bufferData())

    println("Skip forward one second")
    buff.skipForward(1f)
    println(buff.bufferData())

    println("Skip backward one second")
    buff.skipBackward(1f)
    println(buff.bufferData())

    println("Skipping back 40 seconds into unloaded area")
    buff.skipBackward(40f)
    println(buff.bufferData())

    println("Skipping forward 100 seconds into unloaded area")
    buff.skipForward(100f)
    println(buff.bufferData())

    println("Skipping half out of bounds forward")
    buff.skipForward(5f)
    println(buff.bufferData())

    println("Skipping half out of bounds backward")
    buff.skipBackward(7.5f)
    println(buff.bufferData())
}