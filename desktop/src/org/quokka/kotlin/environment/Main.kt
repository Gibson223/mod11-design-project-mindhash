package com.mygdx.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import org.quokka.game.desktop.GameInitializer

fun main(){
    val config = LwjglApplicationConfiguration()
    config.height = 720
    config.width = 1080
    LwjglApplication(GameInitializer, config)
}