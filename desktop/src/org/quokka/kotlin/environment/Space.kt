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
import org.quokka.game.desktop.GameInitializer
import org.quokka.kotlin.config.MAX_LIDAR_FPS
import org.quokka.kotlin.environment.Compression
import org.quokka.kotlin.environment.GuiButtons
import org.quokka.kotlin.environment.drawBar
import org.quokka.kotlin.internals.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.math.*


class Space(val recordingId: Int = 1, val local: Boolean = false, val filepath: String = "core/assets/sample.bag", val axis: Boolean = false) : Screen {
    lateinit var plexer: InputMultiplexer
    val newLidarFps = AtomicBoolean(false)
    /*
     * The frame fetching loop runs at a constant 20fps. These two numbers just determine how many of these frames
     * have to be skipped to achieve the desired framerate.
     * For example 20fps means 0 frames are skipped. 10fps however mean 1 frame is skipped and 5 fps means 3 frames
     * are skipped.
     */
    val framesToSkip = AtomicInteger(0)
    val frameFetchSkipCounter = AtomicInteger(0)
    val lastFpsValue = AtomicInteger(0)

    var fixedCamAzimuth = 0f
    var fixedCamAngle = Math.PI.toFloat() * 0.3f
    var fixedCamDistance = 70f
    val FIXED_CAM_RADIUS_MAX = 100f
    val FIXED_CAM_RADIUS_MIN = 5f
    val FIXED_CAM_ANGLE_MIN = 0f
    val FIXED_CAM_ANGLE_MAX = Math.PI.toFloat() * 0.49f

    val prefs = Gdx.app.getPreferences("My Preferences")

    //-------__Preferancess__---------
    var lidarFPS = prefs.getInteger("LIDAR FPS") //lidar fps 5/10/20
    var lidarFPStimer = 10
    var playbackFPS = 0 // manually fix fps
    /*
     * TODO this should be implemented in the buffer class, right now it is a static 40 seconds.
     *  Just replace the 40 seconds constant in the companion object with a getter from the preferences.
     */
    var memory = 0
    var compresion = prefs.getInteger("COMPRESSION") //compression level
    var gradualCompression = prefs.getBoolean("GRADUAL COMPRESSION")

    //camera setting, if the camera is closer the compression will decrease
    var fixedCamera = prefs.getBoolean("FIXED CAMERA")
    var resolution = Pair(Gdx.graphics.width, Gdx.graphics.height)

    /**
     * dfcm distance from camera margin
     * used in deciding how compressed the data is
     * based on the point's distance from the camera
     */
    var dfcm = prefs.getInteger("DISTANCE")


    val settings = GameInitializer.settings

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

    //compression class initialization
    var cmpss = Compression(
            prefs.getInteger("COMPRESSION"),
            prefs.getBoolean("GRADUAL COMPRESSION"),
            prefs.getInteger("DISTANCE"),
            this.cam
    )

    // List of timers which run in the background, these have to be discarded once the screen is over.
    val timers = mutableListOf<Timer>()

    init {
        println("end of initializing space")
    }

    lateinit var bar : drawBar

    fun create() {
        GuiButtons(this)
        settings.updateSpace()
        bar = drawBar(this.stage, buffer)


        //-----------Camera Creation------------------
        cam.position[0f, 0f] = 30f
        cam.lookAt(0f, 0f, 0f)
        cam.near = .01f
        cam.far = 1000f
        cam.update()



        //---------Environment Creation --------
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))


        //---------Model Population----------

        pix.setColor(66f / 255, 135f / 255, 245f / 255, 1f)
        pix.drawPixel(0, 0)


        // -----------Bottom Text--------
        stage.addActor(label)


        if (local) {
            localFrames = ConcurrentLinkedQueue()
            timers.add(initLocalFileThread())
        }

        timers.add(initFrameUpdateThread())

        // -----------Bottom Text--------
        stage.addActor(label)

        plexer = InputMultiplexer(stage, camController)
        Gdx.input.inputProcessor = plexer
    }

    override fun hide() {
        this.dispose()
//        TODO("Not yet implemented")
    }

    override fun show() {
        create()
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)

        campButtonpress()
        bar.update()
        //if the camera is fixed that means it's always looking at the center of the environment
        if (fixedCamera == true) {
            updateFixedCamera()
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

        //render the decals
        decalBatch.flush()


        string.setLength(0)
        string.append("fps = ")
                .append(Gdx.graphics.framesPerSecond)
                .append(", paused = ")
                .append(pause)
                .append(", frame_index = ")
                .append(buffer.lastFrameIndex)
                .append(", past_buffer_size  = ")
                .append(buffer.pastBufferSize)
                .append(", future_buffer_size  = ")
                .append(buffer.futureBufferSize)
        label.setText(string)
        stage.act(Gdx.graphics.getDeltaTime())
        stage.draw()
    }

    /**
     * This methods is called every tenth of a seconds
     * to load new data in the environment by changing
     * the global variable decal
     * which is both a List<Decal>
     * @author Robert, Till
     */
    fun initFrameUpdateThread(): Timer {
        return timer("Frame Fetcher", period = 1000 / MAX_LIDAR_FPS.toLong(), initialDelay = 1000 / MAX_LIDAR_FPS.toLong()) {
            // Skip frames according to fps
            if (frameFetchSkipCounter.incrementAndGet() > framesToSkip.get()) {
                frameFetchSkipCounter.set(0)
                if (!pause.get()) {
                    if (cmpss.compressionLevel != 1) {
                        decals = fetchNextFrame()?.let { cmpss.compressPoints(it) } ?: decals
                        decals.forEach { colorDecal(it, blueRedFade) }
                    } else {
                        fetchNextFrame()?.let { f ->
                            decals = f.coords.map {
                                val d = Decal.newDecal(0.15f, 0.15f, cmpss.decalTextureRegion)
                                d.setPosition(it.x, it.y, it.z)
                                colorDecal(d, blueRedFade)
                                d
                            }
                        }
                    }
                }
            }

            val maxframes = buffer.framesPerBuffer
            // + 11 since actor 11 is left_bar
            var frameDivider2: Int = 11;
            if(maxframes != 0){
                frameDivider2  = framesIndex / maxframes * 25 + 11
            }

//            if (frameDivider != frameDivider2){
//                stage.clear()
//                GuiButtons(this@Space, frameDivider2)
//            }
        }
    }

    /**
     * Only used when the local argument is enabled. Parses the provided bag file every 2 seconds for 20 seconds of
     * footage.
     *
     * @author Till
     */
    fun initLocalFileThread(): Timer {
        return timer("File Parser", period = 2000) {
            if (localFrames.size < 60) {
                val frames = LidarReader().readLidarFramesInterval(path = filepath, start = framesIndex, end = framesIndex + 12)
                framesIndex += 12
                println("printtttt $framesIndex")
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

    fun updateFixedCamera() {
        val x = fixedCamDistance * cos(fixedCamAzimuth) * cos(fixedCamAngle)
        val y = -fixedCamDistance * sin(fixedCamAzimuth) * cos(fixedCamAngle)
        val z = fixedCamDistance * sin(fixedCamAngle)
        cam.position.set(x, y, z)
        cam.up.set(Vector3.Z)
        cam.lookAt(Vector3(0f, 0f, 0f))
        cam.update()
    }

    fun zoomFixedCloser(delta: Float) {
        fixedCamDistance -= delta * camSpeed * 10
        if (fixedCamDistance < FIXED_CAM_RADIUS_MIN)
            fixedCamDistance = FIXED_CAM_RADIUS_MIN
        if (fixedCamDistance > FIXED_CAM_RADIUS_MAX)
            fixedCamDistance = FIXED_CAM_RADIUS_MAX
    }

    fun zoomFixedAway(delta: Float) {
        fixedCamDistance += delta * camSpeed * 10
        if (fixedCamDistance < FIXED_CAM_RADIUS_MIN)
            fixedCamDistance = FIXED_CAM_RADIUS_MIN
        if (fixedCamDistance > FIXED_CAM_RADIUS_MAX)
            fixedCamDistance = FIXED_CAM_RADIUS_MAX
    }

    fun rotateFixedLeft(delta: Float) {
        fixedCamAzimuth += delta * camSpeed / 10
        fixedCamAzimuth %= Math.PI.toFloat() * 2
    }

    fun rotateFixedRight(delta: Float) {
        fixedCamAzimuth -= delta * camSpeed / 10
        fixedCamAzimuth %= Math.PI.toFloat() * 2
    }

    fun moveFixedUp(delta: Float) {
        fixedCamAngle += delta * camSpeed / 10

        if (fixedCamAngle > FIXED_CAM_ANGLE_MAX)
            fixedCamAngle = FIXED_CAM_ANGLE_MAX
        if (fixedCamAngle < FIXED_CAM_ANGLE_MIN)
            fixedCamAngle = FIXED_CAM_ANGLE_MIN

        fixedCamAngle %= Math.PI.toFloat() * 2
    }

    fun moveFixedDown(delta: Float) {
        fixedCamAngle -= delta * camSpeed / 10

        if (fixedCamAngle > FIXED_CAM_ANGLE_MAX)
            fixedCamAngle = FIXED_CAM_ANGLE_MAX
        if (fixedCamAngle < FIXED_CAM_ANGLE_MIN)
            fixedCamAngle = FIXED_CAM_ANGLE_MIN

        fixedCamAngle %= Math.PI.toFloat() * 2
    }

    fun resetFixed() {
        fixedCamAngle = 0f
        fixedCamAzimuth = 0f
        fixedCamDistance = 70f
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
        lidarFPS = newLFPS // There was an +2 here, not sure why?
        initializeLidarspeed()
        newLidarFps.set(true)
        framesToSkip.set(MAX_LIDAR_FPS / lidarFPS - 1)
        // Reset the buffer to load new footage based on fps
        // Do not remove the ?. For some reason buffer can be null even though it is initialized as a val at creation.
        if (lidarFPS != lastFpsValue.getAndSet(lidarFPS)) {
            buffer?.let {
                thread {
                    it.skipTo(it.lastFrameIndex)
                }
            }
        }
    }

    fun changePlaybackFPS(newFPS: Int) {
        this.playbackFPS = newFPS
        //TODO !!!!
    }

    fun switchFixedCamera(fixed: Boolean) {
        this.fixedCamera = fixed
    }

    fun skipForward10frames() {
        this.framesIndex += 10
        buffer.skipForward(5f)
    }

    fun skipBackwards10Frames() {
        this.framesIndex -= 10
        buffer.skipBackward(5f)
    }

    fun initializeLidarspeed() {
        when (lidarFPS) {
            7 -> lidarFPStimer = 20
            12 -> lidarFPStimer = 10
            22 -> lidarFPStimer = 5
        }
    }


    //------------------------------------------------

    override fun dispose() {
        modelBatch.dispose()
        // Stop timer threads for frame fetching
        for (t in timers) {
            t.cancel()
        }
//        bottomBlock?.dispose()
    }

    override fun resume() {
        pause.set(false)
    }

    override fun resize(width: Int, height: Int) {
        stage.getViewport()?.update(width, height, true);
    }

    override fun pause() {
        pause.set(true)
    }


    /**
     * @author Robert
     *  here methods for the camera controls can be found
     *  they make use of the camera's up and direction vector
     *  to rotate and move it
     */
    //-------Revised Camera Control Methods-----------------------

    val camSpeed = 20f
    val rotationAngle = 75f


   /*
   This method is used for testing,
     it will be left in in case MindHash wants to use it
   It binds camera movements to keyboard keys for easy testing
    */
    fun campButtonpress() {

        val delta = Gdx.graphics.deltaTime
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveLeft(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveRight(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.I)) {
            moveForward(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.K)) {
            moveBackward(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveUp(delta)
            zoomFixedCloser(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveDown(delta)
            zoomFixedAway(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            rotateUp(delta)
            moveFixedUp(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            rotateDown(delta)
            moveFixedDown(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            rotateLeft(delta)
            rotateFixedLeft(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            rotateRight(delta)
            rotateFixedRight(delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            resetCamera()
            resetFixed()
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

}


