/*
 * 2018-02-28
 */
package io.github.ezrnest.mathsymk.structure

import java.util.*

/**
 * A predicate that tests whether two objects are equal.
 *
 * EqualPredicate is at the top of the calculator hierarchy.
 *
 * @author liyicheng
 * 2018-02-28 17:33
 */
@FunctionalInterface
fun interface EqualPredicate<in T> {
    /**
     * Evaluates whether the two objects are equal.
     *
     *
     * * It is *reflexive*: for any non-null reference value `x`,
     * `test(x,x)` should return `true`.
     * * It is *symmetric*: for any non-null reference values `x` and
     * `y`, `test(x,y)` should return `true` if and only if `test(y,x)`
     * returns `true`.
     * * It is *transitive*: for any non-null reference values `x`,
     * `y`, and `z`, if `test(x,y)` returns `true` and
     * `test(y,z)` returns `true`, then `test(x,z)` should return `true`.
     * * It is *consistent*: for any non-null reference values `x` and
     * `y`, multiple invocations of `test(x,y)` consistently return
     * `true` or consistently return `false`, provided no information
     * used in `test` comparisons on the objects is modified.
     *
     * @throws NullPointerException if `x==null || y == null`
     */
    fun isEqual(x: T, y: T): Boolean

//    /**
//     * Gets the class of the number that this calculator deals with.
//     */
//    val numberClass: Class<T>

    companion object {

        @JvmStatic
        fun <T> naturalEqual(): EqualPredicate<T> {
            return object : EqualPredicate<T> {
                override fun isEqual(x: T, y: T): Boolean {
                    return Objects.equals(x, y)
                }

//                override val numberClass: Class<T> = clz
            }
        }

        @JvmStatic
        fun <T> refEqual(): EqualPredicate<T> {
            return object : EqualPredicate<T> {
                override fun isEqual(x: T, y: T): Boolean {
                    return x === y
                }
//                override val numberClass: Class<T> = clz
            }
        }
    }

}
