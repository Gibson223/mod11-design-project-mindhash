package com.mygdx.game.desktop

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
import java.util.*
import kotlin.collections.ArrayList


internal class Space : InputAdapter(), ApplicationListener {

    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null

    var modelBatch: ModelBatch? = null


    var array: ArrayList<ModelInstance>? = null
    var instance: ModelInstance? = null

    var bottomBlock: Model? = null
    var proxi: Model? = null
    var pink: Texture? = null


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


        proxi = modelBuilder.createBox(.5f, .5f, .5f,
                Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong())


        randpop()

        // populate()

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



        Gdx.graphics.setContinuousRendering(false);
        Gdx.graphics.requestRendering();
    }



    override fun render() {
        camController!!.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch!!.begin(cam)
        pink!!.bind()
        modelBatch!!.render(array, environment)
        modelBatch!!.end()


        string!!.setLength(0)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        // string.append(" Visible: ").append(cam.position);
//        string!!.append(cam!!.combined)
        label!!.setText(string)
        stage!!.draw()

    }

    fun randpop(){
        array = ArrayList(53000)
        instance = ModelInstance(bottomBlock,0f,0f,0f)
        array!!.add(instance!!)
//        for (i in 0..53000){
//            var rand = (0..53000).shuffled()
//            instance = ModelInstance(proxi,randx.get(i)*1F,randy.get(i)*1F,randz.get(i)*1F)
//            array!!.add(instance!!)
//        }
        for (i in -50..50)
            for(j in -25..25)
                for (k in -5..5) {
                    instance = ModelInstance(proxi, i * 1f, j * 1f, k * 1f)
                    array!!.add(instance!!)
                }
    }

    fun populate(){
        array = ArrayList(1001)

        instance = ModelInstance(bottomBlock,0f,0f,0f)
        array!!.add(instance!!)
        for(i in -25..25){
            for (j in -25..25){
//                for (k in -25..25) {
                instance = ModelInstance(proxi, i * 1F, j * 1F, 1F)
                array!!.add(instance!!)
//                }
            }
//            instance = ModelInstance(proxi,randx+offzet+i,randy+i,10f)
//            array!!.add(instance!!)
        }
//        if(randx + offzet>50){
//            offzet = 0
//        }
//        offzet++

    }
    override fun dispose() {
        modelBatch!!.dispose()
        bottomBlock!!.dispose()
    }

    override fun resume() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
}