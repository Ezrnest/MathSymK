package io.github.ezrnest.mathsymk.special

import io.github.ezrnest.mathsymk.model.Polynomial
import io.github.ezrnest.mathsymk.structure.UnitRing
import io.github.ezrnest.mathsymk.structure.eval


/**
 * Describes a family of orthogonal polynomials.
 *
 *
 */
interface OrthogonalPolynomials<T> {

    /**
     * Returns the `n`-th orthogonal polynomial.
     */
    operator fun get(n: Int): Polynomial<T>

    /**
     * Returns the recurrence coefficients `(A_n, B_n, C_n)` for the orthogonal polynomials such that
     * ```
     * P_{n+1}(x) = (A_n x + B_n) P_n(x) - C_n P_{n-1}(x)
     * ```
     */
    fun recurCoeff(n: Int): Triple<T, T, T>

//    /**
//     *
//     */
//    fun recurCoeffSecond(n: Int): Triple<T, T, T>

    fun leadingCoeff(n: Int): T

}

interface MonicOrthogonalPolynomials<T> : OrthogonalPolynomials<T> {

}


abstract class AbstractOrthogonalPolynomials<T>(model: UnitRing<T>) : OrthogonalPolynomials<T> {

    abstract val model: UnitRing<T>
    val polyCal = Polynomial.over(model)

    /**
     * cache[0] = P_{-1}(x) = 0
     * cache[n+1] = P_n(x)
     */
    protected val cache = mutableListOf(polyCal.zero, polyCal.one)


    override fun get(n: Int): Polynomial<T> {
        if (n + 1 < cache.size) return cache[n + 1]
        for (i in (cache.size - 1)..n) {
            val (A, B, C) = recurCoeff(i - 1)
            val Pn = polyCal.eval {
                linear(A, B) * cache[i] - C * cache[i - 1]
            }
            cache.add(Pn)
        }
        return cache[n + 1]
    }

}