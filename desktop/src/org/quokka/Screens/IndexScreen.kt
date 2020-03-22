package org.quokka.Screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import org.quokka.game.desktop.GameInitializer
import org.quokka.game.desktop.GameInitializer.settings
import org.quokka.game.desktop.GameInitializer.updateUsedSpace
import java.util.*
import kotlin.system.exitProcess

class IndexScreen : Screen {
    var img: Image
    var img2: Image
    val font: BitmapFont = BitmapFont()
    var stage: Stage
    var skin: Skin

    init {
        font.color = Color.BLACK
        font.data.setScale(2f)
        skin = Skin(Gdx.files.internal("Skins/glassy-ui.json"))

        stage = Stage()
        img = Image(Texture("UTLogo.jpg"))
        img.setPosition(0f, Gdx.graphics.height - img.height)
        img2 = Image(Texture("mindhashLogo2.jpg"))
        img2.setPosition(Gdx.graphics.width - img2.width, Gdx.graphics.height - img2.height)

        stage.addActor(img)
        stage.addActor(img2)

        val badge = Image(Texture("Startbutton.png"))
        stage.addActor(badge)


        val selectBox = SelectBox<String>(skin)
        selectBox.width = 300f
        selectBox.height = 100f
        selectBox.setPosition(Gdx.graphics.width/2f - selectBox.width/2,Gdx.graphics.height /2f + selectBox.height/2)
        stage.addActor(selectBox)

        val files = ArrayList(settings.files.keys).toTypedArray()
        selectBox.setItems(*files)

        badge.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                println("Clicked!")
                updateUsedSpace(settings.files.getValue(selectBox.selected), local = false, axis = false)
            }
        })

        badge.setPosition(Gdx.graphics.width /2 - badge.width / 2, Gdx.graphics.height*0.05f)
        Gdx.input.inputProcessor = stage
    }

    override fun show() {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.input.inputProcessor = stage
        println("openend indexscreen")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        GameInitializer.batch.begin()
        val glyph = GlyphLayout(font,"Press escape to exit the application" )
        font.draw(GameInitializer.batch, glyph, Gdx.graphics.width/2 - glyph.width/2, img.y + img.height/2 )
        GameInitializer.batch.end()
        stage.act()
        stage.draw()
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
            exitProcess(0)
    }

    override fun resize(width: Int, height: Int) {
        stage.getViewport()?.update(width, height, true);
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        println("MyGdxGame disposed")
    }
}