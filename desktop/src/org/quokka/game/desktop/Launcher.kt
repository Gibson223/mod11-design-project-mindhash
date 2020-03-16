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
    LwjglApplication(Space(), config)
}