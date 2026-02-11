package model

import io.github.ezrnest.mathsymk.model.Models
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntModModelTest {

    private fun mod(x: Long, n: Int): Int {
        val nL = n.toLong()
        val r = x % nL
        return if (r >= 0) r.toInt() else (r + nL).toInt()
    }

    @Test
    fun testIntModNLargeModulusOverflowSafeOps() {
        val n = Int.MAX_VALUE / 2 - 1
        val zModN = Models.intModN(n)

        val a = Int.MAX_VALUE - 1
        val b = 10
        assertEquals(mod(a.toLong() + b.toLong(), n), zModN.add(a, b))
        assertEquals(mod(a.toLong() - b.toLong(), n), zModN.subtract(a, b))

        val x = Int.MAX_VALUE - 2
        val y = Int.MAX_VALUE - 3
        assertEquals(mod(x.toLong() * y.toLong(), n), zModN.multiply(x, y))
    }

    @Test
    fun testIntModPReciprocalZeroThrowsForBothCachedAndNonCached() {
        assertFailsWith<ArithmeticException> { Models.intModP(97, cached = true).reciprocal(0) }
        assertFailsWith<ArithmeticException> { Models.intModP(97, cached = false).reciprocal(0) }
    }

    @Test
    fun testIntModPCachedRejectsNonPrime() {
        assertFailsWith<ArithmeticException> { Models.intModP(9, cached = true) }
    }
}
