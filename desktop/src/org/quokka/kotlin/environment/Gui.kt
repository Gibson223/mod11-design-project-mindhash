package org.quokka.kotlin.environment

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.mygdx.game.desktop.Space
import org.quokka.Screens.IndexScreen
import org.quokka.game.desktop.GameInitializer
import org.quokka.kotlin.internals.Database
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue

class Settings {
    val font = BitmapFont()
    val skin = Skin(Gdx.files.internal("Skins/glassy-ui.json"))

    // Swap these if no database connection, but why would you use these if you don't have one?
    // val files = mapOf<String, Int>("Train passby" to 1, "intersection" to 2, "road" to 3)
    val files = Database.recordings.associateBy({it.title}, {it.id})

    val shared_style = Label.LabelStyle(font, Color.WHITE)

    // labels
    val prefs = Gdx.app.getPreferences("My Preferences")

    val lidarFPS = Label("LIDAR FPS", shared_style)
    val lidar_box = SelectBox<Int>(skin)


    val playbackFPS = Label("PLAYBACK FPS", shared_style)
    val playback_slider = Slider(1f, 60f, 5f, false, skin)


    val memory = Label("MEMORY", shared_style)

    val resolution = Label("RESOLUTION", shared_style)
    val resolution_box = SelectBox<String>(skin)


    val compression = Label("COMPRESSION", shared_style)
    val compression_box = SelectBox<Int>(skin)


    val gradualCompression = Label("GRADUAL COMPRESSION", shared_style)
    val gradualBox = CheckBox("", skin)


    val distance = Label("DISTANCE (DFCM)", shared_style)
    val distance_field = TextField("", skin) // Todo: dfcm

    val fixedCamera = Label("FIXED CAMERA", shared_style)
    val camera_checkbox = CheckBox("", skin)

    val back_button = TextButton("BACK", skin)
    val save_button = TextButton("SAVE", skin)


    val dialog = Dialog("", skin)

    init {
        lidar_box.setItems(5, 10, 20)
        lidar_box.selected = prefs.getInteger("LIDAR FPS", 10)
        distance_field.textFieldFilter = TextField.TextFieldFilter.DigitsOnlyFilter()

        playback_slider.value = prefs.getFloat("PLAYBACK FPS", 0f)
        resolution_box.setItems("1920x1080", "1080x720", "FULLSCREEN")
        resolution_box.selected = prefs.getString("RESOLUTION", "1080x720")
        camera_checkbox.isChecked = prefs.getBoolean("FIXED CAMERA", true)

        compression_box.setItems(1, 2, 3, 4)
        compression_box.selected = prefs.getInteger("COMPRESSION", 4)
        gradualBox.isChecked = prefs.getBoolean("GRADUAL COMPRESSION", false)
        distance_field.text = prefs.getInteger("DFCM",15).toString()

        camera_checkbox.isChecked = prefs.getBoolean("FIXED CAMERA", false)

        dialog.setSize(200f, 250f)
        dialog.setPosition(Gdx.graphics.width / 2 - 100f, Gdx.graphics.height / 2 - 101f)
        dialog.contentTable.defaults().pad(10f)
        dialog.color = Color(Color.GRAY.r, Color.GRAY.g, Color.GRAY.b, 1f)


        dialog.contentTable.add(Label("PREFERENCES", shared_style))
        dialog.contentTable.row()

        dialog.contentTable.add(lidarFPS)
        dialog.contentTable.add(lidar_box)
        dialog.contentTable.row()
        dialog.contentTable.add(playbackFPS)
        dialog.contentTable.add(playback_slider)
        dialog.contentTable.row()
        dialog.contentTable.add(memory)
        dialog.contentTable.row()
        dialog.contentTable.add(resolution)
        dialog.contentTable.add(resolution_box)
        dialog.contentTable.row()
        dialog.contentTable.add(compression)
        dialog.contentTable.add(compression_box)
        dialog.contentTable.row()
        dialog.contentTable.add(fixedCamera)
        dialog.contentTable.add(camera_checkbox)
        dialog.contentTable.row()
        dialog.contentTable.add(distance)
        dialog.contentTable.add(distance_field).width(50f)
        dialog.contentTable.row()
        dialog.contentTable.add(gradualCompression)
        dialog.contentTable.add(gradualBox)
        dialog.contentTable.row()

        back_button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                println("quit settings menu")
                updateSpace()
                GameInitializer.space.resume()
                dialog.hide()

            }
        })

        save_button.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                println("saved settings (MOCK)")
                flushall()
            }
        })

        dialog.contentTable.add(back_button)
        dialog.contentTable.add(save_button)

    }

    fun updateSpace(){
        GameInitializer.space.changeLidarFPS(lidar_box.selected)
        GameInitializer.space.changePlaybackFPS(playback_slider.value.toInt())
        if (resolution_box.selected == "FULLSCREEN") {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        } else {
            val (wi, hei) = resolution_box.selected.split("x")
            GameInitializer.space.changeResolution(hei.toInt(), wi.toInt())
        }
        GameInitializer.space.switchFixedCamera(camera_checkbox.isChecked)

        GameInitializer.space.changeCompression(compression_box.selected)
        GameInitializer.space.switchGradualCompression(gradualBox.isChecked)
        GameInitializer.space.changeDFCM(distance_field.text.toInt())


    }

    private fun flushall() {
        prefs.putInteger("LIDAR FPS", lidar_box.selected)
        prefs.putFloat("PLAYBACK FPS", playback_slider.value)
        prefs.putString("RESOLUTION", resolution_box.selected)
        prefs.putBoolean("FIXED CAMERA", camera_checkbox.isChecked)

        prefs.putInteger("COMPRESSION", compression_box.selected)
        prefs.putBoolean("GRADUAL COMPRESSION", gradualBox.isChecked)
        prefs.putBoolean("FIXED CAMERA", camera_checkbox.isChecked)
        prefs.putInteger("DFCM", distance_field.text.toInt())

        prefs.flush()

    }
}

fun GuiButtons(space: Space, frameDivider: Int) {
//http://soundbible.com/1705-Click2.html
    val settings = space.settings

    val home_button: Image = Image(Texture("Screen3D/home_button.png"))
    val settings_button: Image = Image(Texture("Screen3D/setting_button.png"))

    val settings_dialog = settings.dialog
    val reset_button = Image(Texture("Screen3D/reset_button.png"))

    val pause_button: Image = Image(Texture("Screen3D/pause_button.png"))
    val earth_button: Image = Image(Texture("Screen3D/earth_button.png"))
    val arrows_button: Image = Image(Texture("Screen3D/arrows_button.png"))
    val ff_button: Image = Image(Texture("Screen3D/ff_button.png"))
    val bf_button: Image = Image(Texture("Screen3D/bf_button.png"))
    val plus = Image(Texture("Screen3D/plus.png"))
    val minus = Image(Texture("Screen3D/minus.png"))
    val scaleMinusPlus = 0.2f

    space.stage.addActor(minus)
    minus.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked zoom out")
            GameInitializer.click.play()
            space.moveBackward(Gdx.graphics.deltaTime)
        }
    })

    plus.setScale(scaleMinusPlus)
    plus.setPosition(minus.x, minus.y + minus.height * scaleMinusPlus)
    space.stage.addActor(plus)
    plus.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked zoom in")
            GameInitializer.click.play()
            space.moveForward(Gdx.graphics.deltaTime)
        }
    })

    space.stage.addActor(bf_button)
    bf_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked BF")
            GameInitializer.click.play()
            space.skipBackwards10Frames()
        }
    })

    space.stage.addActor(ff_button)
    ff_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked FF")
            GameInitializer.click.play()
            space.skipForward10frames()
        }
    })

    space.stage.addActor(arrows_button)
    arrows_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            GameInitializer.click.play()
        }
        override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
            super.touchDragged(event, x, y, pointer)
            val o = x - 110
            val l = y - 110
            val delta = Gdx.graphics.deltaTime
            if(o.absoluteValue < l.absoluteValue){
                if(l>0){
                    space.moveUp(delta)
                    space.zoomFixedCloser(delta)
                } else {
                    space.moveDown(delta)
                    space.zoomFixedAway(delta)
                }
            } else {
                if (o < 0){
                    space.moveLeft(delta)
                    space.rotateFixedLeft(delta)
                } else {
                    space.moveRight(delta)
                    space.rotateFixedRight(delta)
                }
            }
        }
    })


    space.stage.addActor(earth_button)
    earth_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            println("earth clicked")
            GameInitializer.click.play()

        }

        override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
            super.touchDragged(event, x, y, pointer)
            val o = x - 50
            val l = y - 50
            val delta = Gdx.graphics.deltaTime
            if (o > 0){
                space.rotateRight(delta*o/10)
                space.rotateFixedRight(delta*o/10)
            }
            if (o < 0){
                space.rotateLeft(delta*(-1)*o/10)
                space.rotateFixedLeft(delta*(-1)*o/10)
            }
            if (l > 0){
                space.rotateUp(delta*l/10)
                space.moveFixedUp(delta*l/10)
            }
            if (l < 0){
                space.rotateDown(delta*(-1)*l/10)
                space.moveFixedDown(delta*(-1)*l/10)
            }
//            if(o.absoluteValue < l.absoluteValue){
//                if(l>0){
//                    space.rotateUp(delta)
//                } else {
//                    space.rotateDown(delta)
//                }
//            } else {
//                if (o < 0){
//                    space.rotateLeft(delta)
//                } else {
//                    space.rotateRight(delta)
//                }
//            }
        }
    })


    space.stage.addActor(pause_button)
    pause_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked PAUSE")
            GameInitializer.click.play()
            space.pause.set(!space.pause.get())
        }
    })

    space.stage.addActor(reset_button)
    reset_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked RESET")
            GameInitializer.click.play()
            space.resetCamera()
        }
    })

    space.stage.addActor(settings_button)
    settings_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked SETTINGS and opened settings")
            GameInitializer.click.play()
            space.pause()
            settings_dialog.show(space.stage)
        }
    })
    settings.back_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            settings.updateSpace()
            GameInitializer.click.play()
        }

    })

    space.stage.addActor(home_button)
    home_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked HOME")
            GameInitializer.click.play()
            GameInitializer.screen = IndexScreen()
        }
    })

    minus.setScale(scaleMinusPlus)
    minus.setPosition(Gdx.graphics.width - minus.width * scaleMinusPlus, Gdx.graphics.height * 0.3f)
    plus.setScale(scaleMinusPlus)
    plus.setPosition(minus.x, minus.y + minus.height * scaleMinusPlus)

    pause_button.setPosition(Gdx.graphics.width /2 - (pause_button.width /2), 50f)
    bf_button.setPosition(pause_button.x - bf_button.width, 50f)
    ff_button.setPosition(pause_button.x + pause_button.width, 50f)

    arrows_button.setPosition(0f, 0f)
    earth_button.setPosition(Gdx.graphics.width*0.95f - earth_button.width, Gdx.graphics.height*(1/12f))
    home_button.setPosition(0.toFloat(), Gdx.graphics.height - 101.toFloat())
    settings_button.setPosition(Gdx.graphics.width - settings_button.width, Gdx.graphics.height -settings_button.height)
    reset_button.setPosition(settings_button.x, settings_button.y - reset_button.height)

    var left_bar = Image(Texture("Screen3D/left_bar.png"))
    if (frameDivider == 11) {
        left_bar = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    left_bar.setPosition(Gdx.graphics.width / 2 - 323.toFloat(), 10f)
    left_bar.setName("leftBar")
    space.stage.addActor(left_bar)

    var middle_bar1 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 12) {
        middle_bar1 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar1.setPosition(Gdx.graphics.width / 2 - 300.toFloat(), 10f)
    middle_bar1.setName("middleBar1")
    space.stage.addActor(middle_bar1)

    var middle_bar2 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 13) {
        middle_bar2 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar2.setPosition(Gdx.graphics.width / 2 - 275.toFloat(), 10f)
    middle_bar2.setName("middleBar2")
    space.stage.addActor(middle_bar2)

    var middle_bar3 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 14) {
        middle_bar3 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar3.setPosition(Gdx.graphics.width / 2 - 250.toFloat(), 10f)
    middle_bar3.setName("middleBar3")
    space.stage.addActor(middle_bar3)

    var middle_bar4 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 15) {
        middle_bar4 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar4.setPosition(Gdx.graphics.width / 2 - 225.toFloat(), 10f)
    middle_bar4.setName("middleBar4")
    space.stage.addActor(middle_bar4)

    var middle_bar5 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 16) {
        middle_bar5 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar5.setPosition(Gdx.graphics.width / 2 - 200.toFloat(), 10f)
    middle_bar5.setName("middleBar5")
    space.stage.addActor(middle_bar5)

    var middle_bar6 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 17) {
        middle_bar6 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar6.setPosition(Gdx.graphics.width / 2 - 175.toFloat(), 10f)
    middle_bar6.setName("middleBar6")
    space.stage.addActor(middle_bar6)

    var middle_bar7 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 18) {
        middle_bar7 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar7.setPosition(Gdx.graphics.width / 2 - 150.toFloat(), 10f)
    middle_bar7.setName("middleBar7")
    space.stage.addActor(middle_bar7)

    var middle_bar8 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 19) {
        middle_bar8 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar8.setPosition(Gdx.graphics.width / 2 - 125.toFloat(), 10f)
    middle_bar8.setName("middleBar8")
    space.stage.addActor(middle_bar8)

    var middle_bar9 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 20) {
        middle_bar9 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar9.setPosition(Gdx.graphics.width / 2 - 100.toFloat(), 10f)
    middle_bar9.setName("middleBar9")
    space.stage.addActor(middle_bar9)

    var middle_bar10 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 21) {
        middle_bar10 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar10.setPosition(Gdx.graphics.width / 2 - 75.toFloat(), 10f)
    middle_bar10.setName("middleBar10")
    space.stage.addActor(middle_bar10)

    var middle_bar11 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 22) {
        middle_bar11 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar11.setPosition(Gdx.graphics.width / 2 - 50.toFloat(), 10f)
    middle_bar11.setName("middleBar11")
    space.stage.addActor(middle_bar11)

    var middle_bar12 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 23) {
        middle_bar12 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar12.setPosition(Gdx.graphics.width / 2 - 25.toFloat(), 10f)
    middle_bar12.setName("middleBar12")
    space.stage.addActor(middle_bar12)

    var middle_bar13 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 24) {
        middle_bar13 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar13.setPosition(Gdx.graphics.width / 2.toFloat(), 10f)
    middle_bar13.setName("middleBar13")
    space.stage.addActor(middle_bar13)

    var middle_bar14 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 25) {
        middle_bar14 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar14.setPosition(Gdx.graphics.width / 2 + 25.toFloat(), 10f)
    middle_bar14.setName("middleBar14")
    space.stage.addActor(middle_bar14)

    var middle_bar15 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 26) {
        middle_bar15 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar15.setPosition(Gdx.graphics.width / 2 + 50.toFloat(), 10f)
    middle_bar15.setName("middleBar15")
    space.stage.addActor(middle_bar15)

    var middle_bar16 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 27) {
        middle_bar16 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar16.setPosition(Gdx.graphics.width / 2 + 75.toFloat(), 10f)
    middle_bar16.setName("middleBar16")
    space.stage.addActor(middle_bar16)

    var middle_bar17 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 28) {
        middle_bar17 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar17.setPosition(Gdx.graphics.width / 2 + 100.toFloat(), 10f)
    middle_bar17.setName("middleBar17")
    space.stage.addActor(middle_bar17)

    var middle_bar18 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 29) {
        middle_bar18 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar18.setPosition(Gdx.graphics.width / 2 + 125.toFloat(), 10f)
    middle_bar18.setName("middleBar18")
    space.stage.addActor(middle_bar18)

    var middle_bar19 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 30) {
        middle_bar19 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar19.setPosition(Gdx.graphics.width / 2 + 150.toFloat(), 10f)
    middle_bar19.setName("middleBar19")
    space.stage.addActor(middle_bar19)

    var middle_bar20 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 31) {
        middle_bar20 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar20.setPosition(Gdx.graphics.width / 2 + 175.toFloat(), 10f)
    middle_bar20.setName("middleBar20")
    space.stage.addActor(middle_bar20)

    var middle_bar21 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 32) {
        middle_bar21 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar21.setPosition(Gdx.graphics.width / 2 + 200.toFloat(), 10f)
    middle_bar21.setName("middleBar21")
    space.stage.addActor(middle_bar21)

    var middle_bar22 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 33) {
        middle_bar22 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar22.setPosition(Gdx.graphics.width / 2 + 225.toFloat(), 10f)
    middle_bar22.setName("middleBar22")
    space.stage.addActor(middle_bar22)

    var middle_bar23 = Image(Texture("Screen3D/middle_bar.png"))
    if (frameDivider == 34) {
        middle_bar23 = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    middle_bar23.setPosition(Gdx.graphics.width / 2 + 250.toFloat(), 10f)
    middle_bar23.setName("middleBar23")
    space.stage.addActor(middle_bar23)

    var right_bar = Image(Texture("Screen3D/right_bar.png"))
    if (frameDivider == 35) {
        right_bar = Image(Texture("Screen3D/onVideo_bar.png"))
    }
    right_bar.setPosition(Gdx.graphics.width / 2 + 275.toFloat(), 11f)
    right_bar.setName("rightBar")
    space.stage.addActor(right_bar)
}