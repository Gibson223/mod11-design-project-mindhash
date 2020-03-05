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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import org.quokka.kotlin.Enviroment.Populator
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.concurrent.timer
import kotlin.math.sign


class Space : InputAdapter(), ApplicationListener {

    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null

    var modelBatch: ModelBatch? = null


    var spaceObjects: ArrayList<ModelInstance>? = null
    var instance: ModelInstance? = null

    var bottomBlock: Model? = null
    var proxi: Model? = null
    var onefourPointt: Model? = null
    var fiveeightPoint: Model? = null
    var ninetwelvePoint: Model? = null
    var thirteensixteenPoint: Model? = null
    var morePoint: Model? = null
    var pink: Texture? = null

    var frames: ConcurrentLinkedQueue<LidarFrame>? = null
    var framesIndex = 1800


    var environment: Environment? = null

    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null
    var renderedCount = 0

    override fun create() {
        modelBatch = ModelBatch()
        //-----------Camera Creation------------------
        cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam!!.position[30f, 30f] = 30f
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.near = 1f
        cam!!.far = 1000f
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


        bottomBlock = modelBuilder.createBox(
            10f, 10f, .5f,
            material,
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        proxi = modelBuilder.createBox(
                .1f, .1f, .1f,
                Material(ColorAttribute.createDiffuse(Color.GREEN)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        onefourPointt = modelBuilder.createBox(
            .1f, .1f, .1f,
            Material(ColorAttribute.createDiffuse(Color.BLUE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        fiveeightPoint = modelBuilder.createBox(
                .15f, .15f, .15f,
                Material(ColorAttribute.createDiffuse(Color.GREEN)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        ninetwelvePoint = modelBuilder.createBox(
                .2f, .2f, .2f,
                Material(ColorAttribute.createDiffuse(Color.YELLOW)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )
        ninetwelvePoint = modelBuilder.createBox(
                .25f, .25f, .25f,
                Material(ColorAttribute.createDiffuse(Color.CORAL)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )
        thirteensixteenPoint = modelBuilder.createBox(
                .3f, .3f, .3f,
                Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
        )

        morePoint = modelBuilder.createBox(
                .35f, .35f, .35f,
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
        for (inst in standIn) {
            if(isVisible(inst)) {
                modelBatch!!.render(inst, environment)
                renderedCount++
            }
        }
        modelBatch!!.end()


        string!!.setLength(0)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        string!!.append(" Rendered: ").append(renderedCount)
        string!!.append(" out of: ").append(standIn.size)
        label!!.setText(string)
        stage!!.draw()

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
            println("empty frame")
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

        if(frames!!.isEmpty()){
            objects.add(ModelInstance(proxi,0f,0f,0f))
            println("empty frame")
            return objects
        }

        var aux = frames!!.poll()

        aux.coords.forEach { c ->

            var crtX = c.x-c.x.toInt()* sign(c.x)
            var crtY = c.y-c.y.toInt()* sign(c.y)
            var crtZ = c.z-c.z.toInt()* sign(c.z)


            when (crtX){
                in 0f..0.5f -> crtX = c.x.toInt() *1f
                in 0.5f..0.99f -> crtX = c.x.toInt()+0.5f* sign(c.x)
//                in 0f..0.25f -> crtX = c.x.toInt()*1f
//                in 0.26f..0.5f -> crtX = c.x.toInt()+0.25f* sign(c.x)
//                in 0.51f..0.75f -> crtX = c.x.toInt()+0.5f* sign(c.x)
//                in 0.76f..0.99f -> crtX = c.x.toInt()+0.75f* sign(c.x)
            }
            when (crtY){
                in 0f..0.5f -> crtY = c.y.toInt() *1f
                in 0.5f..0.99f -> crtY = c.y.toInt() + 0.5f* sign(c.y)
//                in 0f..0.25f -> crtY = c.y.toInt() *1f
//                in 0.26f..0.5f -> crtY = c.y.toInt()+0.25f* sign(c.y)
//                in 0.51f..0.75f -> crtY = c.y.toInt()+0.5f* sign(c.y)
//                in 0.76f..0.99f -> crtY = c.y.toInt()+0.75f* sign(c.y)
            }
            when (crtZ) {
                in 0f..0.5f -> crtZ = c.z.toInt() * 1f
                in 0.5f..0.99f -> crtZ = c.z.toInt() + 0.5f* sign(c.z)
//                in 0f..0.25f -> crtZ = c.z.toInt() *1f
//                in 0.26f..0.5f -> crtZ = c.z.toInt()+0.25f* sign(c.z)
//                in 0.51f..0.75f -> crtZ = c.z.toInt()+0.5f* sign(c.z)
//                in 0.76f..0.99f -> crtZ = c.z.toInt()+0.75f* sign(c.z)
            }

            val tripp
                    = Triple(crtX,crtY,crtZ)
            if(map.keys.contains(tripp)){
                map.set(tripp,map.getValue(tripp)+1)
            } else {
                map.set(tripp,0)
            }
        }

        for (key in map.keys)
            when (map.get(key)){
                1,2,3,4 -> objects.add(ModelInstance(onefourPointt,
                        key.first
                        ,key.second
                        ,key.third))
                5,6,7,8 -> objects.add(ModelInstance(fiveeightPoint,
                        key.first
                        ,key.second,
                        key.third))
                9,10,11,12 -> objects.add(ModelInstance(ninetwelvePoint,
                        key.first
                        ,key.second
                        ,key.third))
                13,14,15,16 -> objects.add(ModelInstance(thirteensixteenPoint,
                        key.first
                        ,key.second
                        ,key.third))
                else -> objects.add(ModelInstance(morePoint,
                        key.first
                        ,key.second
                        ,key.third))
            }
        return  objects
    }


    fun newFrame() {
        timer("Array Creator", period = 150,initialDelay = 100) {
            Gdx.graphics.requestRendering();
//            render()
//            println("render requested")
            }
    }


    fun filepop() {
        timer("Array Creator", period = 1000,initialDelay = 0) {

            val ldrrdr = LidarReader()
            var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", framesIndex, framesIndex + 30)
            framesIndex += 30
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

    fun getMaxCoord(frame: LidarFrame):Triple<Int,Int,Int>{
        var x = 0
        var y = 0
        var z = 0

        for (i in frame.coords){
            if(i.x > x)
                x = i.x.toInt()
            if(i.y > y)
                y = i.y.toInt()
            if(i.z > z)
                z = i.z.toInt()
        }
        return Triple(x+1,y+1,z+1)
    }

    fun getMinCoord(frame: LidarFrame):Triple<Int,Int,Int>{
        var x = 0
        var y = 0
        var z = 0

        for (i in frame.coords){
            if(i.x < x)
                x = i.x.toInt()
            if(i.y < y)
                y = i.y.toInt()
            if(i.z < z)
                z = i.z.toInt()
        }
        return Triple(x-1,y-1,z-1)
    }
}



