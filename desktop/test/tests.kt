import org.junit.Assert.*
import org.junit.Test

class TestExample {
    @Test
    internal fun correct() {
        assertEquals(1, 1)
    }

    @Test
    internal fun wrong() {
        assertNotEquals(0, 1)
    }
}