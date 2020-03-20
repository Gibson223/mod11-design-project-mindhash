package com.mygdx.game.desktop

import LidarData.Database
import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
import com.badlogic.gdx.*
import com.badlogic.gdx.Graphics.DisplayMode
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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import org.quokka.game.desktop.GameInitializer
import org.quokka.kotlin.Enviroment.GuiButtons
import org.quokka.kotlin.Enviroment.Settings
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


class Space: Screen {
    lateinit var plexer: InputMultiplexer
    val local = true
    var axis = false
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
    var framesIndex = 2400 //this is basically the timestamp



    var running = AtomicBoolean(true)
    var pause = AtomicBoolean(true)



    var cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    var camController = CameraInputController(cam)


    /**
     * dfcm distance from camera margin
     * used in deciding how compressed the data is
     * based on the point's distance from the camera
     */
    val dfcm = 15

    var modelBatch = ModelBatch()



    var spaceObjects = ArrayList<ModelInstance>(1) // TODO: remove? no uses


    var bottomBlock: Model? = null


    var frames = ConcurrentLinkedQueue<LidarFrame>()


    var environment = Environment()


    val stage = Stage()
    val font = BitmapFont()
    val label = Label(" ", LabelStyle(font, Color.WHITE))
    val string = StringBuilder()
    var errMessage = " "

    val database: Database
    var decalBatch = DecalBatch(CameraGroupStrategy(cam))

    val pix = Pixmap(1, 1, Pixmap.Format.RGB888)

    val settings = Settings(this)

    var decals: List<Decal> = listOf()
    val axisDecals: ArrayList<Decal> = ArrayList(30)

    val blueRedFade = Array(256) { i ->
        val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
        pix.setColor(i / 255f, 0f, 1 - i / 255f, 1f)
        pix.drawPixel(0, 0)
        TextureRegion(Texture(pix))
    }
    val blueYellowFade = Array(256) { i ->
        val pix = Pixmap(1, 1, Pixmap.Format.RGB888)
        pix.setColor(i / 255f, i / 255f, 1 - i / 255f, 1f)
        pix.drawPixel(0, 0)
        TextureRegion(Texture(pix))
    }
    var decalTextureRegion = TextureRegion(Texture(pix))



    init {
        database = Database()
        database.connect("lidar", "mindhash")
    }

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



        spaceObjects = ArrayList<ModelInstance>(1)
        frames = ConcurrentLinkedQueue<LidarFrame>()

        //---------Model Population----------

        pix.setColor(66f / 255, 135f / 255, 245f / 255, 1f)
        pix.drawPixel(0, 0)

        filepop()
        newFrame()



        // -----------Bottom Text--------
        stage.addActor(label)





        plexer = InputMultiplexer(stage, camController)
        Gdx.input.inputProcessor = plexer

    }
    

    override fun hide() {
        TODO("Not yet implemented")
    }

    override fun show() {
        create()
    }


    override fun render(delta: Float) {


        campButtonpress()

        //if the camera is fixed that means it's always looking at the center of the environment
        if (fixedCamera == true) {
            cam.lookAt(0f, 0f, 0f)
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        decals.forEach {d ->
            if (cam.frustum.boundsInFrustum(d.x,d.y,d.z,.3f,.3f,.3f) == true ) {
                decalBatch.add(d)
            }
        }

        if(axis == true ) {
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
     * this methods is called every tenth of a seconds
     * to load new data in the environment
     * the rate at which the method sis called results in
     * the LiDAR FPS
     */
    fun newFrame() {

        timer("Environment population", period = lidarFPStimer.toLong()*10 , initialDelay = 100) {
            if(newLidaarFPS.get() == false) { // the lidarFPS has not been changed
                if (pause.get() != false && frames.isNotEmpty()) {
                    if (compresion == 0) {
                        val f = frames.poll()
                        decals = f.coords.map {
                            val d = Decal.newDecal(0.08f, 0.08f, decalTextureRegion)
                            d.setPosition(it.x, it.y, it.z)
                            d.lookAt(cam.position, cam.up)
                            colorDecal(d, blueRedFade)
                            d
                        }
                    } else {
                        decals = compressPoints()
                        decals.forEach { d -> colorDecal(d, blueRedFade) }
                    }
                }
            } else {    // lidarFPS was changed, cancel the current task and start a new one
                        // with correct period
                this.cancel()
                newLidaarFPS.set(false)
                newFrame()
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
            if (pause.get() != false) {
                if (local == true) { // local for testing purposes only, it uses data from a .bag file
                    val ldrrdr = LidarReader()
                    var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + lidarFPS)
                    framesIndex += lidarFPS
                    intermetidate.forEach { f ->
                        frames.add(f)
                    }
                } else { //if local == false then the data is take from the database
                    if (frames.size < 20) {
                        val intermetidate = database.getFrames(1, framesIndex, lidarFPS)
                        framesIndex += lidarFPS
                        intermetidate.forEach { f ->
                            frames.add(f)
                        }

                    }
                }
            } else {
                decals.forEach { d->
                    d.lookAt(cam.position,cam.up)
                }
            }
        }
    }



    //--------Buttons methods-------------

    fun changeResolution(height: Int, width: Int){
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

    fun changeCompressionlvl(newcomp:Int){
        this.compresion = newcomp
    }

    fun changeGradualCompression(newset: Boolean){
        this.gradualCompression = newset
    }

    fun skipForward10frames(){
        this.framesIndex += 10
    }

    fun skipBackwards10Frames(){
        this.framesIndex -= 10
    }



    //------------------------------------------------

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
        } else throw Error("Divisions not prepared for divisions to be $divisions  ")
        return result
    }


    fun distanceAB3(a: LidarCoord, b: Vector3):Float{
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceAB3(a: Vector3, b: Vector3):Float{
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun disntaceAB2(x: Float,y:Float, a:Float, b:Float):Float{
        return sqrt((x-a).pow(2) + (y-b).pow(2))
    }

    /**
     * This methods decied the val of compression of a point
     * depending on the distance from the camera
     * @param coord is the coordinate being checked
     * @return 1,2,3,4 number of divisions,
     * will be fed into returnCPP
     */
    fun decidDivisions(coord: LidarCoord): Int {
        val camp = cam?.position
        if (camp != null) {
            val distance = distanceAB3(coord,camp)
                    sqrt((coord.x - camp.x).pow(2)
                            + (coord.y - camp.y).pow(2)
                            + (coord.z - camp.z).pow(2))

            val substraction = distance - dfcm

            when (compresion) { //compresion is the maximum level of compression
                                // 1 is least, then 4, 3 and finally 2
                1 -> return 1
                2 -> if (substraction < 0) {
                        return 1
                    } else if (substraction < dfcm) {
                        return 4
                    } else if (substraction < 2 * dfcm) {
                        return 3
                    } else {
                        return 2
                    }
                3 -> if (substraction < 0) {
                        return 1
                    } else if (substraction < dfcm) {
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
        bottomBlock?.dispose()
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
        if (frames.isEmpty()) {
            val d = Decal.newDecal(0.5f, 0.5f, decalTextureRegion)
            d.setPosition(0f, 0f, 0f)
            d.lookAt(cam.position, cam.up)
            println("empty frame")
            objects.add(d)
            return objects
        }

        var crtFrame = frames.poll()//get next frame

        crtFrame.coords.forEach { c ->
            var divisions = compresion //level of compression
            if(gradualCompression == true && compresion != 1) {
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
            d.lookAt(cam.position, cam.up)
            objects.add(d)
        }

        return objects
    }


    /**
     * methods for translating the camera through space
     * @author Robert
     * go up down left right from the point of view of the camera
     * go forward and backwards
     * rotate up down left right
     */
    //-------Camera Control Methods-----------------------


    val camSpeed = 10f
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
        } else if(Gdx.input.isKeyPressed(Input.Keys.W)){
            rotateUp(delta)
        } else if(Gdx.input.isKeyPressed(Input.Keys.S)){
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

    fun resetCamera(){
        cam.position[0f, 0f] = 30f
        cam.lookAt(0f, 0f, 0f)
        cam.up.set(0f,1f,0f)
        cam.update()
    }

    fun moveForward(delta: Float){
        cam.translate(Vector3(cam.direction).scl(delta * camSpeed))
        cam.update()
    }

    fun moveBackward(delta: Float){
        cam.translate(Vector3(cam.direction).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveUp(delta: Float){
        cam.translate(Vector3(cam.up).scl(delta * camSpeed))
        cam.update()
    }

    fun moveDown(delta: Float){
        cam.translate(Vector3(cam.up).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveLeft(delta: Float){
        cam.translate(Vector3(cam.up).rotate(cam.direction,90f).scl(-delta * camSpeed))
        cam.update()
    }

    fun moveRight(delta: Float){
        cam.translate(Vector3(cam.up).rotate(cam.direction,90f).scl(delta * camSpeed))
        cam.update()
    }

    fun rotateUp(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction,90f),delta*rotationAngle)
        cam.update()
    }

    fun rotateDown(delta: Float) {
        cam.rotate(Vector3(cam.up).rotate(cam.direction,90f),-delta*rotationAngle)
        cam.update()
    }

    fun rotateLeft(delta: Float) {
        cam.rotate(cam.up,delta*rotationAngle)
        cam.update()
    }

    fun rotateRight(delta: Float) {
        cam.rotate(cam.up,-delta*rotationAngle)
        cam.update()
    }


}





