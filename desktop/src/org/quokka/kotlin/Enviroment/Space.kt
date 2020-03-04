package com.mygdx.game.desktop

import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.ModelLoader
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import org.quokka.kotlin.Enviroment.Populator
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.concurrent.timer


class Space : InputAdapter(), ApplicationListener {

var cam: PerspectiveCamera? = null
var plexer: InputMultiplexer? = null
var camController: CameraInputController? = null

var modelBatch: ModelBatch? = null


var spaceObjects: ArrayList<ModelInstance>? = null
var instance: ModelInstance? = null

var bottomBlock: Model? = null
var proxi: Model? = null
var pink: Texture? = null

var frames: ConcurrentLinkedQueue<LidarFrame>? = null


var environment: Environment? = null

var stage: Stage? = null
var font: BitmapFont? = null
var label: Label? = null
var string: StringBuilder? = null

override fun create() {
    modelBatch = ModelBatch()
    //-----------Camera Creation------------------
    cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    cam!!.position[30f, 30f] = 30f
    cam!!.lookAt(0f, 0f, 0f)
    cam!!.near = 1f
    cam!!.far = 100f
    cam!!.update()

    //---------Camera controls--------
    camController = CameraInputController(cam)
    Gdx.input.inputProcessor = camController
    environment = Environment()
    environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
    environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))



   spaceObjects = ArrayList<ModelInstance>(1)

    val populator = Populator(this)

    //---------Model Population----------
    var modelBuilder = ModelBuilder()

    modelBuilder.begin()
    modelBuilder.node().id = "Floor"
    pink = Texture(Gdx.files.internal("core/assets/badlogic.jpg"),false)
    pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
    pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    var material = Material(TextureAttribute.createDiffuse(pink))
    modelBuilder.end()


    bottomBlock = modelBuilder.createBox(10f, 10f, .5f,
        material,
        (VertexAttributes.Usage.Position or  VertexAttributes.Usage.Normal.toLong().toInt()).toLong())


    proxi = modelBuilder.createBox(.1f, .1f, .1f,
        Material(ColorAttribute.createDiffuse(Color.ORANGE)),
        (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong())




    // -----------Bottom Text--------
    stage = Stage()
    font = BitmapFont()
    label = Label(" ", LabelStyle(font, Color.WHITE))
    stage!!.addActor(label)
    val act = Actor()
    act.color = Color.BROWN
    stage!!.addActor(act)
    string = StringBuilder()


    plexer = InputMultiplexer(this as InputProcessor, camController)
    Gdx.input.inputProcessor = plexer


    filepop(1850,1860)

    Gdx.graphics.setContinuousRendering(false);
    Gdx.graphics.requestRendering();
}



override fun render() {
    camController!!.update()

    Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

    modelBatch!!.begin(cam)
    pink!!.bind()
    modelBatch!!.render(spaceObjects, environment)
    modelBatch!!.end()


    string!!.setLength(0)
    string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
    // string.append(" Visible: ").append(cam.position);
//        string!!.append(cam!!.combined)
    label!!.setText(string)
    stage!!.draw()

}


    fun filepop(start: Int, end: Int){
        val ldrrdr = LidarReader.DefaultReader()
        var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", start, end)


        timer("Array Creator", period = 10000){
            intermetidate.first().coords.forEach { f ->
                var instance = ModelInstance(
                        proxi,
                        1f * f.coords.first,
                        1f * f.coords.second,
                        1f * f.coords.third
                )
                spaceObjects!!.add(instance!!)
                intermetidate.drop(0)
            }
            Gdx.graphics.requestRendering();
            println("render requested")
        }
    }

    








    fun changeArray(x: ArrayList<ModelInstance>){
        this.spaceObjects = x
    }


    override fun dispose() {
        modelBatch!!.dispose()
        bottomBlock!!.dispose()
    }

    override fun resume() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
}


