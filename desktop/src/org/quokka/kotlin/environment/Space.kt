package com.mygdx.game.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import org.quokka.kotlin.environment.GuiButtons
import org.quokka.kotlin.environment.Settings
import org.quokka.kotlin.internals.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


class Space(val recordingId: Int = 1, val compressed: Boolean = false, val local: Boolean = false, val filepath: String = "core/assets/sample.bag", val axis: Boolean = false) : Screen {
    lateinit var plexer: InputMultiplexer
    var newLidaarFPS = AtomicBoolean(false)



    val prefs = Gdx.app.getPreferences("My Preferences")
    //-------__Preferancess__---------
    var lidarFPS = prefs.getInteger("LIDAR FPS") //lidar fps 5/10/20
    var lidarFPStimer = 10
    var playbackFPS = 0 // manually fix fps
    var memory = 0 // we're not sure yet how this will work
    var compresion = prefs.getInteger("COMPRESSION") //compression level
    var gradualCompression = prefs.getBoolean("GRADUAL COMPRESSION")
    //camera setting, if the camera is closer the compression will decrease
    var fixedCamera = prefs.getBoolean("FIXED CAMERA")
    var resolution  = Pair(Gdx.graphics.width,Gdx.graphics.height)

    /**
     * dfcm distance from camera margin
     * used in deciding how compressed the data is
     * based on the point's distance from the camera
     */
    var dfcm = 15//prefs.getInteger("DISTANCE")

    val settings = Settings(this)

    init {
        settings.updateSpace()
    }


    var pause = AtomicBoolean(false)
    val buffer = Buffer(recordingId)
    // this is basically the timestamp
    var framesIndex = Database.getRecording(recordingId)!!.minFrame


    var cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    var camController = CameraInputController(cam)


    var modelBatch = ModelBatch()


    var environment = Environment()


    var stage: Stage = Stage()
    var font: BitmapFont = BitmapFont()
    var label = Label(" ", LabelStyle(font, Color.WHITE))
    var string: StringBuilder = StringBuilder()
    var errMessage = " "

    var decalBatch = DecalBatch(CameraGroupStrategy(cam))

    val pix = Pixmap(1, 1, Pixmap.Format.RGB888)


    lateinit var localFrames: ConcurrentLinkedQueue<LidarFrame>


    var decals: List<Decal> = listOf()
    val axisDecals: ArrayList<Decal> = ArrayList(30)

    val blueRedFade = Array(256) { i ->
        val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
        pix.setColor(i / 255f, 0f, 1 - i / 255f, 1f)
        pix.drawPixel(0, 0)
        TextureRegion(Texture(pix))
    }
    var decalTextureRegion = TextureRegion(Texture(pix))


    fun create() {
        GuiButtons(this)
        //-----------Camera Creation------------------
        cam.position[0f, 0f] = 30f
        cam.lookAt(0f, 0f, 0f)
        cam.near = .01f
        cam.far = 1000f
        cam.update()

        //---------Camera controls--------
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))


        //---------Model Population----------

        pix.setColor(66f / 255, 135f / 255, 245f / 255, 1f)
        pix.drawPixel(0, 0)

        for (i in -50..50) {
            val dx = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dx.setPosition(i * -1f, -1f, -1f)
            dx.lookAt(cam!!.position, cam!!.up)
            val dy = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dy.setPosition(-1f, -1f * i, -1f)
            dy.lookAt(cam!!.position, cam!!.up)
            val dz = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dz.setPosition(-1f, -1f, i * -1f)
            dz.lookAt(cam!!.position, cam!!.up)
            axisDecals.add(dx)
            axisDecals.add(dy)
            axisDecals.add(dz)
        }


        // -----------Bottom Text--------
        stage.addActor(label)


        if (local) {
            localFrames = ConcurrentLinkedQueue()
            initLocalFileThread()
        }

        initFrameUpdateThread()
        // -----------Bottom Text--------
        stage.addActor(label)

        plexer = InputMultiplexer(stage, camController)
        Gdx.input.inputProcessor = plexer
    }

    override fun hide() {
//        TODO("Not yet implemented")
    }

    override fun show() {
        create()
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f,0f,0f, 1f)

        campButtonpress()
        //if the camera is fixed that means it's always looking at the center of the environment
        if (fixedCamera == true) {
            cam.lookAt(0f, 0f, 0f)
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        decals.forEach { d ->
            if (cam.frustum.boundsInFrustum(d.x, d.y, d.z, .3f, .3f, .3f) == true) {
                decalBatch.add(d)
                d.lookAt(cam.position, cam.up)
            }
        }

        if (axis == true) {
            axisDecals.forEach { d ->
                if (cam.frustum.boundsInFrustum(d.x, d.y, d.z, .3f, .3f, .3f) == true) {
                    decalBatch.add(d)
                }
            }
        }

        decalBatch.flush()


        string.setLength(0)
        string.append(errMessage)
        string.append(" Fps : ").append(Gdx.graphics.framesPerSecond)
        string.append(" cma position : ").append(cam.position)
        string.append(" cam pos origitn : ").append(cam.up)
        label.setText(string)
        stage.act(Gdx.graphics.getDeltaTime())
        stage.draw()
        errMessage = ""
    }

    /**
     * This methods is called every tenth of a seconds
     * to load new data in the environment by changing
     * the global variable decal
     * which is both a List<Decal>
     * @author Robert, Till
     */
    fun initFrameUpdateThread() {
        timer("Array Creator", period = 100, initialDelay = 100) {
            if (!pause.get()) {
                if (compressed) {
                    decals = compressPoints() ?: decals
                    decals.forEach { colorDecal(it, blueRedFade) }
                } else {
                    val nextFrame = fetchNextFrame()
                    nextFrame?.let { f ->
                        decals = f.coords.map {
                            val d = Decal.newDecal(0.15f, 0.15f, decalTextureRegion)
                            d.setPosition(it.x, it.y, it.z)
                            colorDecal(d, blueRedFade)
                            d
                        }
                    }
                }
            }
        }
    }

    /**
     * Only used when the local argument is enabled. Parses the provided bag file every 2 seconds for 20 seconds of
     * footage.
     *
     * @author Till
     */
    fun initLocalFileThread() {
        timer("File Parser", period = 2000) {
            if (localFrames.size < 60) {
                val frames = LidarReader().readLidarFramesInterval(path = filepath, start = framesIndex, end = framesIndex + 20)
                framesIndex += 20
                localFrames.addAll(frames)
            }
        }
    }

    /**
     * Fetches a new frame from either local or database source.
     *
     * @return Null or a lidar frame
     */
    fun fetchNextFrame(): LidarFrame? {
        if (local) {
            return localFrames.poll()
        } else {
            return buffer.nextFrame()
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


    //--------Buttons methods-------------

    fun changeResolution(height: Int, width: Int) {
        Gdx.graphics.setWindowedMode(width, height);
    }

    fun changeLidarFPS(newLFPS: Int) {
        this.lidarFPS = newLFPS + 2
        when(lidarFPS){
            7 -> lidarFPStimer = 20
            12 -> lidarFPStimer = 10
            22 -> lidarFPStimer = 5
        }
        this.newLidaarFPS.set(true)
    }

    fun changePlaybackFPS(newFPS: Int) {
        this.playbackFPS = newFPS
    }

    fun switchFixedCamera(fixed: Boolean) {
        this.fixedCamera = fixed
    }

    fun changeCompression(newcomp:Int){
        this.compresion = newcomp
    }

    fun switchGradualCompression(newset: Boolean){
        this.gradualCompression = newset
    }

    fun skipForward10frames(){
        this.framesIndex += 10
    }

    fun skipBackwards10Frames(){
        this.framesIndex -= 10
    }

    fun initializeLidarspeed(){
        when(lidarFPS){
            7 -> lidarFPStimer = 20
            12 -> lidarFPStimer = 10
            22 -> lidarFPStimer = 5
        }
    }

    fun changeDFCM(dd:Int){
        this.dfcm = dd
    }

    //------------------------------------------------

    /**
     * this methods returns the parent of a point
     * the parent of a point is a point to which the initial point is aproximated
     * @param a is the number being tested
     * @param divisions is the number of divisions meaning
     * if it is 1 then then the number is aproximated to itself
     * if it is 2 then then number is approximated to closes .5 or .0
     * @author Robert
     *
     * @return The parent of a point.
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
        } else throw Error("Divisions not prepared for divisions to be $divisions  ")
        return result
    }


    fun distanceAB3(a: LidarCoord, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceAB3(a: Vector3, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun disntaceAB2(x: Float, y: Float, a: Float, b: Float): Float {
        return sqrt((x - a).pow(2) + (y - b).pow(2))
    }

    /**
     * This methods decied the val of compression of a point
     * depending on the distance from the camera
     * @param coord is the coordinate being checked
     * @return 1,2,3,4 number of divisions,
     * will be fed into returnCPP
     * @author Robert
     */
    fun decidDivisions(coord: LidarCoord): Int {
        val camp = cam?.position
        if (camp != null) {
            val distance = distanceAB3(coord, camp)
            sqrt((coord.x - camp.x).pow(2)
                    + (coord.y - camp.y).pow(2)
                    + (coord.z - camp.z).pow(2))

            val dfcmCopy = dfcm
            val substraction = distance - dfcmCopy

            when (compresion) { //compresion is the maximum level of compression
                // 1 is least, then 4, 3 and finally 2
                1 -> return 1
                2 -> if (substraction < 0) {
                    return 1
                } else if (substraction < dfcmCopy) {
                    return 4
                } else if (substraction < 2 * dfcmCopy) {
                    return 3
                } else {
                    return 2
                }
                3 -> if (substraction < 0) {
                    return 1
                } else if (substraction < dfcmCopy) {
                    return 4
                } else {
                    return 3
                }
                4 -> if (substraction < 0) {
                    return 1
                } else {
                    return 4
                }
            }

        } else throw Error("Could not find camera position in decidDivisions")
        return -1
    }


    override fun dispose() {
        modelBatch.dispose()
//        bottomBlock?.dispose()
    }

    override fun resume() {
        pause.set(false)
    }

    override fun resize(width: Int, height: Int) {
        stage?.getViewport()?.update(width, height, true);
    }

    override fun pause() {
        pause.set(true)
    }



    /**
     * point means a point in the point cloud, an object with x y z float values
     * this methods looks at the next frame in line
     * and puts points which are close enough to each other in one point
     * then gives the remaining points a suitably sized decal
     * based on the amount of points which are compressed into that point
     * @author Robert
     */
    fun compressPoints(): ArrayList<Decal>? {
        var objects = ArrayList<Decal>(15) //end result of the method

        var map = HashMap<LidarCoord, Int>()
        //map containing the coordinates as key and the number of points approximated to that point as value

        var crtFrame = fetchNextFrame()

        // Return null if no new frame is available
        if (crtFrame == null) {
            return null
        }

        crtFrame.coords.forEach { c ->
            var divisions = compresion //level of compression
            if (gradualCompression == true && compresion != 1) {
                divisions = decidDivisions(c) //has to be deiced based on distance from camera
            }
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
            val baseSizeofDecal = .2f

            for (i in 0..8) {
                if (map.get(k) in i * margin..(i + 1) * margin) {
                    d.setDimensions(baseSizeofDecal + i * 0.02f, baseSizeofDecal + i * 0.02f)
                }
            }
            d.setPosition(k.x, k.y, k.z)
            objects.add(d)
        }
        return objects
    }


    /**
     * calculates the distance betwwen two points
     * the points can be represented as
     * either Vector3 or LidarCoord
     * @author Robet
     */
    fun distanceBetween2points(a: LidarCoord, b: LidarCoord): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceBetween2points(a: Vector3, b: LidarCoord): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceBetween2points(a: LidarCoord, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceBetween2points(a: Vector3, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    /**
     * @author Robert
     */
    //-------Camera Control Methods-----------------------


    val camSpeed = 20f
    val rotationAngle = 75f


    // this methods can be deleted later
    fun campButtonpress() {

        val delta = Gdx.graphics.deltaTime
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveLeft(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveRight(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.I)) {
            moveForward(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.K)) {
            moveBackward(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveUp(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveDown(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            rotateUp(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            rotateDown(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            rotateLeft(delta)
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            rotateRight(delta)
//        } else if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
//            rotateZ()
//        } else if (Gdx.input.isKeyPressed(Input.Keys.C)) {
//            rotateZrev()
        } else if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            resetCamera()
        }
    }

    fun resetCamera() {
        cam.position[0f, 0f] = 30f
        cam.lookAt(0f, 0f, 0f)
        cam.up.set(0f, 1f, 0f)
        cam.update()
    }

    fun moveForward(delta: Float) {
        cam.translate(Vector3(cam.direction).scl(delta * camSpeed))
        cam.update()
    }

    fun moveBackward(delta: Float) {
        cam.translate(Vector3(cam.direction).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveUp(delta: Float) {
        cam.translate(Vector3(cam.up).scl(delta * camSpeed))
        cam.update()
    }

    fun moveDown(delta: Float) {
        cam.translate(Vector3(cam.up).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveLeft(delta: Float) {
        cam.translate(Vector3(cam.up).rotate(cam.direction, 90f).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveRight(delta: Float) {
        cam.translate(Vector3(cam.up).rotate(cam.direction, 90f).scl(delta * camSpeed))
        cam.update()
    }

    fun rotateUp(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction, 90f), delta * rotationAngle)
        cam.update()
    }

    fun rotateDown(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction, 90f), -delta * rotationAngle)
        cam.update()
    }

    fun rotateLeft(delta: Float) {
        cam.rotate(cam.up, delta * rotationAngle)
        cam.update()
    }

    fun rotateRight(delta: Float) {
        cam.rotate(cam.up, -delta * rotationAngle)
        cam.update()
    }

    fun rotateZ() {
        cam.rotate(Vector3(0f, 0f, 1f), rotationAngle)
        cam.update()
    }

    fun rotateZrev() {
        cam.rotate(Vector3(0f, 0f, 1f), -rotationAngle)
        cam.update()
    }
}


