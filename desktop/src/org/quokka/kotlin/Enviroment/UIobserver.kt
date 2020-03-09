package org.quokka.kotlin.Enviroment

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.mygdx.game.desktop.Space
import java.util.*

class UIobserver: Runnable, Observable {

    var parent:Space? = null

    constructor(daddy:Space){
        parent = daddy
    }

    override fun run(){
        while (parent!!.getRunning() == true){}
            if (Gdx.input.isKeyPressed(Input.Keys.SPACE) == true){
                this.notifyObservers(Input.Keys.SPACE)
            }
    }



}