package discrete

import io.github.ezrnest.mathsymk.discrete.CombUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CombUtilsTest {

    @Test
    fun testBinomialBigInteger() {
        assertEquals("10", CombUtils.binomialB(5, 2).toString())
        assertEquals("6", CombUtils.binomialB(-3, 2).toString())
    }

    @Test
    fun testMultinomialBigInteger() {
        assertEquals("30", CombUtils.multinomialB(5, 2, 2).toString())
    }

    @Test
    fun testMultisetNumberBigInteger() {
        assertEquals("6", CombUtils.multisetNumberB(3, 2).toString())
    }

    @Test
    fun testCombinationOverflowFallback() {
        val expected = CombUtils.binomialB(66, 33).longValueExact()
        assertEquals(expected, CombUtils.combination(66, 33))
        assertFailsWith<ArithmeticException> {
            CombUtils.combination(67, 33)
        }
    }
}
