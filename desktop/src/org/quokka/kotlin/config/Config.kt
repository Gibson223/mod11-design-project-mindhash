package org.quokka.kotlin.config


/**
 * Global config singleton.
 */
object GlobalConfig {
    // Framerate at which the lidar data is queried. 10 is the max, 1 is the minimum.
    var lidarFps = LidarFps.TEN;
    // How fast the
    var playbackSpeed = 1.0;
    // The size of the buffer for stored frames in seconds.
    var bufferSize = 40;
    // Resolution of the screen
    var resolution = Resolution(1920, 1080)
    // Fullscreen or not
    var fullscreen = false
    // Compression level
    var compressionLevel = CompressionLevel.NONE
}

/**
 * An enum which defines the possible framerates when retrieving frames from the database.
 *
 * @property stepsPerFrame The number of frames skipped for the chosen framerate.
 */
enum class LidarFps(val fps: Int, val stepsPerFrame: Int) {
    TEN(10, 1),
    FIVE(5, 2),
    TWO(2, 5),
    ONE(1, 10)
}

/**
 * The compression level for grouping points together.
 * NONE: No compression at all.
 * DYNAMIC:
 *
 */
enum class CompressionLevel {
    NONE,
    DYNAMIC,
    TWO,
    THREE,
    FOUR
}

/**
 * Data struct to represent screen resolution.
 *
 * @property width Width of the display.
 * @property height Height of the display.
 */
data class Resolution(val width: Int, val height: Int)
