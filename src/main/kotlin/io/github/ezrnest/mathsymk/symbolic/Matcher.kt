package io.github.ezrnest.mathsymk.symbolic

import io.github.ezrnest.mathsymk.structure.PartialOrder
import io.github.ezrnest.mathsymk.symbolic.alg.AlgebraScope
import io.github.ezrnest.mathsymk.symbolic.alg.ComputePow
import io.github.ezrnest.mathsymk.symbolic.alg.SymAlg

//created at 2024/10/10

interface MatchResult {
    val cal: ExprCal

    val refMap: Map<ESymbol, Node>


    fun addRef(name: ESymbol, node: Node): MatchResult {
        val newMap = refMap.toMutableMap()
        newMap[name] = node
        return MatchResultImpl(cal, newMap)
    }

    fun getRef(name: ESymbol): Node? {
        return refMap[name]
    }

    companion object {

        operator fun invoke(cal: ExprCal): MatchResult {
            return MatchResultImpl(cal)
        }

        internal data class MatchResultImpl(
            override val cal: ExprCal,
            override val refMap: Map<ESymbol, Node> = emptyMap()
        ) : MatchResult{
            override fun toString(): String {
                return "MatchResult($refMap)"
            }
        }
    }
}


typealias NodeMatcher = NodeMatcherT<Node>

sealed interface NodeMatcherT<out T : Node> {

    /**
     * Tries to match the given node with the pattern represented by this matcher.
     *
     * Returns the resulting context if the node matches the pattern, or `null` otherwise.
     */
    fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult?


    /**
     * Determines whether this matcher requires a specifically determined node to match.
     */
    fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return false
    }

    /**
     * Gets the specifically determined node that *may* be matched by this matcher or
     * `null` if it can not be determined.
     *
     * That is, if `spec = matcher.getSpecific(context)` is not `null`, then
     * `node != spec` will imply `matcher.matches(node, context) == null`.
     */
    fun getSpecific(ctx: EContext, matching: MatchResult): Node? {
        return null
    }


    val refNames: Set<ESymbol>
}

interface LeafMatcher<T : Node> : NodeMatcherT<T>

interface TransparentMatcher<T : Node> : NodeMatcherT<T> {
    val matcher: NodeMatcherT<T>
}

interface BranchMatcher<T : Node> : NodeMatcherT<T> {
    val symbol: ESymbol
}

interface BranchMatcherChilded<T : Node> : BranchMatcher<T> {
    val children: List<NodeMatcherT<Node>>
}

interface BranchMatcherChildedOrdered<T : Node> : BranchMatcherChilded<T> {
    override val children: List<NodeMatcherT<Node>>
}


abstract class AbsBranchMatcher<T : Node>(final override val symbol: ESymbol) : BranchMatcher<T>

class NodeMatcher1<C : Node>(val child: NodeMatcherT<C>, symbol: ESymbol) :
    AbsBranchMatcher<Node1T<C>>(symbol), BranchMatcherChildedOrdered<Node1T<C>> {

    override val children: List<NodeMatcherT<Node>>
        get() = listOf(child)

    override val refNames: Set<ESymbol>
        get() = child.refNames

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is Node1) return null
        if (symbol != node.symbol) return null
        val subCtx = matching.cal.enterContext(node, ctx)[0]
        return child.matches(node.child, subCtx, matching)
    }

    override fun toString(): String {
        return "${symbol}($child)"
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return child.requireSpecific(ctx, matching)
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): Node1T<Node>? {
        val c = child.getSpecific(ctx, matching) ?: return null
        return Node1T(symbol, c)
    }
}

class NodeMatcher2Ordered<C1 : Node, C2 : Node>(
    val child1: NodeMatcherT<C1>, val child2: NodeMatcherT<C2>, symbol: ESymbol
) :
    AbsBranchMatcher<Node2T<C1, C2>>(symbol), BranchMatcherChildedOrdered<Node2T<C1, C2>> {
    override val children: List<NodeMatcherT<Node>>
        get() = listOf(child1, child2)

    override val refNames: Set<ESymbol> by lazy(LazyThreadSafetyMode.NONE) {
        child1.refNames + child2.refNames
    }

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is Node2) return null
        if (symbol != node.symbol) return null
        val (_,node1, node2) = node
        val (subCtx1, subCtx2) = matching.cal.enterContext(node, ctx)
        var newM = matching
        newM = child1.matches(node1, subCtx1, newM) ?: return null
        newM = child2.matches(node2, subCtx2, newM) ?: return null
        return newM
    }

    override fun toString(): String {
        return "${symbol}($child1, $child2)"
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return child1.requireSpecific(ctx, matching) && child2.requireSpecific(ctx, matching)
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): Node2? {
        val c1 = child1.getSpecific(ctx, matching) ?: return null
        val c2 = child2.getSpecific(ctx, matching) ?: return null
        return Node2T(symbol,c1, c2)
    }
}

class NodeMatcher3Ordered<C1 : Node, C2 : Node, C3 : Node>(
    val child1: NodeMatcherT<C1>, val child2: NodeMatcherT<C2>, val child3: NodeMatcherT<C3>, symbol: ESymbol
) : AbsBranchMatcher<Node3T<C1, C2, C3>>(symbol),
    BranchMatcherChildedOrdered<Node3T<C1, C2, C3>> {
    override val children: List<NodeMatcherT<Node>>
        get() = listOf(child1, child2, child3)
    override val refNames: Set<ESymbol> by lazy(LazyThreadSafetyMode.NONE) {
        child1.refNames + child2.refNames + child3.refNames
    }

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is Node3) return null
        if (symbol != node.symbol) return null
        val (_, node1, node2, node3) = node
        val (subCtx1, subCtx2, subCtx3) = matching.cal.enterContext(node, ctx)
        var newM = matching
        newM = child1.matches(node1, subCtx1, newM) ?: return null
        newM = child2.matches(node2, subCtx2, newM) ?: return null
        newM = child3.matches(node3, subCtx3, newM) ?: return null
        return newM
    }

    override fun toString(): String {
        return "${symbol}($child1, $child2, $child3)"
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return child1.requireSpecific(ctx, matching)
                && child2.requireSpecific(ctx, matching)
                && child3.requireSpecific(ctx, matching)
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): Node3? {
        val c1 = child1.getSpecific(ctx, matching) ?: return null
        val c2 = child2.getSpecific(ctx, matching) ?: return null
        val c3 = child3.getSpecific(ctx, matching) ?: return null
        return Node3T(symbol, c1, c2, c3)
    }
}

class NodeMatcherNOrdered(override val children: List<NodeMatcherT<Node>>, symbol: ESymbol) :
    AbsBranchMatcher<NodeN>(symbol), BranchMatcherChildedOrdered<NodeN> {
    override val refNames: Set<ESymbol> by lazy(LazyThreadSafetyMode.NONE) {
        children.flatMap { it.refNames }.toSet()
    }

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is NodeN) return null
        if (symbol != node.symbol) return null
        if (node.children.size != children.size) return null
        val subCtxs = matching.cal.enterContext(node, ctx)
        var newM = matching
        for (i in children.indices) {
            newM = children[i].matches(node.children[i], subCtxs[i], newM) ?: return null
        }
        return newM
    }

    override fun toString(): String {
        return "${symbol}(${children.joinToString(", ")})"
    }
}

object NothingMatcher : LeafMatcher<Node> {

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        return null
    }

    override val refNames: Set<ESymbol> get() = emptySet()

    override fun toString(): String {
        return "Nothing"
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return true
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): Node? {
        return SymBasic.UNDEFINED
    }
}

object AnyMatcher : LeafMatcher<Node> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        return matching
    }

    override val refNames: Set<ESymbol> get() = emptySet()

    override fun toString(): String {
        return "Any"
    }
}

object AnyRationalMatcher : LeafMatcher<NRational> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node is NRational) return matching
        return null
    }

    override val refNames: Set<ESymbol> get() = emptySet()


    override fun toString(): String {
        return "AnyRational"
    }
}

object AnySymbolMatcher : LeafMatcher<NSymbol> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node is NSymbol) return matching
        return null
    }

    override val refNames: Set<ESymbol> get() = emptySet()

    override fun toString(): String {
        return "AnySymbol"
    }
}


class MatcherRef(val name: ESymbol) : LeafMatcher<Node> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        val ref = matching.refMap[name]
        if (ref == null) {
            return matching.addRef(name, node)
        }
        return if (ref == node) matching else null
    }

    override val refNames: Set<ESymbol> get() = setOf(name)

    override fun toString(): String {
        return "Ref($name)"
    }
}

class MatcherNamed<T : Node>(override val matcher: NodeMatcherT<T>, val name: ESymbol) : TransparentMatcher<T> {
    override val refNames: Set<ESymbol>
        get() = matcher.refNames + name

    override fun toString(): String {
        return "Named(name=$name, $matcher)"
    }

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        val ref = matching.refMap[name]
        if (ref != null && ref != node) return null
        val res = matcher.matches(node, ctx, matching) ?: return null
        return if (ref == null) {
            res.addRef(name, node)
        } else {
            res
        }
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        if (name in matching.refMap) {
            return true // requires the specific node to match
        }
        return matcher.requireSpecific(ctx, matching)
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): Node? {
        if (name in matching.refMap) {
            return matching.refMap[name]
        }
        return matcher.getSpecific(ctx, matching)
    }
}


class MatcherWithPrecondition<T : Node>(
    override val matcher: NodeMatcherT<T>, val pre: (Node, EContext, MatchResult) -> Boolean
) : TransparentMatcher<T> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        return if (pre(node, ctx, matching)) matcher.matches(node, ctx, matching) else null
    }

    override val refNames: Set<ESymbol>
        get() = matcher.refNames
}

class MatcherWithPostcondition<T : Node>(
    override val matcher: NodeMatcherT<T>, val post: (Node, EContext, MatchResult) -> Boolean
) : TransparentMatcher<T> {
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        return matcher.matches(node, ctx, matching)?.takeIf { post(node, ctx, it) }
    }

    override val refNames: Set<ESymbol>
        get() = matcher.refNames
}

class MatcherWithPostConditionNode<T : Node>(
    override val matcher: NodeMatcherT<T>, val condition: Node,
//    val ref
) : TransparentMatcher<T> {
    override val refNames: Set<ESymbol>
        get() = matcher.refNames

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        val newM = matcher.matches(node, ctx, matching) ?: return null
        val satisfied = NodeScopeMatcher.testConditionRef(condition, ctx, newM)
        if (satisfied) return newM
        return null
    }
}

class FixedNodeMatcher<T : Node>(val target: T) : LeafMatcher<T> {
    // TODO
    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        return if (node == target) matching else null
    }

    override val refNames: Set<ESymbol> get() = emptySet()


    override fun toString(): String {
        return target.plainToString()
    }

    override fun requireSpecific(ctx: EContext, matching: MatchResult): Boolean {
        return true
    }

    override fun getSpecific(ctx: EContext, matching: MatchResult): T {
        return target
    }
}

class LeafMatcherFixSym(symbol: ESymbol) :
    AbsBranchMatcher<NodeChilded>(symbol), LeafMatcher<NodeChilded> {

    override val refNames: Set<ESymbol>
        get() = emptySet()

    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is NodeChilded) return null
        if (node.symbol != symbol) return null
        return matching
    }
}


class NodeMatcherNPO(
    children: List<NodeMatcherT<Node>>, symbol: ESymbol,
    var remMatcher: NodeMatcherT<Node> = NothingMatcher
) : AbsBranchMatcher<NodeN>(symbol), BranchMatcherChilded<NodeN> {

    override fun toString(): String {
        return "${symbol}(${childrenChains.flatten().joinToString(", ")})"
    }


    val childrenChains: List<List<NodeMatcherT<*>>> =
        PartialOrder.chainDecomp(children, MatcherPartialOrder).sortedWith(ChainPreference)

    val totalChildren: Int = children.size

    override val children: List<NodeMatcherT<Node>>
        get() = childrenChains.flatten()

    private val requireFullMatch: Boolean
        get() = childrenChains.size == 1 && remMatcher === NothingMatcher

    override val refNames: Set<ESymbol> by lazy(LazyThreadSafetyMode.NONE) {
        children.flatMap { it.refNames }.toSet()
    }

    private fun match0(
        chainIndex: Int, pos: Int, ctx: MatchResult,
        children: List<Node>, subCtxs: List<EContext>,
        matched: BooleanArray, cur: Int
    ): MatchResult? {
        val curChain = childrenChains[chainIndex]
        val curChainRem = curChain.size - pos
        val matcher = curChain[pos]

        for (i in cur..(children.size - curChainRem)) {
            if (matched[i]) continue
            val child = children[i]
            val subCtx = subCtxs[i]
            if (matcher.matches(child, subCtx, ctx) != null) {
                matched[i] = true
                if (pos == curChain.size - 1) {
                    if (chainIndex == childrenChains.size - 1) {
                        return ctx
                    }
                    val sub = match0(chainIndex + 1, 0, ctx, children, subCtxs, matched, 0)
                    if (sub != null) return sub
                } else {
                    val sub = match0(chainIndex, pos + 1, ctx, children, subCtxs, matched, i + 1)
                    if (sub != null) return sub
                }
                matched[i] = false
            }
        }
        return null
    }

    private fun fullMatchOrdered(
        node: NodeN, matchers: List<NodeMatcher>, ctx: EContext, matchResult: MatchResult
    ): MatchResult? {
        val children = node.children
        if (children.size != matchers.size) return null
        val subCtxs = matchResult.cal.enterContext(node, ctx)
        var newM = matchResult
        for (i in children.indices) {
            val child = children[i]
            val subCtx = subCtxs[i]
            val matcher = matchers[i]
            newM = matcher.matches(child, subCtx, newM) ?: return null
        }
        return newM
    }


    override fun matches(node: Node, ctx: EContext, matching: MatchResult): MatchResult? {
        if (node !is NodeN) return null
        if (symbol != node.symbol) return null
        if (requireFullMatch) {
            return fullMatchOrdered(node, childrenChains[0], ctx, matching)
        }
        val nodeChildren = node.children
        val subCtxs = matching.cal.enterContext(node, ctx)
        val nodeCount = nodeChildren.size
        if (nodeCount < totalChildren) return null
        if (nodeCount > totalChildren && remMatcher === NothingMatcher) return null
        val matched = BooleanArray(nodeChildren.size)
        val newM = match0(0, 0, matching, nodeChildren, subCtxs, matched, 0) ?: return null
        if (nodeCount == totalChildren) {
            return newM
        }
        val remChildren = ArrayList<Node>(nodeCount - totalChildren)
        nodeChildren.filterIndexedTo(remChildren) { index, _ -> !matched[index] }
        val remNode = NodeN(symbol, remChildren)
        return remMatcher.matches(remNode, ctx, newM)
    }

    companion object {
        private fun NodeMatcherT<*>.unwrap(): NodeMatcherT<Node> {
            var m = this
            while (m is TransparentMatcher<*>) {
                m = m.matcher
            }
            return m
        }
    }

    object ChainPreference : Comparator<List<NodeMatcherT<*>>> {

        fun compareChildren(o1: BranchMatcher<*>, o2: BranchMatcher<*>): Int {
            if (o1 is BranchMatcherChildedOrdered && o2 is BranchMatcherChildedOrdered) {
                val c = o1.children.size - o2.children.size
                if (c != 0) return c
                for (i in o1.children.indices) {
                    val c = compareNode(o1.children[i], o2.children[i])
                    if (c != 0) return c
                }
            }
            return 0
        }

        fun compareNode(o1: NodeMatcherT<*>, o2: NodeMatcherT<*>): Int {
            val a = o1.unwrap()
            val b = o2.unwrap()
            if (a is BranchMatcher<*> && b is BranchMatcher<*>) {
                val c = a.symbol.compareTo(b.symbol)
                if (c != 0) return c
                return compareChildren(a, b)
            }
            if (a is BranchMatcher) return -1
            if (b is BranchMatcher) return 1

            return 0
        }

        override fun compare(o1: List<NodeMatcherT<*>>, o2: List<NodeMatcherT<*>>): Int {
            if (o1.size != o2.size) return o2.size - o1.size
            for (i in o1.indices) {
                val c = compareNode(o1[i], o2[i])
                if (c != 0) return c
            }
            return 0
        }


    }

    /**
     * Defines a partial order on NodeMatcher such that `x < y` guarantees `x.match < y.match` in node's order.
     */
    object MatcherPartialOrder : PartialOrder<NodeMatcherT<*>> {

        fun compareChildren(o1: BranchMatcher<*>, o2: BranchMatcher<*>): PartialOrder.Result {
            if (o1 is BranchMatcherChildedOrdered && o2 is BranchMatcherChildedOrdered) {
                val c = o1.children.size - o2.children.size
                if (c != 0) return PartialOrder.Result.ofInt(c)
                for (i in o1.children.indices) {
                    val c = compare(o1.children[i], o2.children[i])
                    if (c != PartialOrder.Result.EQUAL) return c
                }
                return PartialOrder.Result.EQUAL
            }
            return PartialOrder.Result.INCOMPARABLE
        }


        override fun compare(o1: NodeMatcherT<*>, o2: NodeMatcherT<*>): PartialOrder.Result {
            val a = o1.unwrap()
            val b = o2.unwrap()
            if (a is BranchMatcher<*> && b is BranchMatcher<*>) {
                val c = a.symbol.compareTo(b.symbol)
                if (c != 0) return PartialOrder.Result.ofInt(c)
                return compareChildren(a, b)
            }
            return PartialOrder.Result.INCOMPARABLE
        }
    }


}


interface MatcherBuilderScope {
    val x: MatcherRef get() = ref("x")
    val y: MatcherRef get() = ref("y")
    val z: MatcherRef get() = ref("z")


    val String.ref: MatcherRef get() = ref(this)
//    val String.s: NodeMatcherT<NSymbol> get() = symbol(NSymbol(this))

    fun ref(name: String): MatcherRef {
        return MatcherRef(ESymbol(name))
    }

    fun symbol(name: ESymbol): NodeMatcherT<NSymbol> {
        return symbol(NSymbol(name))
    }

    fun symbol(s: NSymbol): NodeMatcherT<NSymbol> {
        return FixedNodeMatcher(s)
    }

    fun <T : Node> node(n: T): FixedNodeMatcher<T> {
        return FixedNodeMatcher(n)
    }
}

interface MatcherScopeAlg : MatcherBuilderScope {

    fun <T : Node, S : Node> pow(base: NodeMatcherT<T>, exp: NodeMatcherT<S>): NodeMatcherT<Node2T<T, S>> {
        return NodeMatcher2Ordered(base, exp, SymAlg.Symbols.POW)
    }

    fun <T : Node> exp(x: NodeMatcherT<T>): NodeMatcherT<Node2T<NSymbol, T>> {
        return NodeMatcher2Ordered(NATURAL_E, x, SymAlg.Symbols.POW)
    }

    fun <T : Node> sin(x: NodeMatcherT<T>): NodeMatcherT<Node1T<T>> {
        return NodeMatcher1(x, SymAlg.Symbols.F1_SIN)
    }

    fun <T : Node> cos(x: NodeMatcherT<T>): NodeMatcherT<Node1T<T>> {
        return NodeMatcher1(x, SymAlg.Symbols.F1_COS)
    }

    fun <T : Node> tan(x: NodeMatcherT<T>): NodeMatcherT<Node1T<T>> {
        return NodeMatcher1(x, SymAlg.Symbols.F1_TAN)
    }


    val any: NodeMatcherT<Node> get() = AnyMatcher

    val NATURAL_E get() = symbol(SymAlg.NATURAL_E)
    val π: NodeMatcherT<NSymbol> get() = symbol(SymAlg.PI)


    val integer: NodeMatcherT<NRational> get() = AnyRationalMatcher

    val rational: NodeMatcherT<NRational> get() = AnyRationalMatcher


    val Int.e: NodeMatcherT<NRational> get() = node(SymAlg.Int(this))


    private fun flatten(children: List<NodeMatcherT<Node>>, sig: ESymbol): NodeMatcher {
        // flatten the children
        val newChildren = ArrayList<NodeMatcherT<Node>>(children.size)
        for (c in children) {
            if (c is NodeMatcherNOrdered && c.symbol == sig) {
                newChildren.addAll(c.children)
            } else if (c is NodeMatcherNPO && c.symbol == sig) {
                newChildren.addAll(c.childrenChains[0])
            } else {
                newChildren.add(c)
            }
        }
        return NodeMatcherNPO(newChildren, sig)
    }

    operator fun NodeMatcherT<Node>.times(other: NodeMatcher): NodeMatcher {
        return flatten(listOf(this, other), SymAlg.Symbols.MUL)
    }

    operator fun NodeMatcherT<Node>.plus(other: NodeMatcher): NodeMatcher {
        return flatten(listOf(this, other), SymAlg.Symbols.ADD)
    }


    fun <T : Node> NodeMatcherT<T>.named(name: ESymbol): NodeMatcherT<T> {
        return MatcherNamed(this, name)
    }

    fun <T : Node> NodeMatcherT<T>.named(ref: MatcherRef): NodeMatcherT<T> {
        return MatcherNamed(this, ref.name)
    }

    fun <T : Node> NodeMatcherT<T>.also(postCond: NodeScopeMatched.() -> Boolean): NodeMatcherT<T> {
        return MatcherWithPostcondition(this) { node, ctx, matching ->
            NodeScopeMatched(ctx, matching).postCond()
        }
    }


    companion object : MatcherScopeAlg
}

fun <T : Node> buildMatcher(action: MatcherScopeAlg.() -> NodeMatcherT<T>): NodeMatcherT<T> {
//    TODO()
    return action(MatcherScopeAlg)
}


interface INodeScopeReferring : NodeScope {

    val referenceMap: MutableMap<String, ESymbol>
    val declaredRefs: MutableSet<ESymbol>


    fun ref(name: String): Node {
        val sym = referenceMap.getOrPut(name) {
            ESymbol(name).also { declaredRefs.add(it) }
        }
        return NSymbol(sym)
    }
}

interface NodeScopeMatcher : INodeScopeReferring, NodeScopeWithPredefined {
    override val x: Node get() = ref("x")
    override val y: Node get() = ref("y")
    override val z: Node get() = ref("z")
    override val w: Node get() = ref("w")
    override val a: Node get() = ref("a")
    override val b: Node get() = ref("b")
    override val c: Node get() = ref("c")

    val String.ref get() = ref(this)

    fun Node.named(name: String): Node {
        return Node2T(F2_Named,this, ref(name))
    }

    fun Node.where(clause: Node): Node {
        return Node2T(F2_Where, this, clause)
    }

    fun Node.where(clauseBuilder: () -> Node): Node {
        return Node2T(F2_Where, this, clauseBuilder())
    }

    companion object {


        private class NodeScopeMatcherImpl(
            context: EContext,
        ) : AbstractNodeScopeMatcher(context), NodeScopeMatcher

        operator fun invoke(context: EContext): NodeScopeMatcher = NodeScopeMatcherImpl(context)

        val F2_Named = ESymbol("Named")
        val F2_Where = ESymbol("Where")

        const val MatcherSymbolPrefix = "_"

        private fun NodeScopeMatcher.buildSymbol(node: NSymbol): NodeMatcher {
            val sym = node.symbol
            if (sym in declaredRefs) {
                // TODO: add conditioned symbol
                return MatcherRef(sym)
            }
            return FixedNodeMatcher(node)
        }

        private fun NodeScopeMatcher.buildNamed(node: Node, cal: ExprCal): NodeMatcher {
            require(node is Node2)
            val child = buildMatcher0(node.first, cal)
            val name = (node.second as NSymbol).symbol
            return MatcherNamed(child, name)
        }

        private fun NodeScopeMatcher.buildWhere(node: Node, cal: ExprCal): NodeMatcher {
            require(node is Node2)
            val child = buildMatcher0(node.first, cal)
            val clauseRef = node.second
            return MatcherWithPostConditionNode(child, clauseRef)
        }

        private fun NodeScopeMatcher.buildMatcher0(node: Node, cal: ExprCal): NodeMatcher {
            if (node is NodeChilded) {
                when (node.symbol) {
                    F2_Named -> {
                        return buildNamed(node, cal)
                    }

                    F2_Where -> {
                        return buildWhere(node, cal)
                    }
                }
            }
            if (node is NodeChilded) {
                val def = cal.getDefinition(node.symbol)
                if(def != null){
                    declaredRefs.addAll(def.qualifiedVariables(node))
                }
            }
            when (node) {
                is NSymbol -> return buildSymbol(node)
                is LeafNode -> return FixedNodeMatcher(node)
                is Node1 -> {
                    val child = buildMatcher0(node.child, cal)
                    return NodeMatcher1(child, node.symbol)
                }

                is Node2 -> {
                    val left = buildMatcher0(node.first, cal)
                    val right = buildMatcher0(node.second, cal)
                    return NodeMatcher2Ordered(left, right, node.symbol)
                }

                is Node3 -> {
                    val first = buildMatcher0(node.first, cal)
                    val second = buildMatcher0(node.second, cal)
                    val third = buildMatcher0(node.third, cal)
                    return NodeMatcher3Ordered(first, second, third, node.symbol)
                }

                is NodeChilded -> {
                    val children = node.children.map { buildMatcher0(it, cal) }
                    if (cal.isCommutative(node.symbol)) {
                        return NodeMatcherNPO(children, node.symbol)
                    }
                    return NodeMatcherNOrdered(children, node.symbol)
                }

//                else -> throw IllegalArgumentException("Unknown node type: ${node::class.simpleName}")
            }
        }

        fun buildMatcher(scope: NodeScopeMatcher, node: Node, cal: ExprCal): NodeMatcher {
            return scope.buildMatcher0(node, cal)
        }

        fun warpPartialMatcherReplace(
            matcher: NodeMatcher, rep: RepBuilder, description: String,
            maxDepth: Int = Int.MAX_VALUE
        ): MatcherReplaceRule {
            if (matcher is NodeMatcherNPO && matcher.remMatcher is NothingMatcher) {
                val sym = matcher.symbol
                val remName = ESymbol("rem${matcher.symbol.name}")
                val rem = MatcherRef(remName)
                matcher.remMatcher = rem
                val replacement: RepBuilder = {
                    TODO()
//                    rep()?.let { sub ->
//                        if (!hasRef(remName)) {
//                            sub
//                        } else {
//                            NodeN(sym, listOf(sub, ref(remName)))
//                        }
//                    }
                }
                return MatcherReplaceRule(matcher, replacement, description, maxDepth)
            }
            return MatcherReplaceRule(matcher, rep, description, maxDepth)
        }


        fun substituteIn(nodeRef: Node, rootCtx: EContext, matching: MatchResult): Node {
            val cal = matching.cal
            return cal.substitute(nodeRef, rootCtx) { node, ctx ->
                if (node !is NSymbol) return@substitute null
                val name = node.symbol.name
                if (!name.startsWith(MatcherSymbolPrefix)) {
                    return@substitute null
                }
                val ref = matching.getRef(name)
                    ?: throw IllegalArgumentException("No reference found for [$name]")
                return@substitute ref
            }
        }

        fun testConditionRef(condRef: Node, ctx: EContext, matching: MatchResult): Boolean {
            val reifiedCond = substituteIn(condRef, ctx, matching)
            val cal = matching.cal
            return cal.isSatisfied(ctx, reifiedCond)
        }
    }
}

abstract class AbstractNodeScopeMatcher(context: EContext) : AbstractNodeScope(context), INodeScopeReferring {
    final override val referenceMap: MutableMap<String, ESymbol> = mutableMapOf()
    final override val declaredRefs: MutableSet<ESymbol> = mutableSetOf()
}

fun buildMatcherExpr(cal: ExprCal, action: NodeScopeMatcher.() -> Node): NodeMatcher {
    val scope = NodeScopeMatcher(cal.context)
    val node = cal.reduce(scope.action())
    println(node.plainToString())
    return NodeScopeMatcher.buildMatcher(scope, node, cal)
}


fun main() {
    val cal = TestExprCal
//    val matcher = buildMatcherExpr(cal) {
//        alg {
//            x and all(x) {
//                x gtr ZERO
//            }
//        }
//    }
//    val node = buildAlg {
//        x and all(y) {
//            x gtr y
//        }
//    }
//    println(matcher.matches(node, cal.context, MatchResult(cal)))
    val dispatcher = TreeDispatcher<Int>()
    with(MatcherScopeAlg) {
//        dispatcher.register(pow(rational, rational), 1)
        dispatcher.register(ComputePow.matcher, 1)
    }
    dispatcher.printDispatchTree()
    with(AlgebraScope(EmptyEContext)) {
        dispatcher.dispatch(pow(2.e, 3.e)) {
            println(it)
        }
    }

}
/*


    with(MatcherBuilderScope) {

        dispatcher.register(integer, 1)
        dispatcher.register(AnyMatcher, 2)
//        val m1 =
        dispatcher.register(pow(pow(π, y), rational), 3)
//        val m2 = pow(sin(x), rational)
//        dispatcher.register(m2)
//        val m3 = pow(pow(rational, rational), rational)
//        dispatcher.register(m3)
//        val m4 = pow(rational, rational)
//        dispatcher.register(m4)
//        val m5 = pow(sin(sin(sin(x))), x)
//        dispatcher.register(m5)

        val m1 = pow(sin(x), 2.e)
        val m2 = pow(cos(x), 2.e)
        val mat = NodeMatcherNPO(listOf(m1, m2), NodeSig.ADD, ref("rem"))
        mat.childrenChains.forEach { println(it) }
//        dispatcher.register(mat)
        dispatcher.register(mat, 4)
        dispatcher.register(m1, 5)
        dispatcher.register(m2, 6)
        dispatcher.printDispatchTree()
    }
//    TestExprContext.dispatcher.printDispatchTree()
//    val expr1 = with(NodeBuilderScope) {
//        pow(sin(x), 2.e) + pow(cos(x), 2.e) + x + sin(x)
//    }.let {
//        TestExprContext.simplifyFull(it)
////        it
//    }
//    println(expr1.plainToString())
//    println("Dispatched To")
//    dispatcher.dispatch(expr1) {
//        val ctx = MutableMatchContextImpl()
//        val matches = it.matches(expr1, ctx) != null
//        println("$it")
//        if (matches) {
//            val mapStr = ctx.refMap.entries.joinToString("; ") { "${it.key} = ${it.value.plainToString()}" }
//            println("Matched: {$mapStr}")
//        } else {
//            println("Not Matched")
//        }
//    }
 */