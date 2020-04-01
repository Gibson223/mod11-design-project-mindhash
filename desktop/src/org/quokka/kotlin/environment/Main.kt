package org.quokka.kotlin.environment

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import org.quokka.game.desktop.GameInitializer

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.height = 720
    config.width = 1280
    LwjglApplication(GameInitializer, config)
}
