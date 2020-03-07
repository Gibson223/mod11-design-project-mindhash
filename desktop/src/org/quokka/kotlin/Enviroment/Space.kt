package com.mygdx.game.desktop

import LidarData.Database
import LidarData.LidarCoord
import LidarData.LidarFrame
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import org.quokka.kotlin.Enviroment.Populator
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
    var framesIndex = 2400


    var environment: Environment? = null

    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null

    val database: Database
    var batch: DecalBatch? = null
    var decals: List<Decal> = listOf()
    var decalTextureRegion: TextureRegion? = null

    lateinit var blueYellowFade: Array<TextureRegion>

    init {
        database = Database()
        database.connect("lidar", "mindhash")
    }

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

        frames = ConcurrentLinkedQueue<LidarFrame>()
        //---------Model Population----------
        var modelBuilder = ModelBuilder()

        modelBuilder.begin()
        modelBuilder.node().id = "Floor"
        pink = Texture(Gdx.files.internal("core/assets/badlogic.jpg"), false)
        pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        var material = Material(TextureAttribute.createDiffuse(pink))
        modelBuilder.end()

        batch = DecalBatch(CameraGroupStrategy(cam))
        val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
        pix.setColor(66f/255, 135f/255, 245f/255, 1f)
        pix.drawPixel(0, 0)
        val pixtex = Texture(pix)
        decalTextureRegion = TextureRegion(pixtex)
        blueYellowFade = Array(256) { i ->
            val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
            pix.setColor(i / 255f, i / 255f, 1 - i / 255f, 1f)
            pix.drawPixel(0, 0)
            TextureRegion(Texture(pix))
        }




        bottomBlock = modelBuilder.createBox(
            10f, 10f, .5f,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )


        proxi = modelBuilder.createBox(
            .1f, .1f, .1f,
            Material(ColorAttribute.createDiffuse(Color.ORANGE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )


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



        //Gdx.graphics.setContinuousRendering(false);

        filepop()
        newFrame()
    }


    override fun render() {
        camController!!.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        //modelBatch!!.begin(cam)
        //pink!!.bind()
        //modelBatch!!.render(getNewCoord(), environment)
        //modelBatch!!.end()

        decals.forEach {
            batch!!.add(it)
        }

        batch!!.flush()


        string!!.setLength(0)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        label!!.setText(string)
        stage!!.draw()

    }


    fun getNewCoord(): ArrayList<ModelInstance>{
        var result= ArrayList<ModelInstance>()
        val aux = frames!!.poll()
        if(frames!!.isEmpty()){
            result.add(ModelInstance(proxi,0f,0f,0f))
            return result

        }
        aux.coords.forEach { f ->
            val model = ModelInstance(
            proxi,
            1f * f.x,
            1f * f.y,
            1f * f.z
            )
            result.add(model)
        }
//        println("new frame loaded")
        return  result
    }

    fun newFrame() {
        timer("Array Creator", period = 100,initialDelay = 100) {
            if (frames!!.isNotEmpty()) {
                val f = frames!!.poll()
                decals = f.coords.map {
                    var perc = (it.z - f.minZ) / (f.maxZ - f.minZ)
                    if (perc < 0) {
                        perc = 0f
                    } else if (perc > 1) {
                        perc = 1f
                    }
                    val index = (perc * 255).toInt()
                    //val d = Decal.newDecal(0.05f, 0.05f, blueYellowFade.get(index))
                    val d = Decal.newDecal(0.08f, 0.08f, blueYellowFade[index])
                    d.setPosition(it.x, it.y, it.z)
                    d.lookAt(cam!!.position, cam!!.up)
                    d
                }
            }

            Gdx.graphics.requestRendering();
//            render()
//            println("render requested")
            }
    }



    fun filepop() {
        timer("Array Creator", period = 1000,initialDelay = 0) {

            //val ldrrdr = LidarReader()
            //var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + 12)
            if (frames!!.size < 20) {
                val intermetidate = database.getFrames(1, framesIndex, 12)
                framesIndex += 12
                intermetidate.forEach { f ->
                    frames!!.add(f)
                }
            }
        }
//        println("New batch loaded")
    }


    fun changeArray(x: ArrayList<ModelInstance>) {
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



