package org.quokka.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.mygdx.game.desktop.Space
import org.quokka.kotlin.config.GlobalConfig

fun main() {
    val config = LwjglApplicationConfiguration()
    config.height = GlobalConfig.resolution.height
    config.width = GlobalConfig.resolution.width
    config.fullscreen = GlobalConfig.fullscreen
    LwjglApplication(Space(recordingId = 1, compressed = false, local = false, axis = false), config)
    //LwjglApplication(Space(recordingId = 1, compressed = false, local = true, path = "core/assets/sample.bag",axis = false), config)
}