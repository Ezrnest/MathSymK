package io.github.ezrnest.mathsymk.model

import io.github.ezrnest.mathsymk.Mappable
import io.github.ezrnest.mathsymk.structure.*
import java.util.function.Function


/*
Created by liyicheng 2020/2/24
*/



/**
 * Defines the complex number, which is a number that can be written as `a+bi`, where `a` is the real part and `b` is the imaginary part.
 * The complex numbers satisfy:
 * * Addition: `(a+bi)+(c+di) = (a+c) + (b+d)i`
 * * Multiplication: `i^2 = -1`, `(a+bi)(c+di) = (ac-bd) + (ad+bc)i`
 *
 *
 */
@JvmRecord
data class Complex<T>(val re: T, val im: T) : Mappable<T> {
    /*
    Created by lyc at 2024/8/29
     */

    override fun toString(): String {
        return "$re+${im}i"
    }

    override fun <S> map(mapping: (T) -> S): Complex<S> {
        return Complex(mapping(re), mapping(im))
    }

    fun <N> mapTo(mapper: Function<T, N>): Complex<N> {
        return Complex(mapper.apply(re), mapper.apply(im))
    }

    companion object {

        fun <T> over(model: UnitRing<T>): ComplexOverURingStd<T> {
            return ComplexOverURingStd(model)
        }

        fun <T> over(model: Field<T>): ComplexOnFieldStd<T> {
            return ComplexOnFieldStd(model)
        }

        fun <T> over(reals: Reals<T>): ComplexFromRealsStd<T> {
            return ComplexFromRealsStd(reals)
        }
    }
}


open class ComplexOverURingStd<T>(_model: UnitRing<T>) : ComplexOverURing<T, Complex<T>> {
    override val scalars: UnitRing<T> = _model

    final override val zero: Complex<T> = Complex(_model.zero, _model.zero)

    final override val one: Complex<T>
        get() = Complex(scalars.one, scalars.zero)

    override fun complexOf(re: T, im: T): Complex<T> {
        return Complex(re, im)
    }

    override fun ofReal(a: T): Complex<T> {
        return Complex(a, scalars.zero)
    }

    override fun ofImag(b: T): Complex<T> {
        return Complex(scalars.zero, b)
    }

    override fun re(z: Complex<T>): T {
        return z.re
    }

    override fun im(z: Complex<T>): T {
        return z.im
    }

    final override fun contains(x: Complex<T>): Boolean {
        return scalars.contains(x.re) && scalars.contains(x.im)
    }

    final override fun isEqual(x: Complex<T>, y: Complex<T>): Boolean {
        return scalars.isEqual(x.re, y.re) && scalars.isEqual(x.im, y.im)
    }

    final override fun isZero(x: Complex<T>): Boolean {
        return scalars.isZero(x.re) && scalars.isZero(x.im)
    }


    /*
    Complex number related methods
     */


    final override fun add(x: Complex<T>, y: Complex<T>): Complex<T> {
        return Complex(scalars.add(x.re, y.re), scalars.add(x.im, y.im))
    }

    final override fun negate(x: Complex<T>): Complex<T> {
        return Complex(scalars.negate(x.re), scalars.negate(x.im))
    }

    final override fun subtract(x: Complex<T>, y: Complex<T>): Complex<T> {
        return scalars.eval { Complex(x.re - y.re, x.im - y.im) }
    }


    final override fun scalarMul(k: T, v: Complex<T>): Complex<T> {
        return Complex(scalars.multiply(k, v.re), scalars.multiply(k, v.im))
    }

    override fun modSq(z: Complex<T>): T {
        return scalars.eval { z.re * z.re + z.im * z.im }
    }

    inline val Complex<T>.modSq: T
        get() {
            return modSq(this)
        }

    override fun conj(z: Complex<T>): Complex<T> {
        return Complex(z.re, scalars.negate(z.im))
    }

    inline val Complex<T>.conj: Complex<T>
        get() {
            return scalars.eval { Complex(re, -im) }
        }

    final override fun multiply(x: Complex<T>, y: Complex<T>): Complex<T> {
        return scalars.eval { Complex(x.re * y.re - x.im * y.im, x.re * y.im + x.im * y.re) }
    }


    final override fun multiplyN(x: Complex<T>, n: Long): Complex<T> {
        return scalars.eval { Complex(x.re * n, x.im * n) }
    }

    final override fun sumOf(elements: List<Complex<T>>): Complex<T> {
        return scalars.eval {
            val a = scalars.sumOf(elements.map { it.re })
            val b = scalars.sumOf(elements.map { it.im })
            Complex(a, b)
        }
    }

    /*
    Operator overloading, possible since we can use the concrete class Complex
     */
    operator fun Complex<T>.plus(y: T): Complex<T> {
        return add(this, ofReal(y))
    }

    operator fun T.plus(y: Complex<T>): Complex<T> {
        return add(ofReal(this), y)
    }

    operator fun Complex<T>.minus(y: T): Complex<T> {
        return subtract(this, ofReal(y))
    }

    operator fun T.minus(y: Complex<T>): Complex<T> {
        return subtract(ofReal(this), y)
    }

    operator fun Complex<T>.times(y: T): Complex<T> {
        return multiply(this, ofReal(y))
    }

    operator fun T.times(y: Complex<T>): Complex<T> {
        return multiply(ofReal(this), y)
    }
}


open class ComplexOnFieldStd<T>(override val scalars: Field<T>) :
    ComplexOverURingStd<T>(scalars),
    ComplexOverField<T, Complex<T>> {


    final override fun reciprocal(x: Complex<T>): Complex<T> {
        val d = scalars.eval { x.re * x.re + x.im * x.im }
        return scalars.eval { Complex(x.re / d, -x.im / d) }
    }

    final override fun divide(x: Complex<T>, y: Complex<T>): Complex<T> {
        val d = scalars.eval { y.re * y.re + y.im * y.im }
        return scalars.eval {
            val t1 = x.re * y.re + x.im * y.im
            val t2 = x.im * y.re - x.re * y.im
            Complex(t1 / d, t2 / d)
        }
    }

    final override fun scalarDiv(x: Complex<T>, k: T): Complex<T> {
        return scalars.eval { Complex(x.re / k, x.im / k) }
    }

    /*
    Operator overloading
     */
    operator fun Complex<T>.div(k: T): Complex<T> {
        return scalarDiv(this, k)
    }

    operator fun T.div(v: Complex<T>): Complex<T> {
        return v.inv() * this
    }
}

open class ComplexFromRealsStd<T>(override val reals: Reals<T>) : ComplexOnFieldStd<T>(reals),
    ComplexNumbers<T, Complex<T>> {

    override val scalars: Reals<T>
        get() = reals

}
