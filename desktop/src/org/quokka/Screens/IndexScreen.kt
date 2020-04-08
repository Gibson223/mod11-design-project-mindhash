package org.quokka.Screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import org.quokka.game.desktop.GameInitializer
import org.quokka.game.desktop.GameInitializer.settings
import org.quokka.game.desktop.GameInitializer.updateUsedSpace
import java.util.*
import kotlin.system.exitProcess
import org.quokka.kotlin.environment.drawBar

class IndexScreen : Screen {
    val img = Image(Texture("UTLogo.jpg"))
    val img2 = Image(Texture("mindhashLogo2.jpg"))
    val badge = Image(Texture("Startbutton.png"))


    val font: BitmapFont = BitmapFont()
    var stage: Stage = Stage(ScalingViewport(Scaling.stretch, 1280f, 720f))
    val skin = Skin(Gdx.files.internal("Skins/glassy-ui.json"))
    val selectBox = SelectBox<String>(skin)


    var images = listOf<Actor>(img, img2, badge, selectBox)

    fun create() {
        font.color = Color.BLACK
        font.data.setScale(2f)

        
        stage.addActor(img)
        stage.addActor(img2)

        stage.addActor(badge)
        stage.addActor(selectBox)


        selectBox.width = 300f
        selectBox.height = 100f

        val files = ArrayList(settings.files.keys).toTypedArray()
        println("files ${files[0]}")
        selectBox.setItems(*files)

        badge.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                println("Clicked!")
                updateUsedSpace(settings.files.getValue(selectBox.selected), local = false)
            }
        })

        draw()
        }
    
    fun draw() {
        img.setPosition(0f, stage.viewport.screenHeight - img.height)
        img2.setPosition(stage.viewport.screenWidth - img2.width, stage.viewport.screenHeight - img2.height)
        badge.setPosition(stage.viewport.screenWidth /2 - badge.width / 2, stage.viewport.screenHeight*0.05f)
        selectBox.setPosition(stage.viewport.screenWidth/2f - selectBox.width/2,stage.viewport.screenHeight /2f + selectBox.height/2)
    }

    override fun show() {
        Gdx.gl.glClearColor(1f, 1f, 1f, 1f)
        Gdx.input.inputProcessor = stage
        println("openend indexscreen")
        create()
        stage.viewport.update(Gdx.graphics.width,Gdx.graphics.height)

    }

    override fun render(delta: Float) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        GameInitializer.batch.begin()
//        val glyph = GlyphLayout(font,"Press escape to exit the application" )
//        font.draw(GameInitializer.batch, glyph, stage.viewport.screenWidth/2 - glyph.width/2, img.y + img.height/2 )
        GameInitializer.batch.end()
        stage.act()
        stage.draw()
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
            exitProcess(0)
    }

    override fun resize(width: Int, height: Int) {
        stage.clear()
        stage.viewport.update(1280, 720, true)
        images.forEach { stage.addActor(it) }
        draw()
        stage.viewport.update(width, height, true);
    }
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        println("MyGdxGame disposed")
    }
}