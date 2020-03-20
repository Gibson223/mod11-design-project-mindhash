package org.quokka.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import org.quokka.kotlin.config.GlobalConfig
import org.quokka.kotlin.config.Resolution

fun main() {
    val config = LwjglApplicationConfiguration()
    GlobalConfig.resolution = Resolution(1920, 1080)
    config.height = GlobalConfig.resolution.height
    config.width = GlobalConfig.resolution.width
    config.fullscreen = GlobalConfig.fullscreen
    LwjglApplication(GameInitializer, config)
}