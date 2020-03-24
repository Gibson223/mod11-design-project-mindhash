package org.quokka.kotlin.environment

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import com.mygdx.game.desktop.Space
import org.quokka.Screens.IndexScreen
import org.quokka.game.desktop.GameInitializer
import org.quokka.kotlin.internals.Buffer
import org.quokka.kotlin.internals.Database
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

fun GuiButtons(space: Space) {
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
            space.zoomFixedAway(Gdx.graphics.deltaTime)
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
            space.zoomFixedCloser(Gdx.graphics.deltaTime)
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
                    space.moveFixedUp(delta)
                } else {
                    space.moveDown(delta)
                    space.moveFixedDown(delta)
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
            val o = x - 70
            val l = y - 70
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
            space.resetFixed()
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
}

class drawBar(stage: Stage, val buffer: Buffer? = null){

    val left_bar = Image(Texture("Screen3D/left_bar.png"))
    val right_bar = Image(Texture("Screen3D/right_bar.png"))
    val button = Image(Texture("Screen3D/slider_button.png"))
    val parts = 20

    var bars = arrayListOf<Actor>()

    init {
        println("drawbar called")
        stage.addActor(button)
        button.addListener(object : DragListener() {
            override fun drag(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                val new_center = this@drawBar.button.x - this@drawBar.button.width / 2 + x
                if (new_center < left_bound || new_center > right_bound) {
                    println("out of bars")
                } else {
                    this@drawBar.button.moveBy(x - this@drawBar.button.width / 2, 0f);
                }
            }

            override fun dragStop(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                buffer!!
                println("dropped, $x, $y, ${this@drawBar.button.x}, ${this@drawBar.button.y}")
                var progress = this@drawBar.button.x + this@drawBar.button.width / 2 // current center button
                progress = (progress - left_bound) / (right_bound - left_bound)
                buffer.skipTo(progress)
            }
        })

        var x = Gdx.graphics.width / 2f - (parts/2 *25 /*pixels per middle bar*/)
        var y = 10f

        for (useless in 0 until parts) {
            val middle_bar = Image(Texture("Screen3D/middle_bar.png"))
            val loaded = Image(Texture("Screen3D/middle_barLoaded.png"))

            middle_bar.setPosition(x, y)
            stage.addActor(middle_bar)
            loaded.setPosition(x, y)
            stage.addActor(loaded)
            bars.add(middle_bar)
            x += 25
        }

        button.setPosition(bars.first().x - button.width / 2, bars.first().y + bars.first().height / 2 - button.height / 2)
        button.toFront()

    }

    val left_bound = bars[0].x - button.width / 2
    val right_bound = bars.last().x + button.width / 2

    fun update(){
        buffer!!
        if  (button.listeners.first() is DragListener && !(button.listeners.first() as DragListener).isDragging) {
            var perc = (buffer.lastFrameIndex - buffer.recordingMetaData.minFrame) /(buffer.recordingMetaData.maxFrame - buffer.recordingMetaData.minFrame).toFloat()
            if (perc < 0f)
                perc = 0f
            if (perc > 1f)
                perc = 1f
            val newX =  perc * (right_bound - left_bound) - button.width/2 + bars.first().x
            button.setPosition(newX, button.y)
        } else {
            if (!(button.listeners.first() as DragListener).isDragging) {
                println("problem")
            }
        }

    }

    fun up(){
        button.setPosition(button.x+1, button.y)
    }



}

class sliderBar(stage: Stage, val buffer: Buffer? = null){

    var slider = Slider(0f,1f,0.01f,false,Skin(Gdx.files.internal("Skins/glassy-ui.json")))

    init {
        slider.width = Gdx.graphics.width*0.6f
        slider.setPosition(Gdx.graphics.width*0.2f,15f)
        stage.addActor(slider)
        slider.addListener(object : DragListener() {
            override fun drag(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                if (x/slider.width + slider.percent < 0 || x/slider.width + slider.percent > 1) {
                    println("out of bounds")
                } else {
                    slider.value += x / slider.width
                }
            }
            override fun dragStop(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                buffer!!
                buffer.skipTo(slider.value)
            }
        })

    }

    fun update(){
        buffer!!
        var perc = (buffer.lastFrameIndex - buffer.recordingMetaData.minFrame) /(buffer.recordingMetaData.maxFrame - buffer.recordingMetaData.minFrame).toFloat()
        if (perc < 0f)
            perc = 0f
        if (perc > 1f)
            perc = 1f
        slider.value = perc
    }

    fun up(){
        slider.value += 0.01f
    }



}