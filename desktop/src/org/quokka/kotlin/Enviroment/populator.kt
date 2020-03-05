package org.quokka.kotlin.Enviroment

import LidarData.LidarReader
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.mygdx.game.desktop.Space
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timer


var frameCount = 1800
var parent:Space? = null


class Populator constructor  (mySpace: Space) {
    fun start() {
        timer("Baches of 10",period = 1000){
            filepop(frameCount, frameCount+10)
            frameCount += 11
        }
    }
    init {
            parent = mySpace
        }
    }


    fun filepop(start: Int, end: Int){
        val ldrrdr = LidarReader.DefaultReader()
        var intermetidate = ldrrdr.readLidarFramesInterval("core/assets/sample.bag", start, end)

        val modelBuilder = ModelBuilder()

        val proxi = modelBuilder.createBox(
            .1f, .1f, .1f,
            Material(ColorAttribute.createDiffuse(Color.ORANGE)),
            (VertexAttributes.Usage.Position or
                    VertexAttributes.Usage.TextureCoordinates or
                    VertexAttributes.Usage.Normal.toLong().toInt()).toLong()
            )

        timer("Array Creator", period = 100){
             var array = ArrayList<ModelInstance>(53000)
             intermetidate.first().coords.forEach { f ->
                    var instance = ModelInstance(
                        proxi,
                        1f * f.coords.first,
                        1f * f.coords.second,
                        1f * f.coords.third
                    )
                    array!!.add(instance!!)
                    intermetidate.drop(0)
                    }
             parent!!.changeArray(array)
        }
    }



