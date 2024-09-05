package cn.mathsymk.linear

import cn.mathsymk.IMathObject
import cn.mathsymk.MathObject
import cn.mathsymk.model.struct.GenVector
import cn.mathsymk.model.struct.VectorModel
import cn.mathsymk.model.struct.indices
import cn.mathsymk.structure.*
import java.util.function.Function


/**
 * Describes a vector
 *
 * @author Ezrnest
 */
interface Vector<T> : GenVector<T>, MathObject<T, EqualPredicate<T>>, VectorModel<T, Vector<T>> {
    /*
    Created by Ezrnest at 2024/09/04 15:33
     */

    override fun applyAll(f: (T) -> T): Vector<T> {
        return VectorImpl.apply1(this, model, f)
    }

    override fun valueEquals(obj: IMathObject<T>): Boolean {
        if (obj !is Vector) return false
        if (size != obj.size) return false
        return indices.all { model.isEqual(this[it], obj[it]) }
    }

    override fun <N> mapTo(newCalculator: EqualPredicate<N>, mapper: Function<T, N>): Vector<N> {
        return VectorImpl.apply1(this, newCalculator, mapper::apply)
    }


    override val isZero: Boolean
        get() {
            val model = model as AddGroup<T>
            return indices.all { model.isZero(this[it]) }
        }

    override fun plus(y: Vector<T>): Vector<T> {
        return VectorImpl.add(this, y, model as AddSemigroup<T>)
    }

    override fun unaryMinus(): Vector<T> {
        return VectorImpl.negate(this, model as AddGroup<T>)
    }

    override fun times(k: T): Vector<T> {
        return VectorImpl.multiply(this, k, model as MulSemigroup<T>)
    }

    override fun div(k: T): Vector<T> {
        return VectorImpl.divide(this, k, model as MulGroup<T>)
    }

    override fun minus(y: Vector<T>): Vector<T> {
        return VectorImpl.subtract(this, y, model as AddGroup<T>)
    }

    override fun times(n: Long): Vector<T> {
        return VectorImpl.multiplyLong(this, n, model as AddGroup<T>)
    }


    /**
     * Returns the inner (dot) product of this vector and the given vector: `⟨this, v⟩`.
     */
    infix fun inner(v: Vector<T>): T {
        return VectorImpl.inner(this, v, model as Ring<T>)
    }

    /**
     * Returns the Hadamard product of this vector and the given vector: `this ⊙ v`.
     */
    infix fun odot(v: Vector<T>): Vector<T> {
        return VectorImpl.odot(this, v, model as MulSemigroup<T>)
    }

    /**
     * Returns the Euclidean norm of this vector, which is the square root of the sum of squares of all elements.
     */
    fun norm(): T {
        return VectorImpl.norm(this, model as Reals<T>)
    }

    /**
     * Returns the sum of squares of all elements, which is the square of the norm.
     */
    fun normSq(): T {
        return VectorImpl.normSq(this, model as Ring<T>)
    }

//    fun normP()

    fun unitize(): Vector<T> {
        return VectorImpl.unitize(this, model as Reals<T>)
    }


    companion object {

        /**
         * Creates a new vector with the given [size] and [model], and the data is generated by the [init].
         *
         * The [init] function will be called with the index of the element, and the return value will be the value of the element.
         *
         * @param size the size of the vector
         * @param model the model of the vector
         */
        inline operator fun <T> invoke(size: Int, model: EqualPredicate<T>, init: (Int) -> T): MutableVector<T> {
            require(size > 0)
            val data = Array<Any?>(size) { k -> init(k) }
            return AVector(data, model)
        }

        /**
         * Creates a new vector from a list of [data] and the given [model].
         */
        fun <T> of(data: List<T>, model: EqualPredicate<T>): MutableVector<T> {
            return Vector(data.size, model) { k -> data[k] }
        }

        /**
         * Creates a new vector with the given [model] and [data].
         */
        fun <T> of(model: EqualPredicate<T>, vararg data: T): MutableVector<T> {
            return of(data.asList(), model)
        }

        fun <T> zero(size: Int, model: AddMonoid<T>): MutableVector<T> {
            return VectorImpl.zero(size, model)
        }

        fun <T> constant(size: Int, value: T, model: EqualPredicate<T>): MutableVector<T> {
            return VectorImpl.constant(size, value, model)
        }

        fun <T> sum(vs: List<Vector<T>>): Vector<T> {
            require(vs.isNotEmpty())
            val size = vs[0].size
            val model = vs[0].model as AddMonoid<T>
            return VectorImpl.sum(vs, size, model)
        }

        fun <T> sum(vararg vs: Vector<T>): Vector<T> {
            return sum(vs.asList())
        }

        fun <T> unitVector(length: Int, index: Int, model: UnitRing<T>): Vector<T> {
            require(index in 0..<length)
            val v = zero(length, model)
            v[index] = model.one
            return Vector(length, model) { k -> if (k == index) model.one else model.zero }
        }

        fun <T> space(field: Field<T>, length: Int): CanonicalVectorSpace<T> {
            return CanonicalVectorSpace(length, field)
        }
    }
}


interface MutableVector<T> : Vector<T> {
    operator fun set(i: Int, value: T)

    fun copy(): MutableVector<T> {
        return VectorImpl.copyOf(this)
    }
}


data class AVector<T>(
    val data: Array<Any?>,
    override val model: EqualPredicate<T>
) : MutableVector<T> {
    init {
        require(data.isNotEmpty())
    }

    override val size: Int
        get() = data.size

    override fun get(i: Int): T {
        @Suppress("UNCHECKED_CAST")
        return data[i] as T
    }

    override fun set(i: Int, value: T) {
        data[i] = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun toList(): List<T> {
        return data.map { it as T }
    }

    @Suppress("UNCHECKED_CAST")
    override fun elementSequence(): Sequence<T> {
        return data.asSequence().map { it as T }
    }


    override fun copy(): AVector<T> {
        return AVector(data.copyOf(), model)
    }

    override fun toString(): String {
        return data.joinToString(prefix = "[", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AVector<*>

        if (!data.contentEquals(other.data)) return false
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + model.hashCode()
        return result
    }
}

object VectorImpl {

    private inline fun <T> apply2(
        x: GenVector<T>, y: GenVector<T>,
        model: EqualPredicate<T>, f: (T, T) -> T
    ): AVector<T> {
        require(x.isSameSize(y))
        val data = Array<Any?>(x.size) { k ->
            f(x[k], y[k])
        }
        return AVector(data, model)
    }

    internal inline fun <T, N> apply1(x: GenVector<T>, model: EqualPredicate<N>, f: (T) -> N): AVector<N> {
        val newData = Array<Any?>(x.size) { k -> f(x[k]) }
        return AVector(newData, model)
    }

    fun <T> copyOf(x: Vector<T>): AVector<T> {
        return apply1(x, x.model) { it }
    }

    fun <T> constant(size: Int, value: T, model: EqualPredicate<T>): AVector<T> {
        return AVector(Array(size) { value }, model)
    }

    fun <T> zero(size: Int, model: AddMonoid<T>): AVector<T> {
        return constant(size, model.zero, model)
    }

    fun <T> isEqual(x: GenVector<T>, y: GenVector<T>, model: EqualPredicate<T>): Boolean {
        require(x.isSameSize(y))
        return x.indices.all { model.isEqual(x[it], y[it]) }
    }

    fun <T> add(x: GenVector<T>, y: GenVector<T>, model: AddSemigroup<T>): AVector<T> {
        return apply2(x, y, model, model::add)
    }

    fun <T> subtract(x: GenVector<T>, y: GenVector<T>, model: AddGroup<T>): AVector<T> {
        return apply2(x, y, model, model::subtract)
    }

    fun <T> negate(x: GenVector<T>, model: AddGroup<T>): AVector<T> {
        return apply1(x, model, model::negate)
    }

    fun <T> multiply(x: GenVector<T>, k: T, model: MulSemigroup<T>): AVector<T> {
        return apply1(x, model) { model.multiply(k, it) }
    }

    fun <T> multiplyLong(x: GenVector<T>, k: Long, model: AddGroup<T>): AVector<T> {
        return apply1(x, model) { model.multiplyLong(it, k) }
    }

    fun <T> divide(x: GenVector<T>, k: T, model: MulGroup<T>): AVector<T> {
        return apply1(x, model) { model.divide(it, k) }
    }

    fun <T> sum(vs: List<GenVector<T>>, size: Int, cal: AddMonoid<T>): AVector<T> {
        require(vs.all { it.size == size }) { "Size mismatch! " }
        return vs.fold(zero(size, cal)) { acc, v -> add(acc, v, cal) }
    }

    fun <T> inner(x: GenVector<T>, y: GenVector<T>, model: Ring<T>): T {
        require(x.isSameSize(y))
        return x.indices.fold(model.zero) { acc, i -> model.add(acc, model.multiply(x[i], y[i])) }
    }

    fun <T> odot(x: GenVector<T>, y: GenVector<T>, model: MulSemigroup<T>): AVector<T> {
        require(x.isSameSize(y))
        return apply2(x, y, model, model::multiply)
    }


    fun <T> normSq(x: GenVector<T>, model: Ring<T>): T {
        return inner(x, x, model)
    }

    fun <T> norm(x: GenVector<T>, model: Reals<T>): T {
        return model.sqrt(normSq(x, model))
    }

    fun <T> unitize(x: GenVector<T>, model: Reals<T>): AVector<T> {
        val n = norm(x, model)
        return apply1(x, model) { model.divide(it, n) }
    }


}

open class CanonicalVectorSpace<K>(override val vectorLength: Int, override val scalars: Field<K>) :
    VectorSpace<K>, InnerProductSpace<K, Vector<K>> {

    /**
     * Creates a new vector with the given [data].
     */
    fun vec(vararg data: K): Vector<K> {
        require(data.size == vectorLength)
        return Vector.of(data.asList(), scalars)
    }




    override fun contains(x: Vector<K>): Boolean {
        return x.model == scalars && x.size == vectorLength && x.elementSequence().all { scalars.contains(it) }
    }

    override fun isEqual(x: Vector<K>, y: Vector<K>): Boolean {
        return VectorImpl.isEqual(x, y, scalars)
    }

    override val zero: Vector<K>
        get() = VectorImpl.zero(vectorLength, scalars)

    override fun negate(x: Vector<K>): Vector<K> {
        return VectorImpl.negate(x, scalars)
    }

    override fun scalarMul(k: K, v: Vector<K>): Vector<K> {
        return VectorImpl.multiply(v, k, scalars)
    }

    override fun add(x: Vector<K>, y: Vector<K>): Vector<K> {
        return VectorImpl.add(x, y, scalars)
    }

    override fun subtract(x: Vector<K>, y: Vector<K>): Vector<K> {
        return VectorImpl.subtract(x, y, scalars)
    }

    override fun sum(elements: List<Vector<K>>): Vector<K> {
        return VectorImpl.sum(elements, vectorLength, scalars)
    }

    override fun inner(u: Vector<K>, v: Vector<K>): K {
        return VectorImpl.inner(u, v, scalars)
    }


    override val basis: List<Vector<K>>
        get() = (0 until vectorLength).map { Vector.unitVector(vectorLength, it, scalars) }

    override fun coefficients(v: Vector<K>): List<K> {
        return v.toList()
    }
}