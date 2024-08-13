package cn.mathsymk.model.struct

import util.ModelPatterns


interface AddMonoidModel<T : AddMonoidModel<T>> {
    operator fun plus(y: T): T


    /**
     * 'multiply' this with the given long, which is the result of summing `this`
     * for `k` times.
     */
    operator fun times(n: Long): T {
        @Suppress("UNCHECKED_CAST")
        val x = this as T
        return ModelPatterns.binaryProduce(n, x) { a, b -> a + b }
    }
}

interface MulMonoidModel<T : MulMonoidModel<T>> {

    operator fun times(y: T): T


    /**
     * Returns the `n`-th power of `this`.
     */
    infix fun pow(n: Long): T {
        @Suppress("UNCHECKED_CAST")
        val x = this as T
        return ModelPatterns.binaryProduce(n, x) { a, b -> a.times(b) }
    }
}


/**
 *
 */
interface AddGroupModel<T : AddGroupModel<T>> : AddMonoidModel<T> {
    /**
     * Returns `this + y` as the operation defined in the group.
     */
    override fun plus(y: T): T

    /**
     * Returns the negation of `this`.
     */
    operator fun unaryMinus(): T

    /**
     * Returns `this - y`, which should be equal to `add(negate(y))`.
     */
    operator fun minus(y: T): T {
        return plus(-y)
    }

    override fun times(n: Long): T {
        if (n < 0) {
            return unaryMinus().times(n)
        }
        if (n == 0L) {
            return plus(-this)
        }
        return super.times(n)
    }
}

/**
 * Describes a number model which is suitable for a group where the operation is named as multiplication.
 *
 * While being a group, we allow some elements to be not invertible so the model will be compatible for a ring.
 * In this case, an [ArithmeticException] will be thrown when the reciprocal of a non-invertible element is requested.
 */
interface MulGroupModel<T : MulGroupModel<T>> : MulMonoidModel<T> {
    /**
     * Returns `this * y` as the operation defined in the group.
     */
    override fun times(y: T): T

    /**
     * Determines whether this element is invertible.
     */
    val isInvertible: Boolean

    /**
     * Returns the reciprocal of `this`, that is, the element `e` such that `e * this = this * e = 1`
     */
    operator fun inv(): T


    /**
     * Returns `this - y`, which should be equal to `add(negate(y))`.
     */
    operator fun div(y: T): T = times(y.inv())

    override fun pow(n: Long): T {

        if (n < 0) {
            return inv().pow(n)
        }
        @Suppress("UNCHECKED_CAST")
        val x = this as T
        if (n == 0L) {
            return x.div(x)
        }
        return super.pow(n)
    }
}
