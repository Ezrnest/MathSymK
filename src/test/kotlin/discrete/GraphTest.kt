package discrete

import io.github.ezrnest.mathsymk.discrete.MutableGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphTest {

    @Test
    fun testHasPathAndTraverse() {
        val g = MutableGraph<Int>()
        val v1 = g.addVertex(1)
        val v2 = g.addVertex(2)
        val v3 = g.addVertex(3)
        val v4 = g.addVertex(4)
        g.addEdge(v1, v2)
        g.addEdge(v2, v3)

        assertTrue(g.hasPath(v1, v3))
        assertFalse(g.hasPath(v3, v1))
        assertFalse(g.hasPath(v1, v4))

        val fromV1Dfs = g.dfsFrom(v1).toSet()
        val fromV1Bfs = g.bfs().toSet()
        assertEquals(setOf(v1, v2, v3), fromV1Dfs)
        assertEquals(setOf(v1, v2, v3, v4), fromV1Bfs)
    }

    @Test
    fun testTopoSortAndCycleDetection() {
        val g = MutableGraph<Int>()
        val v1 = g.addVertex(1)
        val v2 = g.addVertex(2)
        val v3 = g.addVertex(3)
        val v4 = g.addVertex(4)

        g.addEdge(v1, v2)
        g.addEdge(v1, v3)
        g.addEdge(v2, v4)
        g.addEdge(v3, v4)

        val topo = g.topoSort()
        assertNotNull(topo)
        val index = topo.withIndex().associate { it.value to it.index }
        assertTrue(index.getValue(v1) < index.getValue(v2))
        assertTrue(index.getValue(v1) < index.getValue(v3))
        assertTrue(index.getValue(v2) < index.getValue(v4))
        assertTrue(index.getValue(v3) < index.getValue(v4))

        g.addEdge(v4, v1)
        assertNull(g.topoSort())
    }

    @Test
    fun testRemoveVertexAlsoRemovesIncidentEdges() {
        val g = MutableGraph<Int>()
        val v1 = g.addVertex(1)
        val v2 = g.addVertex(2)
        val v3 = g.addVertex(3)

        g.addEdge(v1, v2)
        g.addEdge(v2, v3)
        g.addEdge(v1, v3)
        assertTrue(g.hasPath(v1, v3))

        assertTrue(g.removeVertex(v2))
        assertEquals(setOf(v1, v3), g.vertices)
        assertEquals(setOf(v3), g.neighbors(v1))
        assertEquals(setOf(v1), g.incomingNeighbors(v3))
        assertFalse(g.hasPath(v1, v2))
    }
}
