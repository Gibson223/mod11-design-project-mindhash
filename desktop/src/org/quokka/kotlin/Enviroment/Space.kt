package com.mygdx.game.desktop

import LidarData.Database
import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.Texture
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
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import net.java.games.input.Component
import org.quokka.kotlin.Enviroment.UIobserver
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


//class Space : InputAdapter(), ApplicationListener, Observer {
class Space : Screen, InputAdapter(), Observer {
    val compressed = true
    val local = true

    var running = AtomicBoolean(true)
    var pause = AtomicBoolean(false)

    //-------GUI controlls-----
    var fixedCamera = true


    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null

    /**
     * dfcm distance from camera margin
     * used in deciding how compressed the data is
     * based on the point's distance from the camera
     */
    val dfcm = 15

    var modelBatch: ModelBatch? = null


    var spaceObjects: ArrayList<ModelInstance>? = null
    var instance: ModelInstance? = null

    var bottomBlock: Model? = null

    var pink: Texture? = null

    var frames: ConcurrentLinkedQueue<LidarFrame>? = null
    var framesIndex = 2400

    var environment: Environment? = null

    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null
    var errMessage = " "

    val database: Database
    var decalBatch: DecalBatch? = null

    var decals: List<Decal> = listOf()
    var compressedDecals: List<Decal> = listOf()

    lateinit var blueYellowFade: Array<TextureRegion>
    lateinit var blueRedFade: Array<TextureRegion>
    lateinit var decalTextureRegion: TextureRegion


    init {
        database = Database()
        database.connect("lidar", "mindhash")
    }

    fun create() {
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


        decalBatch = DecalBatch(CameraGroupStrategy(cam))

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
        string = StringBuilder()

        plexer = InputMultiplexer(this as InputProcessor, camController) // TODO: ask what this is supposed to do??

        filepop()
        newFrame()
    }

    override fun hide() {
        TODO("Not yet implemented")
    }

    override fun show() {
        Gdx.input.inputProcessor = this;
        create()
    }


    override fun render(delta: Float) {
        camController!!.update()

        if (fixedCamera == true) {
            cam!!.lookAt(0f, 0f, 0f)
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        if (compressed == false) {
            decals.forEach {
                decalBatch!!.add(it)
            }
        } else {
            compressedDecals.forEach {
                decalBatch!!.add(it)
            }
        }
        decalBatch!!.flush()


        string!!.setLength(0)
        string!!.append(errMessage)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        string!!.append(" paused: ").append(pause.get())
        label!!.setText(string)
        stage!!.act(Gdx.graphics.getDeltaTime())
        stage!!.draw()
        errMessage = ""

    }

    /**
     * this methods is called every tenth of a seconds
     * to load new data in the environment
     */
    fun newFrame() {
        timer("Array Creator", period = 100, initialDelay = 100) {
            if (pause.get() == false && frames!!.isNotEmpty()) {
                if (compressed == false) {
                    val f = frames!!.poll()
                    decals = f.coords.map {
                        val d = Decal.newDecal(0.08f, 0.08f, decalTextureRegion)
                        d.setPosition(it.x, it.y, it.z)
                        d.lookAt(cam!!.position, cam!!.up)
                        colorDecal(d, blueRedFade)
                        d
                    }
                } else {
                    compressedDecals = compressPoints()
                    compressedDecals.forEach { d -> colorDecal(d, blueRedFade) }
                }
            }
        }
    }

    /**
     * Helper function which changes the texture of a decal based on its Z coordinate.
     * The percentage of its z coordinate on a relative scale is calculated and then the
     * appropriate texture region is chosen.
     * The effect works best if the array of textures is a color gradient.
     *
     * @param d The decal to be recolored.
     * @param textures An array of texture regions to pick from. Must be non empty.
     */
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


    /**
     * this methods returns the parent of a point
     * the parent of a point is a point to which the initial point is aproximated
     * @param a is the number being tested
     * @param divisions is the number of divisions meaning
     * if it is 1 then then the number is aproximated to itself
     * if it is 2 then then number is approximated to closes .5 or .0
     */
    fun returnCPP(a: Float, divisions: Int): Float {
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
     * will be fed into returnCPP
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
                return 4
            } else if (substraction < 2 * dfcm) {
                return 3
            } else {
                return 2
            }

        } else throw Error("Could not find camera position in decidDivisions")
    }


    override fun dispose() {
        modelBatch!!.dispose()
        bottomBlock!!.dispose()
    }

    override fun resume() {}

    override fun resize(width: Int, height: Int) {
        stage?.getViewport()?.update(width, height, true);
    }

    override fun pause() {
        pause.set(!pause.get())
    }

    fun getRunning():Boolean {
        return running.get()
    }


    /**
     * point means a point in the point cloud, an object with x y z float values
     * this methods looks at the next frame in line
     * and puts points which are close enough to each other in one point
     * then gives the remaining points a suitably sized decal
     * based on the amount of points which are compressed into that point
     */
    fun compressPoints(): ArrayList<Decal> {
        var objects = ArrayList<Decal>(15) //end result of the method

        var map = HashMap<LidarCoord, Int>()
                //map containing the coordinates as key and the number of points approximated to that point as value

        //if the frame is empty (which should never be) add a dummy decal
        if (frames!!.isEmpty()) {
            val d = Decal.newDecal(0.5f, 0.5f, decalTextureRegion)
            d.setPosition(0f, 0f, 0f)
            d.lookAt(cam!!.position, cam!!.up)
            println("empty frame")
            objects.add(d)
            return objects
        }

        var crtFrame = frames!!.poll()//get next frame

        crtFrame.coords.forEach { c ->
            val divisions = decidDivisions(c)
                //calculate the compression power(1/2/3/4) based on the distance from the camera

            //dummy value which contains the point to which the currently analyzed point is approximated to
            // it is the point itself if the camera is close enough
            val tripp = LidarCoord(
                    returnCPP(c.x, divisions),
                    returnCPP(c.y, divisions),
                    returnCPP(c.z, divisions))

            //if the point has not been added before initialize it with one
            //otherwise update its value in the map
            if (map.keys.contains(tripp)) {
                map.set(tripp, map.getValue(tripp) + 1)
            } else {
                map.set(tripp, 1)
            }
        }

        //each point after the compression will represent one or more points
        // based on how many points it represent, the size of the Decal for that point
        // the margin is a number. it represents the step from one size to another
        val margin = 5
        map.keys.forEach { k ->

            var d = Decal.newDecal(.3f, .3f, decalTextureRegion)

            if (map.get(k) in 1..margin) {
                d.setDimensions(0.1f,0.1f)

            } else if (map.get(k) in 1 * margin..2 * margin) {
                d.setDimensions(0.2f,0.2f)

            } else if (map.get(k) in 3 * margin..4 * margin) {
                d.setDimensions(0.2f,0.2f)

            } else if (map.get(k) in 4 * margin..5 * margin) {
                d.setDimensions(0.25f,0.25f)

            } else if (map.get(k) in 5 * margin..6 * margin) {
                d.setDimensions(0.25f,0.25f)

            } else if (map.get(k) in 6 * margin..100) {
                d.setDimensions(0.3f,0.3f)
            }
            d.setPosition(k.x, k.y, k.z)
            d.lookAt(cam!!.position, cam!!.up)
            objects.add(d)
        }

        return objects
    }

    override fun update(o: Observable?, arg: Any?) {
        println("called upadte")
       if(o is UIobserver){
           if (arg == Component.Identifier.Key.SPACE){
               pause()
           }
       }
    }
}





