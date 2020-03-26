import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestExample {
    @Test
    internal fun correct() {
        Assertions.assertEquals(1, 1)
    }

    @Test
    internal fun wrong() {
        Assertions.assertEquals(0, 1)
    }
}