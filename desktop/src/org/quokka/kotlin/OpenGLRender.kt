package org.quokka.kotlin

import LidarData.Database
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class OpenGLRender : Screen {
    val db: Database
    val mProgram: Int = 0
    var mPointBuffer: FloatBuffer
    var pointsSize: Int

    init {
        db = Database()
        db.connect("nyx", "lidar")
        val frame = db.getFrames(recordingId = 1, startFrame = 2000, numberOfFrames = 1).first()
        mPointBuffer = ByteBuffer.allocateDirect(frame.coords.size * 3 * 4).asFloatBuffer()
        frame.coords.forEach {
            mPointBuffer.put(it.x)
            mPointBuffer.put(it.y)
            mPointBuffer.put(it.z)
        }
        pointsSize = frame.coords.size
        db.close()
        render(0f)
    }

    override fun render(delta: Float) {

        val gl = Gdx.gl20
        gl.glClearColor(0.2f, 0f, 0f, 0f)
        gl.glUseProgram(mProgram)
        val buffId = gl.glGenBuffer()
        gl.glBufferData(GL20.GL_ARRAY_BUFFER, mPointBuffer.capacity() * 4, mPointBuffer, 0)
        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, buffId)
        gl.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 0, 0)
        gl.glEnableVertexAttribArray(0)

        gl.glDrawArrays(GL20.GL_POINTS, 0, pointsSize)
        gl.glDisableVertexAttribArray(0)
        gl.glUseProgram(0)
    }

    override fun show() {
        val gl = Gdx.gl20
    }

    override fun hide() {
    }

    override fun pause() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun resume() {
    }

    override fun dispose() {
    }
}

class PointCloudVisualizer : Game() {
    override fun create() {
        this.setScreen(OpenGLRender())
    }

    override fun render() {
        super.render()
    }
}

fun main() {
    PointCloudVisualizer()
}
