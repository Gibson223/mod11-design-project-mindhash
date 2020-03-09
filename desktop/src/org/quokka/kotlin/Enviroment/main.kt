package com.mygdx.game.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration

fun main(){
    val config = LwjglApplicationConfiguration()
    config.height = 900
    config.width = 1800
    LwjglApplication(Space(), config)
}