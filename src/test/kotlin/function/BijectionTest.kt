package function

import io.github.ezrnest.mathsymk.function.Bijection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class BijectionTest {

    private val shift = object : Bijection<Int, Int> {
        override fun apply(x: Int): Int = x + 1
        override fun invert(y: Int): Int = y - 1
    }

    @Test
    fun testInverseRoundTrip() {
        val inv = shift.inverse()
        assertEquals(6, shift.apply(5))
        assertEquals(5, inv.apply(6))
        assertSame(shift, inv.inverse())
    }

    @Test
    fun testApplyAndInvertAreConsistent() {
        for (x in -10..10) {
            val y = shift.apply(x)
            assertEquals(x, shift.invert(y))
            assertEquals(y, shift.inverse().invert(x))
        }
    }
}
