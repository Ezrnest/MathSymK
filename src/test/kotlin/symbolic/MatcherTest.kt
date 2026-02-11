package symbolic

import io.github.ezrnest.mathsymk.symbolic.BasicExprCal
import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.MatcherRef
import io.github.ezrnest.mathsymk.symbolic.NSymbol
import io.github.ezrnest.mathsymk.symbolic.Node2T
import io.github.ezrnest.mathsymk.symbolic.NodeMatcher2Ordered
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MatcherTest {

    @Test
    fun testMatcherRefRequiresConsistentBinding() {
        val cal = BasicExprCal()
        val symF = ESymbol("f")
        val ref = ESymbol("r")
        val symX = ESymbol("x")
        val symY = ESymbol("y")

        val matcher = NodeMatcher2Ordered(MatcherRef(ref), MatcherRef(ref), symF)

        val ok = Node2T(symF, NSymbol(symX), NSymbol(symX))
        val bad = Node2T(symF, NSymbol(symX), NSymbol(symY))

        assertNotNull(matcher.matches(ok, cal))
        assertNull(matcher.matches(bad, cal))
    }
}
