import com.badlogic.gdx.math.Vector3
import org.junit.Assert.*
import org.junit.Test
import org.quokka.kotlin.environment.Compression
import java.util.*

class CompressionKtTest {

    val owner = Compression(1, false, 15,null)

    val z = Vector3(0f,0f,1f)
    val zn = Vector3(0f,0f,-1f)
    val x = Vector3(1f,0f,0f)
    val xn = Vector3(-1f,0f,0f)
    val y = Vector3(0f,1f,0f)
    val yn = Vector3(0f,-1f,0f)

    val second = 0.5f
    val third = 0.33f
    val forth = 0.25f



    /**
     * this tests the some numbers which are supposed
     * to be approximated downwards
     * e.g. 1.30 becomes 1.25 not 1.5
     *          (example has division 4)
     */
    @Test
    internal fun downCase() {

        // tests .01 and 0.51
        for ( i in 0..1){
            assertEquals(i * second ,
                    owner.returnCPP(
                            (i * second + 0.01f),
                            2))
        }

        // tests 0.32, 0.65 and 0.98
        for ( i in 1..3){
            assertEquals(i * third ,
                    owner.returnCPP(
                            (i * third - 0.01f),
                            3))
        }

        // tests 0.01,0.26, 0.51 and 0.76
        for ( i in 0..3){
            assertEquals(i * forth ,
                    owner.returnCPP(
                            (i * forth + 0.01f),
                            4))
        }
    }


    /**
     * this tests the some numbers which are supposed
     * to be approximated upwards
     * e.g. 1.4 becomes 1.5 not 1.25
     *          (example has division 4)
     */
    @Test
    internal fun upCase() {

        // tests .26 and .76
        for ( i in 0..1){
            assertEquals((i+1) * second ,
                    owner.returnCPP(
                            (i * second + 0.01f +  second/2),
                            2))
        }

        //tests .175, .505 and .835
        for ( i in 0..2){
            assertEquals((i+1) * third ,
                    owner.returnCPP(
                            (i * third + 0.01f + third/2 ),
                            3))
        }

        //tests .135, .385, .635 and .885
        for ( i in 0..3){
            assertEquals((i+1) * forth ,
                    owner.returnCPP(
                            (i * forth + 0.01f + forth/2),
                            4))
        }
    }

    /**
     * this tests the some negative numbers which are
     * supposed to be approximated upwards
     * e.g. -1.30 becomes -1.25 not -1.5
     *          (example has division 4)
     */
    @Test
    internal fun upCaseNeg() {

        //tests -0.49 and .01
        for ( i in -1..0){
            assertEquals(i * second ,
                    owner.returnCPP(
                            (i * second + 0.01f),
                            2))
        }

        //tests -0.98, -0.65 and -0.32
        for ( i in -3..-1){
            assertEquals(i * third ,
                    owner.returnCPP(
                            (i * third + 0.01f),
                            3))
        }

        //tests -0.74, -0.49, -0.24 and 0.01
        for ( i in -3..0){
            assertEquals(i * forth ,
                    owner.returnCPP(
                            (i * forth + 0.01f),
                            4))
        }
    }

    /**
     * this tests the some negative numbers which are
     * supposed to be approximated upwards
     * e.g. -1.30 becomes -1.25 not -1.5
     *          (example has division 4)
     */
    @Test
    internal fun downCaseNeg() {

        // tests .26 and .76
        for ( i in -1..0){
            assertEquals((i-1) * second ,
                    owner.returnCPP(
                            (i * second - 0.01f -  second/2),
                            2))
        }

        //tests -0.835, -0.505, -0.175 
        println(owner.returnCPP(-1.1f,3))
        assertEquals(-1.33f ,
                owner.returnCPP(
                        -1.25f,
                        3))
        for ( i in -2..0){
            println(i * third - 0.01f - third/2)
            assertEquals((i-1) * third ,
                    owner.returnCPP(
                            (i * third - 0.01f - third/2 ),
                            3))
        }

        //tests .135, .385, .635 and .885
        for ( i in -3..0){
            assertEquals((i-1) * forth ,
                    owner.returnCPP(
                            (i * forth - 0.01f - forth/2),
                            4))
        }
    }
}