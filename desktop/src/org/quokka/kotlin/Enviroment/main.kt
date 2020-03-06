package com.mygdx.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration

fun main(){
    val config = LwjglApplicationConfiguration()
    config.height = 1600
    config.width = 1900
    LwjglApplication(Space(), config)
}