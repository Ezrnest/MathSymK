package symbolic

import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.NSymbol
import io.github.ezrnest.mathsymk.symbolic.QualifierSymbolDef
import io.github.ezrnest.mathsymk.symbolic.SymBasic
import io.github.ezrnest.mathsymk.symbolic.alg.ExprCalReal
import io.github.ezrnest.mathsymk.symbolic.alg.SymAlg
import io.github.ezrnest.mathsymk.symbolic.alg.SymSets
import io.github.ezrnest.mathsymk.symbolic.alg.alg
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExprCalCoreTest {

    @Test
    fun testTraverseCtxDepthZeroOnlyVisitsRoot() {
        val cal = ExprCalReal()
        val expr = alg { 1.e + 2.e }
        var count = 0
        cal.traverseCtx(expr, cal.context, depth = 0) { _, _ -> count++ }
        assertEquals(1, count)
    }

    @Test
    fun testVariablesOfExcludesQualifiedSymbolsWhenDefinitionRegistered() {
        val cal = ExprCalReal()
        // SUM is a qualifier symbol; register its definition so context can mark bound variables.
        cal.registerSymbol(QualifierSymbolDef(SymAlg.Symbols.SUM))

        val xSym = ESymbol("x")
        val ySym = ESymbol("y")
        val iSym = ESymbol("i")
        val x = NSymbol(xSym)
        val y = NSymbol(ySym)
        val i = NSymbol(iSym)

        val range = SymSets.intRange(alg { 1.e }, x)
        val condition = SymSets.belongs(i, range)
        val clause = alg { i + y }
        val expr = SymBasic.Instance.node3(
            SymAlg.Symbols.SUM,
            SymBasic.Instance.tuple(i),
            condition,
            clause
        )

        val vars = cal.variablesOf(expr)
        // i is bound by SUM, while x/y remain free through range/clause.
        assertEquals(setOf(x, y), vars)
    }

    @Test
    fun testExprCalRealIsEqualAndCompare() {
        val cal = ExprCalReal()
        val lhs = alg { 2.e + 3.e }
        val rhs = alg { 5.e }
        assertTrue(cal.isEqual(lhs, rhs))
        assertEquals(0, cal.compare(lhs, rhs))

        val smaller = alg { 1.e }
        val bigger = alg { 2.e }
        assertTrue(cal.compare(smaller, bigger) < 0)
    }
}
