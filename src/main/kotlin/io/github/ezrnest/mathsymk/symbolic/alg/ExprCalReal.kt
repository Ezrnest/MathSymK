package io.github.ezrnest.mathsymk.symbolic.alg

import io.github.ezrnest.mathsymk.structure.Reals
import io.github.ezrnest.mathsymk.symbolic.BasicExprCal
import io.github.ezrnest.mathsymk.symbolic.ExprCal
import io.github.ezrnest.mathsymk.symbolic.Node
import io.github.ezrnest.mathsymk.symbolic.NodeOrder


class ExprCalReal : BasicExprCal(), Reals<Node>, SymAlg {

//    private fun addAllReduce(rules: RuleList) {
//        rules.list.forEach { addReduceRule(it) }
//    }




    init {
        options[ExprCal.Options.forceReal] = true

        registerReduceRule(RulesExponentialReduce)
        registerReduceRule(RulesTrigonometricReduce)
        registerReduceRule(RulesSummationReduce)

        registerRule(RulesTrigonometricTransform)
    }

    override fun constantValue(name: String): Node {
        return when (name) {
            "pi", SymAlg.Symbols.π.name -> SymAlg.PI
            "e", SymAlg.Symbols.Natural_e.name -> SymAlg.NATURAL_E
            else -> throw IllegalArgumentException("Unknown constant: $name")
        }
    }

    override val one: Node
        get() = SymAlg.ONE

    override val zero: Node
        get() = SymAlg.ZERO


    override fun contains(x: Node): Boolean = true // TODO

    /**
     * Structural equality after one full symbolic reduction on each side.
     *
     * This is intentionally symbolic (not numeric approximation based).
     */
    override fun isEqual(x: Node, y: Node): Boolean {
        val rx = reduce(x)
        val ry = reduce(y)
        return directEquals(rx, ry)
    }

    override fun negate(x: Node): Node {
        return reduce(super.negate(x), 0)
    }

    override fun add(x: Node, y: Node): Node {
        return reduce(super<SymAlg>.sumOf(x, y), 0)
    }

    override fun sumOf(elements: List<Node>): Node {
        return reduce(super<SymAlg>.sumOf(elements), 0)
    }

    override fun multiply(x: Node, y: Node): Node {
        return reduce(super.multiply(x, y), 0)
    }

    override fun productOf(elements: List<Node>): Node {
        return reduce(super<SymAlg>.productOf(elements), 0)
    }

    override fun inv(node: Node): Node {
        return reduce(super<SymAlg>.inv(node), 0)
    }

    override fun reciprocal(x: Node): Node {
        return reduce(super<SymAlg>.inv(x), 0)
    }

    override fun divide(x: Node, y: Node): Node {
        return reduce(super<SymAlg>.divide(x, y), 1)
    }

    override fun Node.div(y: Node): Node {
        return divide(this, y)
    }

    override fun Node.times(y: Node): Node {
        return multiply(this, y)
    }

    override fun Node.minus(y: Node): Node {
        return subtract(this, y)
    }

    override fun Node.unaryMinus(): Node {
        return negate(this)
    }

    override fun Node.plus(y: Node): Node {
        return add(this, y)
    }


    override fun sqrt(x: Node): Node {
        return reduce(super<SymAlg>.sqrt(x), 0)
    }

    override fun exp(x: Node): Node {
        return reduce(super<SymAlg>.exp(x), 0)
    }

    override fun exp(base: Node, pow: Node): Node {
        return reduce(super<SymAlg>.pow(base, pow), 0)
    }

    override fun pow(base: Node, exp: Node): Node {
        return reduce(super<SymAlg>.pow(base, exp), 0)
    }

    override fun nroot(x: Node, n: Int): Node {
        return pow(x, rational(1, n))
    }

    override fun ln(x: Node): Node {
        return reduce(super<SymAlg>.ln(x), 0)
    }

    override fun log(base: Node, x: Node): Node {
        return reduce(super<SymAlg>.log(base, x), 0)
    }

    override fun sin(x: Node): Node {
        return reduce(super<SymAlg>.sin(x), 0)
    }

    override fun cos(x: Node): Node {
        return reduce(super<SymAlg>.cos(x), 0)
    }

    override fun tan(x: Node): Node {
        return reduce(super<SymAlg>.tan(x), 0)
    }

    //    override fun cot(x: Node): Node {
//        return super<NodeScopeAlg>.cot(x).let { reduce(it, 0) }
//    }

    override fun arcsin(x: Node): Node {
        return reduce(super<SymAlg>.arcsin(x), 0)
    }

    override fun arccos(x: Node): Node {
        return reduce(super<SymAlg>.arccos(x), 0)
    }

    override fun arctan(x: Node): Node {
        return reduce(super<SymAlg>.arctan(x), 0)
    }

    override fun abs(x: Node): Node {
        return reduce(super<SymAlg>.abs(x), 0)
    }

    override fun arctan2(y: Node, x: Node): Node {
        TODO()
    }

    /**
     * Canonical ordering after reduction.
     *
     * Returns `0` for symbolically equal nodes; otherwise delegates to [NodeOrder].
     */
    override fun compare(o1: Node, o2: Node): Int {
        val r1 = reduce(o1)
        val r2 = reduce(o2)
        if (directEquals(r1, r2)) return 0
        return NodeOrder.compare(r1, r2)
    }
}
