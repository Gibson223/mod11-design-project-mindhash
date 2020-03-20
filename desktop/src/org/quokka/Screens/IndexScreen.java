package org.quokka.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.mygdx.game.desktop.Space;
import org.quokka.game.desktop.GameInitializer;

public class IndexScreen implements Screen {
    GameInitializer game;
    Texture img;
    Texture img2;
    BitmapFont font;
    Stage stage;
    Dialog dialog;
    Skin skin;

   public IndexScreen(final GameInitializer game){
       this.game = game;
       img = new Texture("UTLogo.jpg");
       img2 = new Texture("MindhashLogo2.jpg");
       font = new BitmapFont();
       font.setColor(Color.BLACK);
       font.getData().setScale(2);
       stage = new Stage();

       skin = new Skin(Gdx.files.internal("Skins/glassy-ui.json"));
       dialog = new Dialog("", skin);
       dialog.setSize(200,200);
       dialog.setPosition(Gdx.graphics.getWidth()/2 - 100,Gdx.graphics.getHeight()/2 - 101);

       final SelectBox<String> selectBox = new SelectBox<String>(skin);
       selectBox.setItems("First file","Second file","Third file");

       dialog.getContentTable().defaults().pad(10);
       dialog.getContentTable().add(selectBox);

       stage.addActor(dialog);

       Image badge = new Image(new Texture("Startbutton.png"));
       badge.setPosition(Gdx.graphics.getWidth()/2 - 151, Gdx.graphics.getHeight()/2 - 401);
       badge.addListener( new ClickListener() {
           @Override
           public void clicked(InputEvent event, float x, float y) {
               System.out.println("Clicked!");
               game.setScreen(new Space());
           }
       });

       stage.addActor(badge);

   }

    @Override
    public void show(){
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        GameInitializer.batch.begin();
        GameInitializer.batch.draw(img, 0, Gdx.graphics.getHeight() - 100);
        GameInitializer.batch.draw(img2, Gdx.graphics.getWidth() - 201, Gdx.graphics.getHeight() - 100);
        font.draw(GameInitializer.batch, "Press escape to exit the application", Gdx.graphics.getWidth() / 2  - 250, Gdx.graphics.getHeight() - 20);
        GameInitializer.batch.end();

        stage.act();
        stage.draw();
        Gdx.input.setInputProcessor(stage);

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
        		System.exit(0);
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        img.dispose();
        img2.dispose();
        System.out.println("MyGdxGame disposed");
    }
}
