package com.mygdx.game.desktop

import LidarData.Database
import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
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
import java.lang.Error
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.xml.soap.Text
import kotlin.collections.ArrayList
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


class Space : InputAdapter(), ApplicationListener {


    val compressed = false
    val local = false

    //-------GUI controlls-----
    var fixedCamera = true


    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null
    val dfcm = 15//distnace from camera margin

    var modelBatch: ModelBatch? = null


    var spaceObjects: ArrayList<ModelInstance>? = null
    var instance: ModelInstance? = null

    var bottomBlock: Model? = null

    var pink: Texture? = null

    var frames: ConcurrentLinkedQueue<LidarFrame>? = null
    var framesIndex = 2400
    var pause = AtomicBoolean(false)

    var environment: Environment? = null

    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null
    var errMessage = " "

    val database: Database
    var batch: DecalBatch? = null
    var decals: List<Decal> = emptyList()
    var decalShaved: List<Decal> = emptyList()

    lateinit var blueYellowFade: Array<TextureRegion>
    lateinit var blueRedFade: Array<TextureRegion>
    lateinit var decalTextureRegion: TextureRegion

    init {
        database = Database()
        database.connect("lidar", "mindhash")
    }

    override fun create() {

        modelBatch = ModelBatch()
        //-----------Camera Creation------------------
        cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam!!.position[0f, 0f] = 30f
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.near = .01f
        cam!!.far = 1000f
        cam!!.update()

        //---------Camera controls--------
        camController = CameraInputController(cam)
        Gdx.input.inputProcessor = camController
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))



        spaceObjects = ArrayList<ModelInstance>(1)


        frames = ConcurrentLinkedQueue<LidarFrame>()
        //---------Model Population----------
        var modelBuilder = ModelBuilder()


        batch = DecalBatch(CameraGroupStrategy(cam))

        val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
        pix.setColor(66f / 255, 135f / 255, 245f / 255, 1f)
        pix.drawPixel(0, 0)
        decalTextureRegion = TextureRegion(Texture(pix))

        blueRedFade = Array(256) { i ->
            val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
            pix.setColor(i / 255f, 0f, 1 - i / 255f, 1f)
            pix.drawPixel(0, 0)
            TextureRegion(Texture(pix))
        }
        blueYellowFade = Array(256) { i ->
            val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
            pix.setColor(i / 255f, i / 255f, 1 - i / 255f, 1f)
            pix.drawPixel(0, 0)
            TextureRegion(Texture(pix))
        }


        modelBuilder.begin()
        modelBuilder.node().id = "Floor"
        pink = Texture(Gdx.files.internal("core/assets/badlogic.jpg"), false)
        pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        var material = Material(TextureAttribute.createDiffuse(pink))
        modelBuilder.end()
        bottomBlock = modelBuilder.createBox(
                10f, 10f, .5f,
                material,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
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

        filepop()
        newFrame()
    }


    override fun render() {

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) == true) {
            pause.getAndSet(!pause.get())
        }

        camController!!.update()

        if (fixedCamera == true) {
            cam!!.lookAt(0f, 0f, 0f)
        }


        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        if (compressed == false) {
            decals.forEach {
                batch!!.add(it)
            }
        } else {
            decalShaved.forEach {
                batch!!.add(it)
            }
        }
        batch!!.flush()


        string!!.setLength(0)
        string!!.append(errMessage)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        string!!.append(" paused: ").append(pause.get())
        label!!.setText(string)
        stage!!.draw()
        errMessage = ""

    }

    fun newFrame() {
        timer("Array Creator", period = 100, initialDelay = 100) {
            if (pause.get() == false && frames!!.isNotEmpty()) {
                if (compressed == false) {
                    val f = frames!!.poll()
                    decals = f.coords.map {
                        // val d = Decal()
                        val d = Decal.newDecal(0.08f, 0.08f, decalTextureRegion)
                        d.setPosition(it.x, it.y, it.z)
                        d.lookAt(cam!!.position, cam!!.up)
                        colorDecal(d, blueRedFade)
                        d
                    }
                } else {
                    decalShaved = shaveDecal()
                    decalShaved.forEach { d -> colorDecal(d, blueRedFade) }
                    println("Got new decals: ${decalShaved.size}")
                }
            }
        }
    }

    fun colorDecal(d: Decal, textures: Array<TextureRegion>) {
        val minZ = -10
        val maxZ = 15
        var perc = (d.position.z - minZ) / (maxZ - minZ)
        if (perc < 0f)
            perc = 0f
        else if (perc > 1f)
            perc = 1f
        d.textureRegion = textures.get((perc * (textures.size - 1)).toInt())
    }


    fun filepop() {
        timer("Array Creator", period = 1000, initialDelay = 0) {
            val fps = 12
            if (pause.get() == false) {
                if (local == true) {
                    val ldrrdr = LidarReader()
                    var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + fps)
                    framesIndex += fps
                    intermetidate.forEach { f ->
                        frames!!.add(f)
                    }
                } else {
                    if (frames!!.size < 20) {
                        val intermetidate = database.getFrames(1, framesIndex, fps)
                        framesIndex += fps
                        intermetidate.forEach { f ->
                            frames!!.add(f)
                        }

                    }
                }
            }
        }
//        println("New batch loaded")
    }


    fun decideCPR(a: Float, divisions: Int): Float {
        var result = 0f
        var auxxx = 0f
        if (a > -1 && a < 1) {
            auxxx = a
        } else {
            auxxx = a - a.toInt()
        }
        val margin: Float
        if (divisions == 1) {
            return a
        } else if (divisions == 2) {
            margin = .5f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..1f -> result = a.toInt() + margin * sign(a)

                in -1f..margin * -1 -> result = a.toInt() + margin * sign(a)
                in margin * -1..0f -> result = a.toInt() * 1f
            }
        } else if (divisions == 3) {
            margin = .33f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..margin * 2 -> result = a.toInt() + margin * sign(a)
                in margin * 2..1f -> result = a.toInt() + margin * 2 * sign(a)

                in margin * -1..0f -> result = a.toInt() * 1f
                in margin * -2..margin * -1 -> result = a.toInt() + margin * sign(a)
                in -1f..margin * -2 -> result = a.toInt() + margin * 2 * sign(a)
            }

        } else if (divisions == 4) {
            margin = .25f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..margin * 2 -> result = a.toInt() + margin * sign(a)
                in margin * 2..margin * 3 -> result = a.toInt() + margin * 2 * sign(a)
                in margin * 3..1f -> result = a.toInt() + margin * 3 * sign(a)

                in margin * -1..0f -> result = a.toInt() * 1f
                in -1f..margin * -3 -> result = a.toInt() + margin * 3 * sign(a)
                in margin * -2..margin * -1 -> result = a.toInt() + margin * 1 * sign(a)
                in margin * -3..margin * -2 -> result = a.toInt() + margin * 2 * sign(a)
            }
        } else throw Error("Divisions not prepared for that ")
        return result
    }


    /**
     * This methods decied the vel of compression of a point
     * depending on the distance from the camera
     * @param coord is the coordinate being checked
     * @return 1,2,3,4 number of divisions,
     * will be fed into decideCPR
     */
    fun decidDivisions(coord: LidarCoord): Int {
        val camp = cam?.position
        if (camp != null) {
            val distance =
                    sqrt((coord.x - camp.x).pow(2)
                            + (coord.y - camp.y).pow(2)
                            + (coord.z - camp.z).pow(2))

            val substraction = distance - dfcm
            if (substraction < 0) {
                return 1
            } else if (substraction < dfcm) {
                return 2
            } else if (substraction < 2 * dfcm) {
                return 3
            } else {
                return 4
            }

        } else throw Error("Could not find camera position in decidDivisions")
    }


    fun changeArray(x: ArrayList<ModelInstance>) {
        this.spaceObjects = x
    }


    override fun dispose() {
        modelBatch!!.dispose()
        bottomBlock!!.dispose()
    }

    override fun resume() {}

    override fun resize(width: Int, height: Int) {
        stage?.getViewport()?.update(width, height, true);
    }

    override fun pause() {}


    fun shaveDecal(): ArrayList<Decal> {
        var objects = ArrayList<Decal>(15)
        var map = HashMap<Triple<Float, Float, Float>, Int>()

        if (frames!!.isEmpty()) {
            val d = Decal.newDecal(0.5f, 0.5f, decalTextureRegion)
            d.setPosition(0f, 0f, 0f)
            d.lookAt(cam!!.position, cam!!.up)
            println("empty frame")
            objects.add(d)
            return objects
        }


        var crtFrame = frames!!.poll()
        crtFrame.coords.forEach { c ->

            val divisions = decidDivisions(c)

            val tripp = Triple(
                    decideCPR(c.x, divisions),
                    decideCPR(c.y, divisions),
                    decideCPR(c.z, divisions))

            if (map.keys.contains(tripp)) {
                map.set(tripp, map.getValue(tripp) + 1)
            } else {
                map.set(tripp, 1)
            }
        }


        val margin = 5
        map.keys.forEach { k ->
            var d = Decal.newDecal(.4f, .4f, decalTextureRegion)
            var perc = (k.third - crtFrame.minZ) / (crtFrame.maxZ - crtFrame.minZ)
            if (perc < 0) {
                perc = 0f
            } else if (perc > 1) {
                perc = 1f
            }
            val index = (perc * 255).toInt()
            if (map.get(k) in 1..margin) {
                d = Decal.newDecal(0.1f, 0.1f, blueYellowFade[index])

            } else if (map.get(k) in 1 * margin..2 * margin) {
                d = Decal.newDecal(0.15f, 0.15f, blueYellowFade[index])

            } else if (map.get(k) in 3 * margin..4 * margin) {
                d = Decal.newDecal(0.2f, 0.2f, blueYellowFade[index])

            } else if (map.get(k) in 4 * margin..5 * margin) {
                d = Decal.newDecal(0.25f, 0.25f, blueYellowFade[index])

            } else if (map.get(k) in 5 * margin..6 * margin) {
                d = Decal.newDecal(0.3f, 0.3f, blueYellowFade[index])

            } else if (map.get(k) in 6 * margin..7 * margin) {
                d = Decal.newDecal(0.35f, 0.35f, blueYellowFade[index])
            }
            d.setPosition(k.first, k.second, k.third)
            d.lookAt(cam!!.position, cam!!.up)
            objects.add(d)
        }

        return objects
    }
}





