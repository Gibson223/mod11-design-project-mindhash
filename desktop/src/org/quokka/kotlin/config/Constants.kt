package org.quokka.kotlin.config


/**
 * TODO remove this in favor of integers from Gdx preferences
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

const val MAX_LIDAR_FPS = 20
