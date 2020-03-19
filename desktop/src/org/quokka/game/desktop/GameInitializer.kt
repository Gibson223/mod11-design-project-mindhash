package org.quokka.game.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.BitmapFont

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mygdx.game.desktop.Space


class GameInitializer : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont


    override fun create() {
        batch = SpriteBatch()
        //Use LibGDX's default Arial font.
        font = BitmapFont()
        this.setScreen(Space())
    }

    override fun render() {
        super.render() //important!
    }

//    override fun pause() {
//        TODO("Not yet implemented")
//    }
//
//    override fun resume() {
//        TODO("Not yet implemented")
//    }
//
//    override fun resize(width: Int, height: Int) {
//        TODO("Not yet implemented")
//    }

    override fun dispose() {
        batch!!.dispose()
        font!!.dispose()
    }
}
