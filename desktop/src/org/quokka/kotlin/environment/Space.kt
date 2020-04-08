package org.quokka.kotlin.environment

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScalingViewport
import org.quokka.Screens.IndexScreen
import org.quokka.game.desktop.GameInitializer
import org.quokka.kotlin.config.MAX_LIDAR_FPS
import org.quokka.kotlin.internals.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.math.cos
import kotlin.math.sin


class Space(val recordingId: Int = 1, val local: Boolean = false, val filepath: String = "core/assets/sample.bag") : Screen {

    companion object {
        const val FIXED_CAM_RADIUS_MAX = 100f
        const val FIXED_CAM_RADIUS_MIN = 5f
        const val FIXED_CAM_ANGLE_MIN = 0f
        const val FIXED_CAM_ANGLE_MAX = Math.PI.toFloat() * 0.49f
        const val ZOOM_STEP_SIZE = 10f
        const val CAM_SPEED = 0.03f
        const val AUTOMATIC_CAMERA_SPEED_MODIFIER = 3f
        const val ROTATION_ANGLE_MODIFIER = 1f
    }

    private lateinit var plexer: InputMultiplexer
    private val newLidarFps = AtomicBoolean(false)
    /*
     * The frame fetching loop runs at a constant 20fps. These two numbers just determine how many of these frames
     * have to be skipped to achieve the desired framerate.
     * For example 20fps means 0 frames are skipped. 10fps however mean 1 frame is skipped and 5 fps means 3 frames
     * are skipped.
     */
    private val framesToSkip = AtomicInteger(0)
    private val frameFetchSkipCounter = AtomicInteger(0)
    private val lastFpsValue = AtomicInteger(0)

    private val prefs = Gdx.app.getPreferences("My Preferences")

    //-------  Perferences  -------
    private var lidarFPS = prefs.getInteger("LIDAR FPS") //lidar fps 5/10/20

    //-------  Camera  -------
    private var fixedCamera = prefs.getBoolean("FIXED CAMERA")
    private var automaticCamera = prefs.getBoolean("AUTOMATIC CAMERA")
    private var gpsEnv = AtomicBoolean(prefs.getBoolean("GPS ENVIRONMENT",false))
    private var fixedCamAzimuth = 0f
    private var fixedCamAngle = Math.PI.toFloat() * 0.3f
    private var fixedCamDistance = 70f


    val settings = GameInitializer.settings
    var pause = AtomicBoolean(false)
    val buffer: Buffer = PrerecordedBuffer(recordingId)

    // this is basically the timestamp
    private var framesIndex = Database.getRecording(recordingId)!!.minFrame


    var cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

//    private var camController = CameraInputController(cam)

    private var environment = Environment()

//    var stage: Stage = Stage()
    var stage: Stage = Stage(ScalingViewport(Scaling.stretch, 1280f, 720f))
//    var stage: Stage = Stage(FitViewport( Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()))
    private var font: BitmapFont = BitmapFont()
    private var label = Label(" ", LabelStyle(font, Color.WHITE))
    private var string: StringBuilder = StringBuilder()

    private var decalBatch = DecalBatch(CameraGroupStrategy(cam))


    private val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
    var decalTextureRegion = TextureRegion(Texture(pix))


    private lateinit var localFrames: ConcurrentLinkedQueue<LidarFrame>


    private var decals: List<Decal> = listOf()

    private val blueRedFade = Array(256) { i ->
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
            this
    )

    // List of timers which run in the background, these have to be discarded once the screen is over.
    private val timers = mutableListOf<Timer>()

    lateinit var modelBatch: ModelBatch
    lateinit var rendableObjects: ArrayList<ModelInstance>
    lateinit var instance : ModelInstance
    lateinit var globe :ModelInstance
    lateinit var pink :Texture

    init {
        println("end of initializing space")
    }

    lateinit var gui: GuiButtons

    private fun create() {
        gui = GuiButtons(this)
        settings.updateSpace()


        //-----------Camera Creation------------------
        cam.position[0f, 0f ] = 30f
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

        stage.addActor(label)

        plexer = InputMultiplexer(stage)
        Gdx.input.inputProcessor = plexer


        //--------Earth and City-------
        modelBatch = ModelBatch()

        val load = ObjLoader()
        val model = load.loadModel(Gdx.files.internal("ma_place.obj"));
        instance = ModelInstance(model)
        instance.transform.rotate(Vector3.X,90f)


        pink = Texture(Gdx.files.internal("yeet.jpeg"),false)

        rendableObjects = ArrayList(2)
        if(gpsEnv.get()) {
            rendableObjects.add(instance)
        }
    }


    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)

        campButtonpress()
        gui.bar.update()
        // if the camera is fixed that means it's always looking at the center of the environment
        // This is also triggers if the automatic camera is chosen
        if (fixedCamera || automaticCamera) {
            if (automaticCamera) {
                moveAutomaticCamera(delta)
            }
            updateFixedCamera()
        }

//        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        decals.forEach { d ->
            if (cam.frustum.boundsInFrustum(d.x, d.y, d.z, .3f, .3f, .3f) == true) {
                decalBatch.add(d)
                d.lookAt(cam.position, cam.up)
            }
        }


        //render the decals
        decalBatch.flush()
        val material = Material(TextureAttribute.createDiffuse(pink))
        val modelBuilder = ModelBuilder()



        val vec = globeUpdate()

//        var vec = cam.direction.mul(cam.position)

        globe = ModelInstance(modelBuilder.createSphere(1f,1f,1f,15,15,
                material,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()),
                vec.x,vec.y,vec.z)
        globe.transform.rotate(Vector3.X,90F)
        //render city and earth
        rendableObjects.add(globe)

        modelBatch.begin(cam);
        modelBatch.render(rendableObjects, environment);
        modelBatch.end();
        rendableObjects.remove(globe)


        // Lable situated at the bottom of the screen
        // useful tool for debugging
        // just add strings to the string object to be displayed
        string.setLength(0)
        string.append("fps = ")
                .append(Gdx.graphics.framesPerSecond)

//                .append(", paused = ")
//                .append(pause)
//                .append(", frame_index = ")
//                .append(buffer.lastFrameIndex)
//                .append(", past_buffer_size  = ")
//                .append(buffer.pastBufferSize)
//                .append(", future_buffer_size  = ")
//                .append(buffer.futureBufferSize)
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

    /**
     * this methods updates the LidarFPS
     * LidarFPS being the rate at which data is loaded
     * default is 10, the other two options are 5 and 20
     */
    fun changeLidarFPS(newLFPS: Int) {
        lidarFPS = newLFPS // There was an +2 here, not sure why?
        newLidarFps.set(true)
        framesToSkip.set(MAX_LIDAR_FPS / lidarFPS - 1)
        // Reset the buffer to load new footage based on fps
        // Do not remove the ?. For some reason buffer can be null even though it is initialized as a val at creation.
        if (lidarFPS != lastFpsValue.getAndSet(lidarFPS)) {
            buffer?.let {
                thread {
                    it.clear()
                }
            }
        }
    }

    /**
     * updates the resolution of the application
     */
    fun changeResolution(height: Int, width: Int) {
        Gdx.graphics.setWindowedMode(width, height);
    }

    /**
     * switch Fixed Camera
     */
    fun switchFixedCamera(fixed: Boolean) {
        this.fixedCamera = fixed
        prefs.putBoolean("FIXED CAMERA",fixedCamera)
        settings.camera_checkbox.isChecked = fixed
    }

    /**
     * switch automatic camera
     */
    fun switchAutomaticCamera(automatic: Boolean) {
        this.automaticCamera = automatic
        prefs.putBoolean("AUTOMATIC CAMERA",automaticCamera)
        settings.automatic_camera_checkbox.isChecked = automatic
    }

    /**
     * skip 10 frames forwards
     */
    fun skipForward10frames() {
        this.framesIndex += 10
        buffer.skipForward(5f)
    }

    /**
    * toggle gps env
    */
    fun toggleGPS(toggle : Boolean) {
        this.gpsEnv.set(toggle)
        prefs.putBoolean("GPS ENVIRONMENT",gpsEnv.get())
//        settings.gps_environment_checkbox.isChecked = toggle
        if(gpsEnv.get()){
            rendableObjects.add(instance)
        } else {
            rendableObjects.remove(instance)
        }
    }


    /**
     * skip 10 frames backwards
     */
    fun skipBackwards10Frames() {
        this.framesIndex -= 10
        buffer.skipBackward(5f)
    }


    override fun dispose() {
        for (t in timers) {
            t.cancel()
        }
        decalBatch.dispose()
    }

    override fun resume() {
        pause.set(false)
    }

    override fun resize(width: Int, height: Int) {
        stage.clear()
        stage.viewport.update(1280, 720, true)
        gui.images.forEach { stage.addActor(it) }
        gui.draw()
        stage.viewport.update(width, height, true);
    }

    override fun pause() {
        pause.set(true)
    }

    override fun hide() {
        this.dispose()
    }

    override fun show() {
        println("called show")
        println(stage.viewport.screenWidth)
        println(stage.viewport.screenHeight)
        println(Gdx.graphics.width)
        println(Gdx.graphics.height)
        changeResolution(720,1280)
        create()
        stage.viewport.update(Gdx.graphics.width,Gdx.graphics.height)
//        changeResolution(Gdx.graphics.height, Gdx.graphics.width)
    }



    //-------Revised Camera Control Methods-----------------------

   /**
    * This method was used for testing initially,
    * it transformed into the main keyboard observer
    * all the buttons work with the HUD on
    * but it is mainly intended for use with the HUD turned off
    */
    fun campButtonpress() {

        val delta = Gdx.graphics.deltaTime

       if(fixedCamera == false && automaticCamera == false ) {

           //rotation of camera button
           if (Gdx.input.isKeyPressed(Input.Keys.W)) {
               rotateUp(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.S)) {
               rotateDown(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.A)) {
               rotateLeft(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.D)) {
               rotateRight(delta * 50)
           }


           //movement of camera buttons
           if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
               moveUp(delta*200)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
               moveDown(delta*200)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
               moveLeft(delta*200)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
               moveRight(delta*200)
           }

           if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
               moveForward(delta)
           }
           if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
               moveBackward((delta))
           }

           if (Gdx.input.isKeyPressed(Input.Keys.X)) {
               resetCamera()
           }
       } else {
           //move buttons while camera is fixed
           if (Gdx.input.isKeyPressed(Input.Keys.W)) {
               moveFixedUp(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.S)) {
               moveFixedDown(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.A)) {
               rotateFixedLeft(delta * 50)
           }
           if (Gdx.input.isKeyPressed(Input.Keys.D)) {
               rotateFixedRight(delta * 50)
           }

           if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
               zoomFixedCloser()
           }
           if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
               zoomFixedAway()
           }

           if (Gdx.input.isKeyPressed(Input.Keys.X)) {
               resetFixed()
           }
       }

        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rollRight(delta*40)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rollLeft(delta*40)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            switchAutomaticCamera(!automaticCamera)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            switchFixedCamera(!fixedCamera)
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            cmpss.changeCompression(1)
            prefs.putInteger("COMPRESSION",1)
            settings.compression_box.selected = 1
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            cmpss.changeCompression(2)
            prefs.putInteger("COMPRESSION",2)
            settings.compression_box.selected = 2
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            cmpss.changeCompression(3)
            prefs.putInteger("COMPRESSION",3)
            settings.compression_box.selected = 3
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            cmpss.changeCompression(4)
            prefs.putInteger("COMPRESSION",4)
            settings.compression_box.selected = 4
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            GameInitializer.screen = IndexScreen()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if(pause.get() == false ){
                pause()
            } else {
                resume()
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.COMMA)) {
            skipBackwards10Frames()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
            skipForward10frames()
        }

       if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
           gui.hideHUD()
           prefs.putBoolean("HIDE HUD", gui.hidden)
       }

       if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
           cmpss.switchGradualCompression(!cmpss.gradualCompression)
           settings.gradualBox.isChecked = cmpss.gradualCompression
       }

       if(Gdx.input.isKeyJustPressed(Input.Keys.M)) {
           toggleGPS(!gpsEnv.get())
       }

    }



    //----------TIll camera controls---------

    fun updateFixedCamera() {
        val x = fixedCamDistance * cos(fixedCamAzimuth) * cos(fixedCamAngle)
        val y = -fixedCamDistance * sin(fixedCamAzimuth) * cos(fixedCamAngle)
        val z = fixedCamDistance * sin(fixedCamAngle)
        cam.position.set(x, y, z)
        cam.up.set(Vector3.Z)
        cam.lookAt(Vector3(0f, 0f, 0f))
        cam.update()
    }

    fun moveAutomaticCamera(delta: Float) {
        rotateFixedRight(delta * AUTOMATIC_CAMERA_SPEED_MODIFIER)
    }

    fun zoomFixedCloser() {
        fixedCamDistance -= ZOOM_STEP_SIZE
        if (fixedCamDistance < FIXED_CAM_RADIUS_MIN)
            fixedCamDistance = FIXED_CAM_RADIUS_MIN
        if (fixedCamDistance > FIXED_CAM_RADIUS_MAX)
            fixedCamDistance = FIXED_CAM_RADIUS_MAX
    }

    fun zoomFixedAway() {
        fixedCamDistance += ZOOM_STEP_SIZE
        if (fixedCamDistance < FIXED_CAM_RADIUS_MIN)
            fixedCamDistance = FIXED_CAM_RADIUS_MIN
        if (fixedCamDistance > FIXED_CAM_RADIUS_MAX)
            fixedCamDistance = FIXED_CAM_RADIUS_MAX
    }

    fun rotateFixedLeft(delta: Float) {
        fixedCamAzimuth += delta * CAM_SPEED
        fixedCamAzimuth %= Math.PI.toFloat() * 2
    }

    fun rotateFixedRight(delta: Float) {
        fixedCamAzimuth -= delta * CAM_SPEED
        fixedCamAzimuth %= Math.PI.toFloat() * 2
    }

    fun moveFixedUp(delta: Float) {
        fixedCamAngle += delta * CAM_SPEED

        if (fixedCamAngle > FIXED_CAM_ANGLE_MAX)
            fixedCamAngle = FIXED_CAM_ANGLE_MAX
        if (fixedCamAngle < FIXED_CAM_ANGLE_MIN)
            fixedCamAngle = FIXED_CAM_ANGLE_MIN

        fixedCamAngle %= Math.PI.toFloat() * 2
    }

    fun moveFixedDown(delta: Float) {
        fixedCamAngle -= delta * CAM_SPEED

        if (fixedCamAngle > FIXED_CAM_ANGLE_MAX)
            fixedCamAngle = FIXED_CAM_ANGLE_MAX
        if (fixedCamAngle < FIXED_CAM_ANGLE_MIN)
            fixedCamAngle = FIXED_CAM_ANGLE_MIN

        fixedCamAngle %= Math.PI.toFloat() * 2
    }

    fun resetFixed() {
        fixedCamAngle = Math.PI.toFloat() * 0.3f
        fixedCamAzimuth = 30f
        fixedCamDistance = 70f
    }


    //----------Robert camera controls------------

    /*
     * @author Robert
     *  here methods for the camera controls can be found
     *  they make use of the camera's up and direction vector
     *  to rotate and move it
     */

    fun resetCamera() {
        cam.position[0f, 0f] = 30f
        cam.lookAt(0f, 0f, 0f)
        cam.up.set(0f, 1f, 0f)
        cam.update()
    }

    fun moveForward(delta: Float) {
        cam.translate(Vector3(cam.direction).scl(ZOOM_STEP_SIZE))
        cam.update()
    }

    fun moveBackward(delta: Float) {
        cam.translate(Vector3(cam.direction).scl(-ZOOM_STEP_SIZE))
        cam.update()
    }

    fun moveUp(delta: Float) {
        cam.translate(Vector3(cam.up).scl(delta * CAM_SPEED))
        cam.update()
    }

    fun moveDown(delta: Float) {
        cam.translate(Vector3(cam.up).scl(-delta * CAM_SPEED))
        cam.update()
    }

    fun moveLeft(delta: Float) {
        cam.translate(Vector3(cam.up).rotate(cam.direction, 90f).scl(-delta * CAM_SPEED))
        cam.update()
    }

    fun moveRight(delta: Float) {
        cam.translate(Vector3(cam.up).rotate(cam.direction, 90f).scl(delta * CAM_SPEED))
        cam.update()
    }

    fun rotateUp(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction, 90f), delta * ROTATION_ANGLE_MODIFIER)
        cam.update()

    }

    fun rotateDown(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction, 90f), -delta * ROTATION_ANGLE_MODIFIER)
        cam.update()
    }

    fun rotateLeft(delta: Float) {
        cam.rotate(cam.up, delta * ROTATION_ANGLE_MODIFIER)
        cam.update()
    }

    fun rotateRight(delta: Float) {
        cam.rotate(cam.up, -delta * ROTATION_ANGLE_MODIFIER)
        cam.update()
    }

    fun rollRight(delta: Float) {
        cam.rotate(cam.direction, -delta * ROTATION_ANGLE_MODIFIER)
        cam.update()
    }

    fun rollLeft(delta: Float) {
        cam.rotate(cam.direction, delta * ROTATION_ANGLE_MODIFIER)
        cam.update()
    }


    fun globeUpdate():Vector3{
        val forwardScalar = 5f
        var rightScalar = 4.4f
        var downwardsScalar = 2.3f

        if (gui.rotated){
            rightScalar = -4.4f
            downwardsScalar = -2.3f
        }

        val rightWard = Vector3(cam.up).rotate(cam.direction, 90f).scl(rightScalar)
        val ground = Vector3(cam.position)
        val forWard = Vector3(cam.direction).scl(forwardScalar)
        val downWard = Vector3(cam.up).scl(-1*downwardsScalar)
        ground.add(forWard)
        ground.add(rightWard)
        ground.add(downWard)
        return  ground
    }
}


