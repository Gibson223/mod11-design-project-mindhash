package com.mygdx.game.desktop

import com.badlogic.gdx.*
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.ModelLoader
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import java.util.*
import kotlin.collections.ArrayList


internal class Basic3DTestK : InputAdapter(), ApplicationListener {

    var cam: PerspectiveCamera? = null
    var plexer: InputMultiplexer? = null
    var camController: CameraInputController? = null

    var modelBatch: ModelBatch? = null
    var instance: ModelInstance? = null

    var aseetM: AssetManager? = null

    var bottomBlock: Model? = null
    var camfollower: Model? = null
    var ship: Model? = null
    var proxi: Model? = null
    private var selectionMaterial: Material? = null
    private var originalMaterial: Material? = null

    var pink: Texture? = null


    var environment: Environment? = null

    var array: ArrayList<ModelInstance>? = null
    var ugggggh: ModelInstance? = null
    //    protected Array<Foo> instances = new Array<Foo>();
    var stage: Stage? = null
    var font: BitmapFont? = null
    var label: Label? = null
    var string: StringBuilder? = null

    private val visibleCount = 0
    private val position = Vector3()
    private val selected = -1
    private val selecting = -1

    private val randx = (0..53000).shuffled()
    private val randy = (0..53000).shuffled()
    private val randz = (0..53000).shuffled()
    private var offzet = 0

    override fun create() {
        modelBatch = ModelBatch()
        //-----------Camera------------------
        cam = PerspectiveCamera(67F, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        cam!!.position[30f, 30f] = 30f
        cam!!.lookAt(0f, 0f, 0f)
        cam!!.near = 1f
        cam!!.far = 10000f
        cam!!.update()

        //---------Camera controls--------
        camController = CameraInputController(cam)
        Gdx.input.inputProcessor = camController
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        

        //---------Model Population----------
        var modelBuilder = ModelBuilder()


        aseetM = AssetManager()
        aseetM!!.load("assets/bucket.png", Texture::class.java)

        var meshbuild = MeshBuilder();
        modelBuilder.begin()
        modelBuilder.node().id = "Floor"
//        modelBuilder.part("Floor",GL10.GL_TRIANGLES,VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal)

        pink = Texture(Gdx.files.internal("assets/korea.jpeg"),false)
        pink!!.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pink!!.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        var material = Material(TextureAttribute.createDiffuse(pink))

//        meshbuild = modelBuilder.part("room1", GL20.GL_TRIANGLES,
//                VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or
//                        VertexAttributes.Usage.TextureCoordinates, material)
        modelBuilder.end()



        ugggggh = ModelInstance( modelBuilder.createBox(10f, 10f, 10f,
                material,
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong()),
                0f,0f,0f)

        bottomBlock = modelBuilder.createBox(10f, 10f, .5f,
                material,
                (VertexAttributes.Usage.Position or  VertexAttributes.Usage.Normal.toLong().toInt()).toLong())


        camfollower = modelBuilder.createBox(2f, 2f, 2f,
                Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal.toLong().toInt()).toLong())

        proxi = modelBuilder.createBox(.5f, .5f, .5f,
                Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates or VertexAttributes.Usage.Normal.toLong().toInt()).toLong())


        val loader: ModelLoader<*> = ObjLoader()
        ship = loader.loadModel(Gdx.files.internal("assets/ship.obj"))


        randpop()

//        pink = Texture(Gdx.files.internal("assets/pink.jpg")) // #3
//        sprbtch = SpriteBatch()

        // populate()
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


        selectionMaterial = Material()
        selectionMaterial!!.set(ColorAttribute.createDiffuse(Color.ORANGE))
        originalMaterial = Material()
    }



    override fun render() {
        camController!!.update()



//        sprbtch?.begin();
//        sprbtch?.draw(pink, 100f, 100f)
//        sprbtch?.end();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        modelBatch!!.begin(cam)
        pink!!.bind()
        modelBatch!!.render(array, environment)
        modelBatch!!.end()


        string!!.setLength(0)
        string!!.append(" FPS: ").append(Gdx.graphics.framesPerSecond)
        // string.append(" Visible: ").append(cam.position);
        string!!.append(cam!!.combined)
        label!!.setText(string)
        stage!!.draw()

    }

    fun randpop(){
        array = ArrayList(53000)
//        for (i in 0..53000){
//            var rand = (0..53000).shuffled()
//            instance = ModelInstance(proxi,randx.get(i)*1F,randy.get(i)*1F,randz.get(i)*1F)
//            array!!.add(instance!!)
//        }
        for (i in 0..100)
            for(j in 0..50)
                for (k in 0..10) {
                    instance = ModelInstance(proxi, i * 1f, j * 1f, k * 1f)
                    array!!.add(instance!!)
                }
    }

    fun populate(){
        array = ArrayList(1001)

        instance = ModelInstance(bottomBlock,0f,0f,0f)
        array!!.add(instance!!)
        for(i in -25..25){
            for (j in -25..25){
//                for (k in -25..25) {
                    instance = ModelInstance(proxi, i * 1F, j * 1F, 1F)
                    array!!.add(instance!!)
//                }
            }
//            instance = ModelInstance(proxi,randx+offzet+i,randy+i,10f)
//            array!!.add(instance!!)
        }
//        if(randx + offzet>50){
//            offzet = 0
//        }
//        offzet++

    }
    override fun dispose() {
        modelBatch!!.dispose()
        camfollower!!.dispose()
        bottomBlock!!.dispose()
    }

    override fun resume() {}
    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
}