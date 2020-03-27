package org.quokka.kotlin.environment

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Vector3
import com.mygdx.game.desktop.Space
import org.quokka.kotlin.internals.LidarCoord
import org.quokka.kotlin.internals.LidarFrame
import java.util.HashMap
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt


/*
 * class containing all the methods used in the compression of the points
 * the GUI changes to the relevant values call directly the setters in this class
 * @author Robert
 */
class Compression(comprLVL: Int, gradual: Boolean, disnta: Int, owner: Space?) {

    // current compression level of the system
    var compressionLevel = comprLVL

    // boolean keeping track of if gradual compression is on or off
    var gradualCompression = gradual


    // margin for distnace from a camera for a point to have different compressionLVL
    var dfcm = disnta

    // the camera of the environment, its position is needed in the calculation of gradual compression
    var space = owner



    /**
     * point means a point in the point cloud, an object with x y z float values
     * this method is given the next LidarFrame
     * and puts points which are close enough to each other in one point
     * then gives the remaining points a suitably sized decal
     * based on the amount of points which are compressed into that point
     * all the decals are place in the objects variable which is returned
     */
    fun compressPoints(crtFrame: LidarFrame ): ArrayList<Decal>? {
        val objects = ArrayList<Decal>(15) //end result of the method

        val map = HashMap<LidarCoord, Int>()
        //map containing the coordinates as key and the number of points approximated to that point as value


        // Return null if no new frame is available
        if (crtFrame == null) {
            return null
        }

        crtFrame.coords.forEach { c ->
            var divisions = compressionLevel //level of compression
            if (gradualCompression == true && compressionLevel != 1) {
                divisions = decidDivisions(c) //has to be deiced based on distance from camera
            }
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

            val d = Decal.newDecal(.3f, .3f, space?.decalTextureRegion)
            val baseSizeofDecal = .2f

            for (i in 0..8) {
                if (map.get(k) in i * margin..(i + 1) * margin) {
                    d.setDimensions(baseSizeofDecal + i * 0.02f, baseSizeofDecal + i * 0.02f)
                }
            }
            d.setPosition(k.x, k.y, k.z)
            objects.add(d)
        }
        return objects
    }


    /**
     * This methods decied the val of compression of a point
     * depending on the distance from the camera
     * @param coord is the coordinate being checked
     * @return 1,2,3,4 number of divisions,
     * will be fed into returnCPP
     */
    fun decidDivisions(coord: LidarCoord): Int {
        var camp = Vector3(0f,0f,0f)

        camp = space?.cam?.position!!

        //calculate distance between camera and point
        val distance = distanceAB3(coord, camp)
        sqrt((coord.x - camp.x).pow(2)
                + (coord.y - camp.y).pow(2)
                + (coord.z - camp.z).pow(2))

        // distance from the camera with dfcm subtracted
        val substraction = distance - dfcm

        when (compressionLevel) { //compressionLevel is the maximum level of compression
            // 1 is least, then 4, 3 and finally 2
            1 -> return 1
            2 -> if (substraction < 0) {
                    return 1
                } else if (substraction < dfcm) {
                    return 4
                } else if (substraction < 2 * dfcm) {
                    return 3
                } else {
                    return 2
                }
            3 -> if (substraction < 0) {
                    return 1
                } else if (substraction < dfcm) {
                    return 4
                } else {
                    return 3
                }
            4 -> if (substraction < 0) {
                    return 1
                } else {
                    return 4
                }
        }

        return -1
    }



    /**
     * this methods returns the parent of a point
     * the parent of a point is a point to which the initial point is aproximated
     * @param a is the number being tested
     * @param divisions is the number of divisions meaning
     * if it is 1 then then the number is aproximated to itself
     * if it is 2 then then number is approximated to closes .5 or .0
     * if it is 3 then then number is approximated to closes .33, .66 or .0
     * if it is 5 then then number is approximated to closes .25, .5, .75 or .0
     * @return The parent of a point.
     */
    fun returnCPP(a: Float, divisions: Int): Float {
        var result = 0f
        var auxxx: Float // this will be between 0 and 1, factorial part of number a
        if (a > -1 && a < 1) {
            auxxx = a
        } else {
            auxxx = a - a.toInt()
        }
        auxxx *= sign(a) // make it always positive
        val margin: Float
        if (divisions == 1) {
            return a
        } else if (divisions == 2) {
            margin = .5f
            when (auxxx) {
                in 0f..margin/2 -> result = a.toInt() * 1f
                in margin/2..margin*3/2 -> result = a.toInt() + margin * sign(a)
                in margin*3/2..1f -> result = (a.toInt() + 1* sign(a)) * 1f

            }
        } else if (divisions == 3) {
            margin = .33f
            when (auxxx) {
                in 0f..margin/2 -> result = a.toInt() * 1f
                in margin/2..margin*3/2 -> result = a.toInt() + margin * sign(a)
                in margin*3/2..margin*5/2 -> result = a.toInt() + margin* 2 * sign(a)
                in margin*5/2 ..1f -> result = (a.toInt() + 1* sign(a)) *1f
            }

        } else if (divisions == 4) {
            margin = .25f
            when (auxxx) {
                in 0f..margin/2 -> result = a.toInt() * 1f
                in margin/2..margin*3/2 -> result = a.toInt() + margin * sign(a)
                in margin*3/2..margin*5/2 -> result = a.toInt() + margin* 2 * sign(a)
                in margin*5/2..margin*7/2 -> result = a.toInt() + margin* 3 * sign(a)
                in margin*7/2 ..1f -> result = (a.toInt() + 1* sign(a)) *1f
            }
        } else throw Error("Divisions not prepared for divisions to be $divisions  ")
        return result
    }

    //--------------Math Methods----------
    /*
    Methods for calculating the distance between two 3d
     @param a, a LidarCoord and @param b, a Vector3
     a Vector3 and another Vector3
     */
    fun distanceAB3(a: LidarCoord, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    fun distanceAB3(a: Vector3, b: Vector3): Float {
        return sqrt((a.x - b.x).pow(2)
                + (a.y - b.y).pow(2)
                + (a.z - b.z).pow(2))
    }

    /*
    Calculates the distance between two points in a 2D space
    @param x and @param y are the coordinates of the first point
    @param a and @param b are the coordinates of the second point
     */
    fun disntaceAB2(x: Float, y: Float, a: Float, b: Float): Float {
        return sqrt((x - a).pow(2) + (y - b).pow(2))
    }


    //------------------ Setters ---------------

    fun changeDFCM(dd: Int) {
        this.dfcm = dd
    }

    fun changeCompression(newcomp: Int) {
        this.compressionLevel = newcomp
    }

    fun switchGradualCompression(newset: Boolean) {
        this.gradualCompression = newset
    }
}