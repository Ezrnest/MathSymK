package symbolic

import io.github.ezrnest.mathsymk.symbolic.BasicExprCal
import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.Node1T
import io.github.ezrnest.mathsymk.symbolic.RuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuleSetDSLTest {

    @Test
    fun testRuleSetRejectsUnboundReferenceInResult() {
        val provider = RuleSet {
            rule(ref("x"), ref("y"))
        }
        assertFailsWith<IllegalArgumentException> {
            provider.init(BasicExprCal())
        }
    }

    @Test
    fun testRuleSetSkipsNoOpRule() {
        val provider = RuleSet {
            rule(ref("x"), ref("x"))
        }
        val rules = provider.init(BasicExprCal())
        assertEquals(0, rules.size)
    }

    @Test
    fun testRuleSetRegistersValidRule() {
        val f = ESymbol("f")
        val provider = RuleSet {
            rule(ref("x"), Node1T(f, ref("x")))
        }
        val rules = provider.init(BasicExprCal())
        assertEquals(1, rules.size)
    }
}
