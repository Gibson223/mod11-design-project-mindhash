package com.mygdx.game.desktop

import LidarData.Database
import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


class Space : InputAdapter(), ApplicationListener {

    val compressed = false
    val local = true
    var axis = false

    var lidarFPS = 12

    var running = AtomicBoolean(true)
    var pause = AtomicBoolean(false)

    //-------GUI controlls-----
    var fixedCamera = false


    var cam: PerspectiveCamera? = null
    var camController: CameraInputController? = null

    /**
     * dfcm distance from camera margin
     * used in deciding how compressed the data is
     * based on the point's distance from the camera
     */
    val dfcm = 15

    var modelBatch: ModelBatch? = null


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
    val axisDecals: ArrayList<Decal> = ArrayList(30)


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


        for(i in -50..50){
            val dx = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dx.setPosition(i*-1f,-1f,-1f)
            dx.lookAt(cam!!.position, cam!!.up)
            val dy = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dy.setPosition(-1f,-1f*i,-1f)
            dy.lookAt(cam!!.position, cam!!.up)
            val dz = Decal.newDecal(.25f, .25f, decalTextureRegion)
            dz.setPosition(-1f,-1f,i*-1f)
            dz.lookAt(cam!!.position, cam!!.up)
            axisDecals.add(dx)
            axisDecals.add(dy)
            axisDecals.add(dz)
        }


        // -----------Bottom Text--------
        stage = Stage()
        font = BitmapFont()
        label = Label(" ", LabelStyle(font, Color.WHITE))
        stage!!.addActor(label)
        string = StringBuilder()


        filepop()
        newFrame()
    }


    /**
     * this is the render method
     * it is called 60 times per second
     * it renders the environment and the camera within
     */
    override fun render() {
//        camController!!.update()


        campButtonpress()

        //if the camera is fixed that means it's always looking at the center of the environment
        if (fixedCamera == true) {
            cam!!.lookAt(0f, 0f, 0f)
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)


        decals.forEach {d ->
            if (cam!!.frustum.boundsInFrustum(d.x,d.y,d.z,.3f,.3f,.3f) == true ) {
                decalBatch!!.add(d)
            }
        }

        if(axis == true ) {
            axisDecals.forEach { d ->
                if (cam!!.frustum.boundsInFrustum(d.x, d.y, d.z, .3f, .3f, .3f) == true) {
                    decalBatch!!.add(d)
                }
            }
        }

        decalBatch!!.flush()


        string!!.setLength(0)
        string!!.append(errMessage)
        string!!.append(" up : ").append(cam!!.up)
        string!!.append(" direction: ").append(cam!!.direction)
        label!!.setText(string)
        stage!!.act(Gdx.graphics.getDeltaTime())
        stage!!.draw()
        errMessage = ""

    }

    /**
     * this methods is called every tenth of a seconds
     * to load new data in the environment by changing
     * the global variable decal 
     * which is both a List<Decal>
     * @author Robert
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
                    decals = compressPoints()
                    decals.forEach { d -> colorDecal(d, blueRedFade) }
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


    /**
     * this method retrieves information from the DB
     * it is called periodically every second
     * and retrieves lidarFPS(global variable) number of frames
     * the lidar data is generate at 10 frames per second
     * @author Robert
     */
    fun filepop() {
        timer("Array Creator", period = 1000, initialDelay = 0) {
            if (pause.get() == false) {
                if (local == true) { // local for testing purposes only, it uses data from a .bag file
                    val ldrrdr = LidarReader()
                    var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + lidarFPS)
                    framesIndex += lidarFPS
                    intermetidate.forEach { f ->
                        frames!!.add(f)
                    }
                } else { //if local == false then the data is take from the database
                    if (frames!!.size < 20) {
                        val intermetidate = database.getFrames(1, framesIndex, lidarFPS)
                        framesIndex += lidarFPS
                        intermetidate.forEach { f ->
                            frames!!.add(f)
                        }

                    }
                }
            } else {
                decals.forEach { d->
                    d.lookAt(cam!!.position,cam!!.up)
                }
            }
        }
    }


    /**
     * this methods returns the parent of a point
     * the parent of a point is a point to which the initial point is aproximated
     * @param a is the number being tested
     * @param divisions is the number of divisions meaning
     * if it is 1 then then the number is aproximated to itself
     * if it is 2 then then number is approximated to closes .5 or .0
     * @author Robert
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
     * @author Robert
     */
    fun decidDivisions(coord: LidarCoord): Int {
        val camp = cam?.position
        if (camp != null) {
            val distance = distanceBetween2points(coord,camp)

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
     * @author Robert
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
            val baseSizeofDecal = .2f

            for (i in 0..8){
                if (map.get(k) in i*margin .. (i+1)*margin){
                    d.setDimensions(baseSizeofDecal+i*0.02f,baseSizeofDecal+i*0.02f)
                }
            }
            d.setPosition(k.x, k.y, k.z)
            d.lookAt(cam!!.position, cam!!.up)
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
    fun distanceBetween2points(a: LidarCoord, b:LidarCoord):Float{
        return  sqrt((a.x - b.x).pow(2)
                        + (a.y - b.y).pow(2)
                        + (a.z - b.z).pow(2))
    }
    fun distanceBetween2points(a: Vector3, b:LidarCoord):Float{
        return  sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }
    fun distanceBetween2points(a: LidarCoord, b:Vector3):Float{
        return  sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }
    fun distanceBetween2points(a: Vector3, b:Vector3):Float{
        return  sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }
    /**
     * @author Robert
     */
    //-------Camera Control Methods-----------------------


    val camSpeed = 10f
    val rotationAngle = 50f


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
        } else if (Gdx.input.isKeyPressed(Input.Keys.R)) {
            resetCamera()
        }
    }

    fun resetCamera(){
        cam!!.position[0f, 0f] = 30f
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.update()
    }

    fun moveForward(delta: Float){
        cam!!.translate(Vector3(cam!!.direction).scl(delta * camSpeed))
        cam!!.update()
    }

    fun moveBackward(delta: Float){
        cam!!.translate(Vector3(cam!!.direction).scl(-delta * camSpeed))
        cam!!.update()
    }

    fun moveUp(delta: Float){
        cam!!.translate(Vector3(cam!!.up).scl(delta * camSpeed))
        cam!!.update()
    }

    fun moveDown(delta: Float){
        cam!!.translate(Vector3(cam!!.up).scl(-delta * camSpeed))
        cam!!.update()
    }

    fun moveLeft(delta: Float){
        cam!!.translate(Vector3(cam!!.up).rotate(cam!!.direction,90f).scl(-delta * camSpeed))
        cam!!.update()
    }

    fun moveRight(delta: Float){
        cam!!.translate(Vector3(cam!!.up).rotate(cam!!.direction,90f).scl(delta * camSpeed))
        cam!!.update()
    }

    fun rotateUp(delta: Float) {
        cam!!.rotate(Vector3(cam!!.up).rotate(cam!!.direction,90f),delta*rotationAngle)
        cam!!.update()
    }

    fun rotateDown(delta: Float) {
        cam!!.rotate(Vector3(cam!!.up).rotate(cam!!.direction,90f),-delta*rotationAngle)
        cam!!.update()
    }

    fun rotateLeft(delta: Float) {
        cam!!.rotate(cam!!.up,delta*rotationAngle)
        cam!!.update()
    }

    fun rotateRight(delta: Float) {
        cam!!.rotate(cam!!.up,-delta*rotationAngle)
        cam!!.update()
    }

    fun rotateZ(){
        cam!!.rotate(Vector3(0f,0f,1f),rotationAngle)
        cam!!.update()

    }

    fun rotateZrev(){
        cam!!.rotate(Vector3(0f,0f,1f),-rotationAngle)
        cam!!.update()

    }


}





