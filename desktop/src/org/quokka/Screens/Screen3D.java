package org.quokka.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import org.quokka.game.desktop.GameInitializer;

public class Screen3D implements Screen {
    GameInitializer game;
    Texture bf_button;
    Texture ff_button;
    Texture arrows_button;
    Texture earth_button;
    Texture pause_button;
    Texture reset_button;
    Texture setting_button;

    public Screen3D (final GameInitializer game){
        this.game = game;
        bf_button = new Texture("Screen3D/bf_button.png");
        ff_button = new Texture("Screen3D/ff_button.png");
        arrows_button = new Texture("Screen3D/arrows_button.png");
        earth_button = new Texture("Screen3D/earth_button.png");
        pause_button = new Texture("Screen3D/pause_button.png");
        reset_button = new Texture("Screen3D/reset_button.png");
        setting_button = new Texture("Screen3D/setting_button.png");
    }

    @Override
    public void show(){

    }

    @Override
    public void render(float delta){
        Gdx.gl.glClearColor(1,1,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.batch.begin();
        game.batch.draw(bf_button,Gdx.graphics.getWidth() / 2 - 175,0);
        game.batch.draw(ff_button,Gdx.graphics.getWidth() / 2 + 75,0);
        game.batch.draw(pause_button,Gdx.graphics.getWidth() / 2 - 50,0);
        game.batch.draw(reset_button,Gdx.graphics.getWidth() - 110,Gdx.graphics.getHeight() - 251);
        game.batch.draw(setting_button,Gdx.graphics.getWidth() - 110,Gdx.graphics.getHeight() - 101);
        game.batch.draw(arrows_button,Gdx.graphics.getWidth() - 251,0);
        game.batch.draw(earth_button,Gdx.graphics.getWidth() - 215,70);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height){

    }

    @Override
    public void pause(){

    }

    @Override
    public void resume(){

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
