package org.quokka.game.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import org.quokka.kotlin.environment.Space
import org.quokka.Screens.IndexScreen
import org.quokka.kotlin.environment.Settings


object GameInitializer : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var space : Space
    lateinit var settings : Settings
    lateinit var click : Sound

    override fun create() {
        click = Gdx.audio.newSound(Gdx.files.internal("click.mp3"))
        batch = SpriteBatch()
        font = BitmapFont() //
        //Use LibGDX's default Arial font.
        settings = Settings()
        this.setScreen(IndexScreen())
    }

    override fun render() {
        super.render() //important!
    }

    fun updateUsedSpace(recordingId: Int, local: Boolean){
        space = Space(recordingId = recordingId, local = local)
        this.setScreen(space)
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
        batch.dispose()
        font.dispose()
    }
}
