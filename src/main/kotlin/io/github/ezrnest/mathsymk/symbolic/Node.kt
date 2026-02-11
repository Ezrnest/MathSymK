package io.github.ezrnest.mathsymk.symbolic
// created at 2024/10/01
import io.github.ezrnest.mathsymk.model.BigFrac
import io.github.ezrnest.mathsymk.util.all2
import java.math.BigInteger

class ESymbol(
    /**
     * The display name of the symbol.
     */
    val name: String
) : Comparable<ESymbol> {
    override fun toString(): String {
        return if (displayHash) {
            // use first four digits of the hash code
            "$name@${hashCode().toString(10).take(2)}"
        } else name
    }

    override fun compareTo(other: ESymbol): Int {
        val nameComp = name.compareTo(other.name)
        if (nameComp != 0) return nameComp
        return hashCode().compareTo(other.hashCode())
    }

    companion object {
        var displayHash = false
    }
}

sealed interface Node {
    var meta: Map<TypedKey<*>, Any?>


    fun plainToString(): String

    fun treeToString(): String {
        return treeTo(StringBuilder()).toString()
    }

    fun <A : Appendable> treeTo(builder: A, level: Int = 0, indent: String = ""): A


    fun traverse(depth: Int = Int.MAX_VALUE, action: (Node) -> Unit)

    fun allSymbols() : Set<ESymbol> {
        val res = mutableSetOf<ESymbol>()
        traverse { node ->
            if (node is SymbolNode) {
                res.add(node.symbol)
            }
        }
        return res
    }

    /**
     * Check if the tree contains any node that satisfies the [action].
     */
    fun recurAny(depth: Int = Int.MAX_VALUE, action: (Node) -> Boolean): Boolean

    fun traversePostOrder(depth: Int = Int.MAX_VALUE, action: (Node) -> Unit)

    fun traverseLeveled(depth: Int = Int.MAX_VALUE, level: Int = 0, action: (Node, Int) -> Unit)

    fun traversePostOrderLeveled(depth: Int = Int.MAX_VALUE, level: Int = 0, action: (Node, Int) -> Unit)

    fun recurMap(depth: Int = Int.MAX_VALUE, action: (Node) -> Node): Node


    /**
     * Replacing every occurrence of [src] with [dest] in the tree.
     */
    fun replace(src: Node, dest: Node): Node {
        return recurMap { if (it == src) dest else it }
    }

    /**
     * Check if the tree contains any node with the symbol [symbol].
     */
    fun containSymbol(symbol: ESymbol): Boolean {
        return recurAny { node ->
            node is SymbolNode && node.symbol == symbol
        }
    }

    /**
     * Replacing every occurrence of the symbol [src] with [dest] in the tree, including the symbol of the node.
     */
    fun replaceSymbol(src: NSymbol, dest: NSymbol): Node {
        return mapSymbol { if (it == src.symbol) dest.symbol else it }
    }

    fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node
    // a bit more efficient than the general replace method

    fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node


    fun deepEquals(other: Node): Boolean


    companion object {


    }
}


sealed class AbstractNode : Node {
    override var meta: Map<TypedKey<*>, Any?> = emptyMap()

    override fun toString(): String {
        return plainToString()
    }
}

sealed interface SymbolNode : Node {
    val symbol: ESymbol
}

sealed interface LeafNode : Node {

    override fun traverse(depth: Int, action: (Node) -> Unit) {
        action(this)
    }

    override fun traversePostOrder(depth: Int, action: (Node) -> Unit) {
        action(this)
    }

    override fun traverseLeveled(depth: Int, level: Int, action: (Node, Int) -> Unit) {
        action(this, level)
    }

    override fun traversePostOrderLeveled(depth: Int, level: Int, action: (Node, Int) -> Unit) {
        action(this, level)
    }

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        return action(this)
    }

    override fun <A : Appendable> treeTo(builder: A, level: Int, indent: String): A {
        builder.append(indent).append(plainToString()).append(";  ").append(meta.toString()).appendLine()
        return builder
    }


    override fun recurAny(depth: Int, action: (Node) -> Boolean): Boolean {
        return action(this)
    }
}

data class NRational(val value: BigFrac) : AbstractNode(), LeafNode {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NRational) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is NRational) return false
        return value == other.value
    }


    override fun plainToString(): String {
        val (nume, deno) = value
        if (deno == BigInteger.ONE) return nume.toString()
        return "$nume/$deno"
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        return this
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        return this
    }
}


data class NSymbol(
    override val symbol: ESymbol
) : AbstractNode(), LeafNode, SymbolNode {

    override fun toString(): String {
        return "Symbol($symbol)"
    }

    override fun plainToString(): String {
        return symbol.toString()
    }

    override fun deepEquals(other: Node): Boolean {
        return this === other || other is NSymbol && symbol == other.symbol
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        return mapping(symbol).let { if (it === symbol) this else NSymbol(it) }
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        return mapping(symbol) ?: this
    }
}

data class NOther(val name: String) : AbstractNode(), LeafNode {
    override fun plainToString(): String {
        return name
    }

    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is NOther) return false
        return name == other.name
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        return this
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        return this
    }
}

sealed interface NodeChilded : Node, SymbolNode {
    override val symbol: ESymbol

    val children: List<Node>
    val childCount: Int

    override fun <A : Appendable> treeTo(builder: A, level: Int, indent: String): A {
        builder.append(indent).append(symbol.toString()).append(";  ").append(meta.toString()).appendLine()
        val newIndent = "$indent|  "
        children.forEach { it.treeTo(builder, level + 1, newIndent) }
        return builder
    }

    override fun plainToString(): String {
        return symbol.toString() + children.joinToString(prefix = "(", postfix = ")") { it.plainToString() }
    }

    override fun traverse(depth: Int, action: (Node) -> Unit) {
        action(this)
        if (depth > 0) {
            children.forEach { it.traverse(depth - 1, action) }
        }
    }

    override fun traversePostOrder(depth: Int, action: (Node) -> Unit) {
        if (depth > 0) {
            children.forEach { it.traversePostOrder(depth - 1, action) }
        }
        action(this)
    }

    override fun traverseLeveled(depth: Int, level: Int, action: (Node, Int) -> Unit) {
        action(this, level)
        if (depth > 0) {
            children.forEach { it.traverseLeveled(depth - 1, level + 1, action) }
        }
    }

    override fun traversePostOrderLeveled(depth: Int, level: Int, action: (Node, Int) -> Unit) {
        if (depth > 0) {
            children.forEach { it.traversePostOrderLeveled(depth - 1, level + 1, action) }
        }
        action(this, level)
    }

    fun newWithChildren(children: List<Node>): NodeChilded

    fun copyWith(newSymbol: ESymbol): NodeChilded

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        if (depth <= 0) return action(this)
        val res = newWithChildren(children.map { it.recurMap(depth - 1, action) })
        return action(res)
    }

    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is NodeChilded) return false
        if (symbol != other.symbol) return false
        val children = children
        val otherChildren = other.children
        if (children.size != otherChildren.size) return false
        return children.all2(otherChildren) { a, b -> a.deepEquals(b) }
    }

//    override fun sortWith(order: Comparator<Node>): Node {
//        val newChildren = children.map { it.sortWith(order) }.sortedWith(order)
//        return newWithChildren(newChildren).also { it.meta = meta }
//    }
}

typealias Node1 = Node1T<*>

data class Node1T<out C : Node>(override val symbol: ESymbol, val child: C) : AbstractNode(), NodeChilded {

    override val children: List<Node>
        get() = listOf(child)

    override val childCount: Int
        get() = 1


    override fun newWithChildren(children: List<Node>): NodeChilded {
        require(children.size == 1)
        return newWithChildren(children[0])
    }


    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is Node1) return false
        if (symbol != other.symbol) return false
        return child.deepEquals(other.child)
    }

    override fun toString(): String {
        return plainToString()
    }

    fun <S : Node> newWithChildren(child: S): Node1T<S> {
        return Node1T(symbol, child)
    }

    override fun copyWith(newSymbol: ESymbol): NodeChilded {
        return Node1T(newSymbol, child)
    }

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        if (depth <= 0) return action(this)
        val newChild = child.recurMap(depth - 1, action)
        if (newChild === child) return action(this)
        return action(Node1T(symbol, newChild))
    }

    override fun recurAny(depth: Int, action: (Node) -> Boolean): Boolean {
        if (action(this)) return true
        if (depth <= 0) return false
        return child.recurAny(depth - 1, action)
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        val newChild = child.mapSymbol(mapping)
        val newSymbol = mapping(symbol)
        if (newSymbol == symbol && newChild === child) return this
        return Node1T(newSymbol, newChild)
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        val newChild = child.mapSymbolNode(mapping)
        val func = mapping(symbol) ?: return if (newChild === child) this else Node1T(symbol, newChild)
        if (func is NSymbol) {
            return Node1T(func.symbol, newChild)
        }
        return SymBasic.eval(func, newChild)
    }
}

typealias Node2 = Node2T<*, *>

data class Node2T<C1 : Node, C2 : Node>(
    override val symbol: ESymbol,
    val first: C1, val second: C2
) : AbstractNode(), NodeChilded {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Node2Impl<*, *>) return false
//        return name == other.name && first == other.first && second == other.second
//    }
//
//    override fun hashCode(): Int {
//        var result = name.hashCode()
//        result = 31 * result + first.hashCode()
//        result = 31 * result + second.hashCode()
//        return result
//    }

    override val children: List<Node>
        get() = listOf(first, second)

    override val childCount: Int get() = 2

    override fun toString(): String {
        return plainToString()
    }

    override fun newWithChildren(children: List<Node>): NodeChilded {
        require(children.size == 2)
        return newWithChildren(children[0], children[1])
    }

    override fun copyWith(newSymbol: ESymbol): NodeChilded {
        return Node2T(newSymbol, first, second)
    }


    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is Node2) return false
        if (symbol != other.symbol) return false
        return first.deepEquals(other.first) && second.deepEquals(other.second)
    }

    fun <S1 : Node, S2 : Node> newWithChildren(first: S1, second: S2): Node2T<S1, S2> {
        return Node2T(symbol, first, second)
    }

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        if (depth <= 0) return action(this)
        val newFirst = first.recurMap(depth - 1, action)
        val newSecond = second.recurMap(depth - 1, action)
        if (newFirst === first && newSecond === second) return action(this)
        return action(Node2T(symbol, newFirst, newSecond))
    }

    override fun recurAny(depth: Int, action: (Node) -> Boolean): Boolean {
        if (action(this)) return true
        if (depth <= 0) return false
        return first.recurAny(depth - 1, action) || second.recurAny(depth - 1, action)
    }


    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        val new1 = first.mapSymbol(mapping)
        val new2 = second.mapSymbol(mapping)
        val newSymbol = mapping(symbol)
        if (newSymbol == symbol && new1 === first && new2 === second) return this
        return Node2T(newSymbol, new1, new2)
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        val new1 = first.mapSymbolNode(mapping)
        val new2 = second.mapSymbolNode(mapping)
        val newSymbol = mapping(symbol)
            ?: return if (new1 === first && new2 === second) this else Node2T(symbol, new1, new2)
        if (newSymbol is NSymbol) {
            return Node2T(newSymbol.symbol, new1, new2)
        }
        return SymBasic.eval(newSymbol, new1, new2)
    }


}

typealias Node3 = Node3T<*, *, *>

data class Node3T<out C1 : Node, out C2 : Node, out C3 : Node>(
    override val symbol: ESymbol, val first: C1, val second: C2,
    val third: C3
) : AbstractNode(), NodeChilded {

    override fun toString(): String {
        return plainToString()
    }

    override val children: List<Node>
        get() = listOf(first, second, third)

    override val childCount: Int get() = 3

    override fun newWithChildren(children: List<Node>): NodeChilded {
        require(children.size == 3)
        return newWithChildren(children[0], children[1], children[2])
    }

    override fun copyWith(newSymbol: ESymbol): NodeChilded {
        return Node3T(newSymbol, first, second, third)
    }


    override fun deepEquals(other: Node): Boolean {
        if (this === other) return true
        if (other !is Node3) return false
        if (symbol != other.symbol) return false
        return first.deepEquals(other.first) && second.deepEquals(other.second) && third.deepEquals(other.third)
    }


    fun <S1 : Node, S2 : Node, S3 : Node> newWithChildren(first: S1, second: S2, third: S3): Node3T<S1, S2, S3> {
        return Node3T(symbol, first, second, third)
    }

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        if (depth <= 0) return action(this)
        val newFirst = first.recurMap(depth - 1, action)
        val newSecond = second.recurMap(depth - 1, action)
        val newThird = third.recurMap(depth - 1, action)
        if (newFirst === first && newSecond === second && newThird === third) return action(this)
        return action(Node3T(symbol, newFirst, newSecond, newThird))
    }

    override fun recurAny(depth: Int, action: (Node) -> Boolean): Boolean {
        if (action(this)) return true
        if (depth <= 0) return false
        return first.recurAny(depth - 1, action)
                || second.recurAny(depth - 1, action)
                || third.recurAny(depth - 1, action)
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        val new1 = first.mapSymbol(mapping)
        val new2 = second.mapSymbol(mapping)
        val new3 = third.mapSymbol(mapping)
        val newSymbol = mapping(symbol)
        if (newSymbol == symbol && new1 === first && new2 === second && new3 === third) return this
        return Node3T(newSymbol, new1, new2, new3)
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        val new1 = first.mapSymbolNode(mapping)
        val new2 = second.mapSymbolNode(mapping)
        val new3 = third.mapSymbolNode(mapping)
        val newSymbol = mapping(symbol)
            ?: return if (new1 === first && new2 === second && new3 === third) this else Node3T(
                symbol, new1, new2, new3
            )
        if (newSymbol is NSymbol) {
            return Node3T(newSymbol.symbol, new1, new2, new3)
        }
        return SymBasic.eval(newSymbol, new1, new2, new3)
    }
}


data class NodeN(
    override val symbol: ESymbol, override val children: List<Node>
) : AbstractNode(), NodeChilded {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeN) return false
        return symbol == other.symbol && children == other.children
    }

    override fun hashCode(): Int {
        var result = symbol.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    override val childCount: Int
        get() = children.size

    override fun toString(): String {
        return plainToString()
    }

    override fun newWithChildren(children: List<Node>): NodeN {
        return NodeN(symbol, children)
    }

    override fun copyWith(newSymbol: ESymbol): NodeChilded {
        return NodeN(newSymbol, children)
    }

    override fun recurMap(depth: Int, action: (Node) -> Node): Node {
        if (depth <= 0) return action(this)
        var changed = false
        val newChildren = children.map { it.recurMap(depth - 1, action).also { if (it !== children) changed = true } }
        if (!changed) return action(this)
        return action(NodeN(symbol, newChildren))
    }

    override fun recurAny(depth: Int, action: (Node) -> Boolean): Boolean {
        if (action(this)) return true
        if (depth <= 0) return false
        return children.any { it.recurAny(depth - 1, action) }
    }

    override fun mapSymbol(mapping: (ESymbol) -> ESymbol): Node {
        var changed = false
        val newChildren = children.map { c ->
            c.mapSymbol(mapping).also {
                if (it !== c) changed = true
            }
        }
        val newSymbol = mapping(symbol)
        return if (newSymbol != symbol) {
            if (changed) NodeN(newSymbol, newChildren) else NodeN(newSymbol, children)
        } else {
            if (changed) NodeN(symbol, newChildren) else this
        }
    }

    override fun mapSymbolNode(mapping: (ESymbol) -> Node?): Node {
        val newChildren = run {
            var changed = false
            children.map { c ->
                c.mapSymbolNode(mapping).also {
                    if (it !== c) changed = true
                }
            }.let { if (changed) it else children }
        }

        val newSymbol = mapping(symbol)
            ?: return if (newChildren !== children) NodeN(symbol, newChildren) else this
        if (newSymbol is NSymbol) {
            return if (newChildren !== children) NodeN(newSymbol.symbol, newChildren)
            else NodeN(newSymbol.symbol, children)
        }
        return SymBasic.eval(newSymbol, newChildren)
    }
}
