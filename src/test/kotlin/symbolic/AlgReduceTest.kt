package symbolic

import io.github.ezrnest.mathsymk.symbolic.BasicExprCal
import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.ExprCal
import io.github.ezrnest.mathsymk.symbolic.NSymbol
import io.github.ezrnest.mathsymk.symbolic.SymBasic
import io.github.ezrnest.mathsymk.symbolic.alg.ExprCalReal
import io.github.ezrnest.mathsymk.symbolic.alg.alg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AlgReduceTest {

    @Test
    fun testComputePowIntegerExponent() {
        val cal = ExprCalReal()
        val expr = alg { pow(2.e, 10.e) }
        val reduced = cal.reduce(expr)
        val expected = alg { 1024.e }
        assertTrue(cal.directEquals(expected, reduced))
    }

    @Test
    fun testComputePowZeroCases() {
        val cal = ExprCalReal()
        val zeroToPositive = cal.reduce(alg { pow(0.e, 3.e) })

        // Semantics of 0^0 are intentionally left unspecified for now.
        assertTrue(cal.directEquals(alg { 0.e }, zeroToPositive))
    }

    @Test
    fun testComputePowMinusOneHalfInForceRealThrows() {
        val cal = ExprCalReal()
        assertFailsWith<ArithmeticException> {
            cal.reduce(alg { pow((-1).e, rational(1, 2)) })
        }
    }

    @Test
    fun testComputePowMinusOneHalfInComplexModeGivesI() {
        val cal = BasicExprCal()
        val reduced = cal.reduce(alg { pow((-1).e, rational(1, 2)) })
        assertTrue(cal.directEquals(alg { 𝑖 }, reduced))
    }

    @Test
    fun testMergeProductCombinesSameBase() {
        val cal = ExprCalReal()
        val x = NSymbol(ESymbol("x"))
        val reduced = cal.reduce(alg { x * x * x })
        val expected = alg { pow(x, 3.e) }
        assertTrue(cal.directEquals(expected, reduced))
    }

    @Test
    fun testTrigSpecialAnglesAndIdentity() {
        val cal = ExprCalReal()
        val x = NSymbol(ESymbol("x"))

        val sinPi6 = cal.reduce(alg { sin(rational(1, 6) * π) })
        // cos special-angle rule currently matches rational * π form directly.
        val cosPi3 = cal.reduce(alg { cos(rational(1, 3) * π) })
        val tanPi2 = cal.reduce(alg { tan(rational(1, 2) * π) })
        val identity = cal.reduce(alg { pow(sin(x), 2.e) + pow(cos(x), 2.e) })

        assertTrue(cal.directEquals(alg { rational(1, 2) }, sinPi6))
        assertTrue(cal.directEquals(alg { rational(1, 2) }, cosPi3))
        assertEquals(SymBasic.Undefined, tanPi2)
        assertTrue(cal.directEquals(alg { 1.e }, identity))
    }
}
