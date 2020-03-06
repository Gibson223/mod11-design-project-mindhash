package com.mygdx.game.desktop

import LidarData.LidarCoord
import LidarData.LidarFrame
import LidarData.LidarReader
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import org.quokka.kotlin.Enviroment.Populator
import java.lang.Error
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.concurrent.timer
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


class Space : InputAdapter(), ApplicationListener {

    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null
    val dfcm = 8 //distnace from camera margin

    var modelBatch: ModelBatch? = null


    var spaceObjects: ArrayList<ModelInstance>? = null
    var instance: ModelInstance? = null

    var bottomBlock: Model? = null
    var proxi: Model? = null
    var onethreePoint: Model? = null
    var foursixPoint: Model? = null
    var sevenninePoint: Model? = null
    var tentwelvePoint: Model? = null
    var thriteenfifteenPoint: Model? = null
    var morePoint: Model? = null
    var pink: Texture? = null

    var frames: ConcurrentLinkedQueue<LidarFrame>? = null
    var framesIndex = 1800


    var environment: Environment? = null

    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null
    var errMessage = " "
    var renderedCount = 0

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

        modelBuilder.begin()
        modelBuilder.node().id = "Floor"
        pink = Texture(Gdx.files.internal("core/assets/badlogic.jpg"), false)
        pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        var material = Material(TextureAttribute.createDiffuse(pink))
        modelBuilder.end()

        val boxsize = .35f

        bottomBlock = modelBuilder.createBox(
            10f, 10f, .5f,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        proxi = Decal

        proxi = modelBuilder.createBox(
                10f, 10f, 10f,
                Material(ColorAttribute.createDiffuse(Color.GREEN)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        onethreePoint = modelBuilder.createBox(boxsize,boxsize,boxsize,
            Material(ColorAttribute.createDiffuse(Color.LIME)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        foursixPoint = modelBuilder.createBox(boxsize,boxsize,boxsize,
                Material(ColorAttribute.createDiffuse(Color.YELLOW)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        sevenninePoint = modelBuilder.createBox(
                boxsize,boxsize,boxsize,
                Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )
        tentwelvePoint = modelBuilder.createBox(
                boxsize,boxsize,boxsize,
                Material(ColorAttribute.createDiffuse(Color.BLUE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )
        thriteenfifteenPoint = modelBuilder.createBox(
                boxsize,boxsize,boxsize,
                Material(ColorAttribute.createDiffuse(Color.PINK)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

//        fifteeneighteenPoint = modelBuilder.createBox(
//               .35f, .35f, .35f,
//                Material(ColorAttribute.createDiffuse(Color.GOLD)),
//                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
//        )

        morePoint = modelBuilder.createBox(
                boxsize+.2f,boxsize+.2f,boxsize+.2f,
                Material(ColorAttribute.createDiffuse(Color.RED)),
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



        Gdx.graphics.setContinuousRendering(false);

        filepop()
        newFrame()
    }


    override fun render() {
        camController!!.update()
        renderedCount = 0


        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch!!.begin(cam)
        pink!!.bind()
        val standIn = shave()
//        val standIn = getNewCoord()
        for (inst in standIn) {
            if(isVisible(inst)) {
                modelBatch!!.render(inst, environment)
                renderedCount++
            }
        }
        modelBatch!!.end()


        string!!.setLength(0)
        string!!.append(errMessage)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        string!!.append(" Rendered: ").append(renderedCount)
        string!!.append(" out of: ").append(standIn.size)
        string!!.append(" cam: ").append(cam?.position)
        label!!.setText(string)
        stage!!.draw()
        errMessage = ""

}

    fun isVisible(inst:ModelInstance):Boolean{
        var position = Vector3()
        inst!!.transform.getTranslation(position);
        return cam!!.frustum.pointInFrustum(position);
    }



    fun getNewCoord(): ArrayList<ModelInstance>{
        var result= ArrayList<ModelInstance>()
        val aux = frames!!.poll()
        if(frames!!.isEmpty()){
            result.add(ModelInstance(proxi,0f,0f,0f))
            errMessage="empty frame"
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

    fun shave():ArrayList<ModelInstance>{
        var objects = ArrayList<ModelInstance>(15)
        var map = HashMap<Triple<Float,Float,Float>,Int>()

//        if(frames!!.isEmpty()){
//            objects.add(ModelInstance(proxi,0f,0f,0f))
//            println("empty frame")
//            return objects
//        }

        while(frames!!.isEmpty() || frames!!.peek().coords.size < 1000){
            frames!!.poll()
        }

        var crtFrame = frames!!.poll()
        crtFrame.coords.forEach { c ->

            val divisions = decidDivisions(c)

            val tripp
                    = Triple(
                    decideCPR(c.x,divisions),
                    decideCPR(c.y,divisions),
                    decideCPR(c.z,divisions))

            if(map.keys.contains(tripp)){
                map.set(tripp,map.getValue(tripp)+1)
            } else {
                map.set(tripp,1)
            }
        }

        val margin = 5
        for (key in map.keys){
            if(map.get(key) in 1..margin){
                objects.add(ModelInstance(onethreePoint,
                        key.first
                        ,key.second
                        ,key.third))

            } else if (map.get(key) in 1*margin..2*margin) {
                objects.add(ModelInstance(foursixPoint,
                        key.first
                        ,key.second,
                        key.third))

            } else if (map.get(key) in 3*margin..4*margin) {
                objects.add(ModelInstance(sevenninePoint,
                        key.first
                        ,key.second
                        ,key.third))
            } else if (map.get(key) in 4*margin..5*margin) {
                objects.add(ModelInstance(tentwelvePoint,
                        key.first
                        , key.second
                        , key.third))
            } else if (map.get(key) in 5*margin..6*margin) {
                objects.add(ModelInstance(thriteenfifteenPoint,
                        key.first
                        , key.second
                        , key.third))
            } else if (map.get(key) in 6*margin..7*margin) {
                objects.add(ModelInstance(morePoint,
                        key.first
                        , key.second
                        , key.third))
            }
        }

        return  objects
    }


    fun decideCPR(a:Float,divisions:Int):Float{
        var result = 0f
        var auxxx = 0f
        if(a > -1 && a < 1){
            auxxx = a
        } else {
            auxxx = a - a.toInt()
        }
        val margin:Float
        if(divisions == 1){
            return  a
        } else if(divisions == 2) {
            margin = .5f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..1f -> result = a.toInt() + margin * sign(a)

                in -1f..margin*-1 -> result = a.toInt() + margin * sign(a)
                in margin*-1..0f -> result = a.toInt() * 1f
            }
        } else if(divisions == 3){
            margin = .33f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..margin*2 -> result = a.toInt() + margin * sign(a)
                in margin*2..1f -> result = a.toInt() + margin *2* sign(a)

                in margin*-1..0f -> result = a.toInt() * 1f
                in margin*-2..margin*-1 -> result = a.toInt() + margin * sign(a)
                in -1f..margin*-2 -> result = a.toInt() + margin *2* sign(a)
            }

        } else if(divisions == 4) {
            margin = .25f
            when (auxxx) {
                in 0f..margin -> result = a.toInt() * 1f
                in margin..margin * 2 -> result = a.toInt() + margin * sign(a)
                in margin * 2..margin * 3 -> result = a.toInt() + margin * 2 * sign(a)
                in margin * 3..1f -> result = a.toInt() + margin * 3 * sign(a)

                in margin*-1..0f -> result = a.toInt() * 1f
                in -1f..margin * -3 -> result = a.toInt() + margin *3* sign(a)
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
    fun decidDivisions(coord: LidarCoord):Int{
        val camp = cam?.position
        if (camp != null) {
            val distance =
                    sqrt((coord.x-camp.x).pow(2)
                    + (coord.y-camp.y).pow(2)
                    + (coord.z-camp.z).pow(2))

            val substraction = distance -dfcm
            if (substraction < 0){
                return 1
            } else if (substraction < dfcm ){
                return 2
            } else if (substraction < 2*dfcm){
                return 3
            } else {
                return 4
            }

        } else throw Error("Could not find camera position in decidDivisions")
    }


    fun newFrame() {
        timer("Array Creator", period = 100,initialDelay = 100) {
            Gdx.graphics.requestRendering();
            }
    }


    fun filepop() {
        timer("Array Creator", period = 1000,initialDelay = 0) {

            val fps = 20

            val ldrrdr = LidarReader()
            var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + fps)
            framesIndex += fps
            intermetidate.forEach { f ->
                frames!!.add(f)
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

    override fun resize(width: Int, height: Int) {
        stage?.getViewport()?.update(width, height, true);
    }
    override fun pause() {}


}



