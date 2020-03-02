package org.quokka.game.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import com.badlogic.gdx.maps.Map;

import java.util.ArrayList;
import java.util.Iterator;




class Basic3DTestt extends InputAdapter implements ApplicationListener {

//    public static class Foo extends ModelInstance {
//        public final Vector3 center = new Vector3();
//        public final Vector3 dimensions = new Vector3();
//        public final float radius;
//
//        private final static BoundingBox bounds = new BoundingBox();
//
//        public Foo (Model model, String rootNode, boolean mergeTransform) {
//            super(model, rootNode, mergeTransform);
//            calculateBoundingBox(bounds);
//            bounds.getCenter(center);
//            bounds.getDimensions(dimensions);
//            radius = dimensions.len() / 2f;
//        }
//    }

    public PerspectiveCamera cam;
    public ModelBatch modelBatch;
    public ModelInstance instance;
    public Model bsicModel;
    public Model camfollower;
    public Model proxi;
    public Texture pink;

    public Environment environment;

    public CameraInputController camController;

    public InputMultiplexer plexer ;

    public ArrayList<ModelInstance> array;
//    protected Array<Foo> instances = new Array<Foo>();

    public Stage stage;
    public BitmapFont font;
    public Label label;
    public StringBuilder string;


    private int visibleCount;
    private Vector3 position = new Vector3();

    private int selected = -1, selecting = -1;
    private Material selectionMaterial;
    private Material originalMaterial;

    @Override
    public void create () {
        modelBatch = new ModelBatch();

        //-----------Camera------------------
        cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        System.out.println(Gdx.graphics.getWidth());
        cam.position.set(0f, 0f, 20f);
        cam.lookAt(0,0,0);
        cam.near = 1f;
        cam.far = 10000f;
        System.out.println(cam.combined);
        cam.update();
        //---------Camera controls--------
        camController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(camController);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        //---------Model Population----------
        ModelBuilder modelBuilder = new ModelBuilder();
        bsicModel = modelBuilder.createBox(.5f, .5f, .5f,
                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        instance = new ModelInstance(bsicModel);


        camfollower = modelBuilder.createBox(2f,2f,2f,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        proxi = modelBuilder.createBox(1f,1f,1f,
                new Material(ColorAttribute.createDiffuse(Color.BLUE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);



        array = new ArrayList<>(1001);
//        array.add(instance);
//        for(int i=0; i<10;i++){
//            for(int j=0; j<10;j++) {
//                for (int k = 0; k < 10; k++) {
//                    instance = new ModelInstance(model, i * 10, j * 10, k * 10);
//                    array.add(instance);
//                }
//            }
//        }
        instance = new ModelInstance(bsicModel, 0, 0, 0);
        array.add(instance);
        array.add(instance);
        array.add(instance);

        // -----------Bottom Text--------
        stage = new Stage();
        font = new BitmapFont();
        label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
        stage.addActor(label);
        Actor act = new Actor();
        act.setColor(Color.BROWN);
        stage.addActor(act);

        string = new StringBuilder();

        plexer =new InputMultiplexer((InputProcessor) this, camController);
        Gdx.input.setInputProcessor(plexer);

        selectionMaterial = new Material();
        selectionMaterial.set(ColorAttribute.createDiffuse(Color.ORANGE));
        originalMaterial = new Material();



    }

    public int getSign(float x){
        if(x>=0){
            return 1;
        } else
            return  -1;
    }

    public float getPositive(float x){
//        if(x>=0){
//            return x;
//        } else
//            return  x*(-1);
        return  x;
    }

    @Override
    public void render () {

        camController.update();

        //instance = new ModelInstance(model, cam.position.x+30+cam.direction.x,cam.position.y+5+cam.direction.y,cam.position.z+5+cam.direction.z);
        float fallowx = 0;
        float fallowy = 0;
        float fallowz = 0;

        fallowx = cam.direction.x*20+cam.position.x;
        fallowy = cam.direction.y*20+cam.position.y;
        fallowz = cam.direction.z*20+cam.position.z;


        instance = new ModelInstance(camfollower, fallowx,fallowy,fallowz);
        array.set(0,instance);


        fallowx +=4*cam.direction.x;
        fallowy +=4*cam.direction.y;
        fallowz +=4*cam.direction.z;

        instance = new ModelInstance(proxi, fallowx,fallowy,fallowz);
        array.set(1,instance);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);

        modelBatch.render(array,environment);
        modelBatch.end();


        string.setLength(0);
        string.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
       // string.append(" Visible: ").append(cam.position);
        string.append(" x: ").append(fallowx);
        string.append(" y: ").append(fallowy);
        string.append(" z: ").append(fallowz);
        string.append(cam.combined);
        label.setText(string);
        stage.draw();
    }

//    public int getObject (int screenX, int screenY) {
//        Ray ray = cam.getPickRay(screenX, screenY);
//        int result = -1;
//        float distance = -1;
//        for (int i = 0; i < instances.size; ++i) {
//            final Foo instance = instances.get(i);
//            instance.transform.getTranslation(position);
//            position.add(instance.center);
//            float dist2 = ray.origin.dst2(position);
//            if (distance >= 0f && dist2 > distance) continue;
//            if (Intersector.intersectRaySphere(ray, position, instance.radius, null)) {
//                result = i;
//                distance = dist2;
//            }
//        }
//        return result;
//    }
//
//    @Override
//    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//        selecting = getObject(screenX, screenY);
//        return selecting >= 0;
//    }
//
//    @Override
//    public boolean touchDragged(int screenX, int screenY, int pointer) {
//        return selecting >= 0;
//    }
//
//    @Override
//    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//        if (selecting >= 0) {
//            if (selecting == getObject(screenX, screenY))
//                setSelected(selecting);
//            selecting = -1;
//            return true;
//        }
//        return false;
//    }
//    public void setSelected (int value) {
//        if (selected == value) return;
//        if (selected >= 0) {
//            Material mat = instances.get(selected).materials.get(0);
//            mat.clear();
//            mat.set(originalMaterial);
//        }
//        selected = value;
//        if (selected >= 0) {
//            Material mat = instances.get(selected).materials.get(0);
//            originalMaterial.clear();
//            originalMaterial.set(mat);
//            mat.clear();
//            mat.set(selectionMaterial);
//        }
//    }

    @Override
    public void dispose () {
        modelBatch.dispose();
        camfollower.dispose();
        bsicModel.dispose();
    }

    @Override
    public void resume () {
    }

    @Override
    public void resize (int width, int height) {
    }

    @Override
    public void pause () {
    }
}