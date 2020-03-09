package org.quokka.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.mygdx.game.desktop.Space

fun main() {
    val config = LwjglApplicationConfiguration()
    config.height = 1080
    config.width = 1920
    LwjglApplication(Space(), config)
}