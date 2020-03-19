package org.quokka.kotlin.Enviroment

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.mygdx.game.desktop.Space

fun save_settings(setdialog: Dialog){
    val prefs: Preferences = Gdx.app.getPreferences("MindhashPrefs")

}

fun settingsdialog(space: Space): Dialog {
    val font = BitmapFont()
    val skin = Skin(Gdx.files.internal("Skins/glassy-ui.json"))

    val shared_style = Label.LabelStyle(font, Color.WHITE)


    // labels
    val lidar = Label("LIDAR FPS", shared_style)
    val playback = Label("PLAYBACK FPS",shared_style)
    val memory = Label("MEMORY",shared_style)
    val resolution = Label("RESOLUTION",shared_style)
    val compression = Label("COMPRESSION",shared_style)
    val distance = Label("DISTANCE",shared_style)
    val fixed_camera = Label("FIXED CAMERA", shared_style)

    val lidar_box =  SelectBox<Int>(skin)
    val camera_checkbox = CheckBox("", skin)
    val compression_box =  SelectBox<Int>(skin)
    val resolution_box =  SelectBox<String>(skin)
    val playback_slider = Slider(1f, 60f, 5f,false,  skin)
    val distance_field = TextField("", skin)

    val back_button = TextButton("BACK", skin)
    val save_button = TextButton("SAVE", skin)


    val dialog = Dialog("", skin)

    dialog.setSize(200f, 250f)
    dialog.setPosition(Gdx.graphics.width / 2 - 100f, Gdx.graphics.height / 2 - 101f)
    dialog.contentTable.defaults().pad(10f)
    dialog.color = Color(Color.GRAY.r, Color.GRAY.g, Color.GRAY.b, 1f)
    lidar_box.setItems(1, 2, 5, 10)
    lidar_box.setItems(0, 1, 2, 3, 4)
    resolution_box.setItems("1920x1080", "1080x720")
    compression_box.setItems(1,2,3,4)

    dialog.contentTable.add(Label("PREFERENCES", shared_style))
    dialog.contentTable.row()

    dialog.contentTable.add(lidar)
    dialog.contentTable.add(lidar_box)
    dialog.contentTable.row()
    dialog.contentTable.add(playback)
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
    dialog.contentTable.add(fixed_camera)
    dialog.contentTable.add(camera_checkbox)
    dialog.contentTable.row()
    dialog.contentTable.add(distance)
    dialog.contentTable.add(distance_field).width(50f)
    dialog.contentTable.row()


    back_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("quit settings menu")
            space.pause()
            dialog.hide()
        }
    })

    save_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("saved settings (MOCK)")
        }
    })

    dialog.contentTable.add(back_button)
    dialog.contentTable.add(save_button)

    return dialog
}

fun GuiButtons(space: Space){
    var bf_button: Image = Image(Texture("Screen3D/bf_button.png"))
    bf_button.setPosition(Gdx.graphics.width / 2 - 175.toFloat(), 0f)
    space.stage!!.addActor(bf_button)
    bf_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked BF")
        }
    })

    var ff_button: Image = Image(Texture("Screen3D/ff_button.png"))
    ff_button.setPosition(Gdx.graphics.width / 2 + 75.toFloat(), 0f)
    space.stage!!.addActor(ff_button)
    ff_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked FF")
        }
    })

    var arrows_button: Image = Image(Texture("Screen3D/arrows_button.png"))
    arrows_button.setPosition(Gdx.graphics.width - 251.toFloat(), 0f)
    space.stage!!.addActor(arrows_button)
    arrows_button.addListener(object: ClickListener(){
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            val x1 = arrows_button.width/3
            val x2 = 2*arrows_button.width/3
            val x3 = arrows_button.width

            val y1 = arrows_button.height/3
            val y2 = 2*arrows_button.height/3
            val y3 = arrows_button.height

            val delta = Gdx.graphics.deltaTime
            when {
                x < x1 && y < y1 -> println("q1")
                x < x2 && y < y1 -> space.rotateDown(delta)
                x < x3 && y < y1 -> println("q3")

                x < x1 && y < y2 -> space.rotateLeft(delta)
                x < x2 && y < y2 -> println("center")
                x < x3 && y < y2 -> space.rotateRight(delta)

                x < x1 && y < y3 -> println("q7")
                x < x2 && y < y3 -> space.rotateUp(delta)
                x < x3 && y < y3 -> println("q9")

            }
        }
    })



    var earth_button: Image = Image(Texture("Screen3D/earth_button.png"))
    earth_button.setPosition(Gdx.graphics.width - 185.toFloat(), 60f)
    space.stage!!.addActor(earth_button)
    earth_button.addListener(object: ClickListener(){
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            println("earth clicked")
        }
    })


    var pause_button: Image = Image(Texture("Screen3D/pause_button.png"))
    pause_button.setPosition(Gdx.graphics.width / 2 - 50.toFloat(), 0f)
    space.stage!!.addActor(pause_button)
    pause_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked PAUSE")
            space.pause()
        }
    })

    var reset_button: Image = Image(Texture("Screen3D/reset_button.png"))
    reset_button.setPosition(Gdx.graphics.width - 110.toFloat(), Gdx.graphics.height - 251.toFloat())
    space.stage!!.addActor(reset_button)
    reset_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked RESET")
            space.resetCamera()
        }
    })

    val settings_dialog = settingsdialog(space)

    var settings_button: Image = Image(Texture("Screen3D/setting_button.png"))
    settings_button.setPosition(Gdx.graphics.width - 110.toFloat(), Gdx.graphics.height - 101.toFloat())
    space.stage!!.addActor(settings_button)
    settings_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked SETTINGS and opened settings")
            settings_dialog.show(space.stage)
            space.pause()

        }
    })


    var home_button: Image = Image(Texture("Screen3D/home_button.png"))
    home_button.setPosition(0.toFloat(), Gdx.graphics.height - 101.toFloat())
    space.stage!!.addActor(home_button)
    home_button.addListener(object : ClickListener() {
        override fun clicked(event: InputEvent, x: Float, y: Float) {
            println("clicked HOME")
        }
    })


    fun changLidarFPS(newLFPS: Int){
        space.lidarFPS = newLFPS
        space.newLidaarFPS.set(true)
    }

    fun changePlaybackFPS(newFPS: Int){
        space.playbackFPS = newFPS
    }

    fun changeResolution(height: Int, width: Int){
        space.changeResolution(height,width)
    }

    fun switchFixedCamera(){
        space.fixedCamera == !space.fixedCamera
    }



}