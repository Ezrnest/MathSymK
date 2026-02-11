package symbolic

import io.github.ezrnest.mathsymk.symbolic.ESymbol
import io.github.ezrnest.mathsymk.symbolic.NSymbol
import io.github.ezrnest.mathsymk.symbolic.Node1T
import io.github.ezrnest.mathsymk.symbolic.Node2T
import io.github.ezrnest.mathsymk.symbolic.Node3T
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeTest {

    @Test
    fun testNode1MapSymbolMapsNodeSymbolAndChildren() {
        val sx = ESymbol("x")
        val sy = ESymbol("y")
        val sf = ESymbol("f")
        val sg = ESymbol("g")

        val node = Node1T(sf, NSymbol(sx))
        val mapped = node.mapSymbol { s ->
            when (s) {
                sx -> sy
                sf -> sg
                else -> s
            }
        }
        assertEquals(Node1T(sg, NSymbol(sy)), mapped)
    }

    @Test
    fun testNode2AndNode3MapSymbolMapsNodeSymbols() {
        val sa = ESymbol("a")
        val sb = ESymbol("b")
        val sf = ESymbol("f")
        val sg = ESymbol("g")
        val sh = ESymbol("h")
        val sk = ESymbol("k")

        val n2 = Node2T(sf, NSymbol(sa), NSymbol(sb))
        val n2Mapped = n2.mapSymbol { s ->
            when (s) {
                sa -> sb
                sb -> sa
                sf -> sg
                else -> s
            }
        }
        assertEquals(Node2T(sg, NSymbol(sb), NSymbol(sa)), n2Mapped)

        val n3 = Node3T(sh, NSymbol(sa), NSymbol(sb), Node1T(sf, NSymbol(sa)))
        val n3Mapped = n3.mapSymbol { s ->
            when (s) {
                sa -> sb
                sb -> sa
                sh -> sk
                sf -> sg
                else -> s
            }
        }
        assertEquals(Node3T(sk, NSymbol(sb), NSymbol(sa), Node1T(sg, NSymbol(sb))), n3Mapped)
    }
}
