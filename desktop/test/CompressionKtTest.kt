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


    @Test
    internal fun returnCPPTest() {
        var defendant = owner.returnCPP(1.27f,4)
        assertEquals(1.25f, defendant)
        defendant = owner.returnCPP(1.27f,4)
    }

    @Test
    internal fun wrong() {
//        assertEquals(0, 1)
    }
}