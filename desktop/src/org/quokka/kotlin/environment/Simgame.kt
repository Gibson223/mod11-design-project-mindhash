package org.quokka.kotlin.environment

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer


class Simgame: ApplicationAdapter() {

    private var camera: OrthographicCamera? = null
    private var batch: SpriteBatch? = null
    private var sim: Simulation? = null
    private var crtBoard: Array<Array<Simulation.SpaceUnit>>? = null


    override fun create() {

        camera = OrthographicCamera()
        camera!!.setToOrtho(false, 800f, 480f)
        batch = SpriteBatch()

        sim = Simulation()
        crtBoard = sim!!.build(250,250,200,100000f)
    }


    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera?.update();
        val shape =  ShapeRenderer()

        batch?.begin();
        for (i in 0..1000)
            for (j in 0..1000){
                shape.setProjectionMatrix(camera!!.combined);
                shape.begin(ShapeRenderer.ShapeType.Line);
                if (crtBoard!![i][j].reg == 0){
                    shape.setColor(Color.GREEN);
                } else {
                    shape.setColor(Color.RED);
                }
                if(crtBoard!![i][j].transition == true ){
                    if (crtBoard!![i][j].reg != 0){
                        shape.setColor(Color.YELLOW);
                    } else {
                     shape.setColor(Color.CYAN);
                  }
                }
                shape.rect(i.toFloat(), j.toFloat(), 3f, 3f);
                shape.end();
            }

        batch?.end();


    }
}

