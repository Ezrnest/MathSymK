package symbolic

import io.github.ezrnest.mathsymk.symbolic.AnyMatcher
import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.FixedNodeMatcher
import io.github.ezrnest.mathsymk.symbolic.NSymbol
import io.github.ezrnest.mathsymk.symbolic.Node1T
import io.github.ezrnest.mathsymk.symbolic.NodeMatcher1
import io.github.ezrnest.mathsymk.symbolic.TreeDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeDispatcherTest {

    @Test
    fun testDispatchFindsFixedAndWildcardMatchers() {
        val symX = ESymbol("x")
        val x = NSymbol(symX)
        val dispatcher = TreeDispatcher<String>()
        dispatcher.register(FixedNodeMatcher(x), "fixed-x")
        dispatcher.register(AnyMatcher, "wildcard")

        val result = dispatcher.dispatchToList(x).toSet()
        assertEquals(setOf("fixed-x", "wildcard"), result)
    }

    @Test
    fun testDispatchUntilSupportsEarlyStopPredicate() {
        val symX = ESymbol("x")
        val x = NSymbol(symX)
        val dispatcher = TreeDispatcher<String>()
        dispatcher.register(AnyMatcher, "wildcard")
        dispatcher.register(FixedNodeMatcher(x), "fixed-x")

        val hit = dispatcher.dispatchUntil(x) { it == "fixed-x" }
        assertEquals("fixed-x", hit)
    }

    @Test
    fun testDispatchNoBranchMatchForDifferentSymbol() {
        val x = NSymbol(ESymbol("x"))
        val f = ESymbol("f")
        val g = ESymbol("g")
        val nodeF = Node1T(f, x)
        val nodeG = Node1T(g, x)
        val dispatcher = TreeDispatcher<String>()
        dispatcher.register(NodeMatcher1(AnyMatcher, f), "f-branch")

        val result = dispatcher.dispatchToList(nodeG)
        assertTrue(result.isEmpty())
        assertEquals(listOf("f-branch"), dispatcher.dispatchToList(nodeF))
    }
}
