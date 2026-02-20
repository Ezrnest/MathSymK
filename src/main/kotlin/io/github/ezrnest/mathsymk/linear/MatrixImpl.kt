package io.github.ezrnest.mathsymk.linear

import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.addColTo
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.addRowTo
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.divAssign
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.divRow
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.mulAddCol
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.mulAddRow
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.mulCol
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.mulRow
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.negateRow
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.transformCols
import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.transformRows
import io.github.ezrnest.mathsymk.model.Polynomial
import io.github.ezrnest.mathsymk.structure.*
import io.github.ezrnest.mathsymk.util.IterUtils
import io.github.ezrnest.mathsymk.util.ModelPatterns
import kotlin.math.min


@ConsistentCopyVisibility
@JvmRecord
data class AMatrix<T> internal constructor(
    override val row: Int, override val column: Int,
    val data: Array<Any?>,
) : MutableMatrix<T> {

    init {
        require(row * column == data.size)
//        require(data.isNotEmpty()) // allow empty matrix
    }

    override val size: Int
        get() = data.size

    private fun checkIndex(i: Int, j: Int) {
        require(i in 0 until row && j in 0 until column)
    }

    private fun toPos(i: Int, j: Int): Int {
        return i * column + j
    }


    private fun getChecked(i: Int, j: Int): T {
        @Suppress("UNCHECKED_CAST")
        return data[toPos(i, j)] as T
    }

    override fun get(i: Int, j: Int): T {
        checkIndex(i, j)
        return getChecked(i, j)
    }

    override fun rowAt(rowIdx: Int): Vector<T> {
        val pos0 = toPos(rowIdx, 0)
        return AVector(data.copyOfRange(pos0, pos0 + column))
    }

    override fun setAll(row: Int, col: Int, matrix: Matrix<T>) {
        if (matrix !is AMatrix) {
            super.setAll(row, col, matrix)
            return
        }
        val mData = matrix.data
        for (i in matrix.rowIndices) {
            val p = matrix.toPos(i, 0)
            val destPos = toPos(i + row, col)
            mData.copyInto(data, destPos, p, p + matrix.column)
        }
    }


    override fun copy(): AMatrix<T> {
        return AMatrix(row, column, data.copyOf())
    }

    override fun set(i: Int, j: Int, value: T) {
        checkIndex(i, j)
        data[toPos(i, j)] = value
    }

    override fun setRow(i: Int, row: GenVector<T>) {
        require(row.size == column)
        require(i in 0..<this.row)
        if (row !is AVector) {
            super.setRow(i, row)
            return
        }
        row.data.copyInto(data, toPos(i, 0))
    }

    internal inline fun inPlaceApply1(f: (T) -> T) {
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            data[i] = f(data[i] as T)
        }
    }


    override fun swapRow(r1: Int, r2: Int, colStart: Int, colEnd: Int) {
        require(r1 in 0 until row && r2 in 0 until row)
        val s1 = toPos(r1, 0)
        val s2 = toPos(r2, 0)
        for (l in colStart until colEnd) {
            val t = data[s1 + l]
            data[s1 + l] = data[s2 + l]
            data[s2 + l] = t
        }
    }


    override fun swapCol(c1: Int, c2: Int, rowStart: Int, rowEnd: Int) {
        require(c1 in 0 until column && c2 in 0 until column)
        var l = toPos(rowStart, 0)
        for (r in rowStart until rowEnd) {
            val t = data[l + c1]
            data[l + c1] = data[l + c2]
            data[l + c2] = t
            l += row
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AMatrix<*>

        return row == other.row && column == other.column && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String {
        return MatrixImpl.formatString(this)
    }

    companion object {
        fun <T> isEqual(x: AMatrix<T>, y: AMatrix<T>, predicate: EqualPredicate<T>): Boolean {
            if (x.row != y.row || x.column != y.column) return false
            val size = x.size
            return (0 until size).all {
                @Suppress("UNCHECKED_CAST")
                predicate.isEqual(x.data[it] as T, y.data[it] as T)
            }
        }


        fun <T> isZero(x: AMatrix<T>, model: AddMonoid<T>): Boolean {
            @Suppress("UNCHECKED_CAST")
            return x.data.all { model.isZero(it as T) }
        }

        internal inline fun <T> ofFlatten(row: Int, col: Int, init: (Int) -> T): AMatrix<T> {
            val data = Array<Any?>(row * col) { init(it) }
            return AMatrix(row, col, data)
        }

        fun <T> fromFlattenList(row: Int, col: Int, data: List<T>): AMatrix<T> {
            require(row * col == data.size)
            return AMatrix(row, col, data.toTypedArray())
        }


        internal inline operator fun <T> invoke(
            row: Int, column: Int, init: (Int, Int) -> T,
        ): AMatrix<T> {
            val data = Array<Any?>(row * column) { }
            for (i in 0..<row) {
                val pos = i * column
                for (j in 0..<column) {
                    data[pos + j] = init(i, j)
                }
            }
            return AMatrix(row, column, data)
        }

        internal inline fun <T1, T2, N> apply2(
            x: AMatrix<T1>, y: AMatrix<T2>, f: (T1, T2) -> N,
        ): AMatrix<N> {
            require(x.shapeMatches(y))
            val d1 = x.data
            val d2 = y.data
            return ofFlatten(x.row, x.column) { k ->
                @Suppress("UNCHECKED_CAST")
                f(d1[k] as T1, d2[k] as T2)
            }
        }

        internal inline fun <T, N> apply1(x: AMatrix<T>, f: (T) -> N): AMatrix<N> {
            val data = x.data
            return ofFlatten(x.row, x.column) { k ->
                @Suppress("UNCHECKED_CAST")
                f(data[k] as T)
            }
        }

        internal inline fun <T, N> apply2InPlace(x: AMatrix<T>, y: AMatrix<N>, f: (T, N) -> T) {
            require(x.shapeMatches(y))
            val d1 = x.data
            val d2 = y.data
            for (i in 0 until x.size) {
                @Suppress("UNCHECKED_CAST")
                d1[i] = f(d1[i] as T, d2[i] as N)
            }
        }


        fun <T> copyOf(x: Matrix<T>): AMatrix<T> {
            if (x is AMatrix) {
                return x.copy()
            }
            return AMatrix(x.row, x.column) { i, j -> x[i, j] }
        }

        fun <T> of(row: Int, column: Int, init: (Int, Int) -> T): AMatrix<T> {
            return AMatrix(row, column, init)
        }

        fun <T> of(row: Int, col: Int, vararg data: T): AMatrix<T> {
            require(row * col == data.size)
            val dataCopy = Array<Any?>(data.size) { data[it] }
            return AMatrix(row, col, dataCopy)
        }

        internal fun <T> AMatrix<T>.negateInPlace(model: AddGroup<T>) {
            inPlaceApply1 { model.negate(it) }
        }

        internal fun <T> AMatrix<T>.plusAssign(y: Matrix<T>, model: AddSemigroup<T>) {
            return MatrixImpl.addInPlace(this, y, model)
        }

        internal fun <T> AMatrix<T>.timesAssign(k: T, model: MulSemigroup<T>) {
            inPlaceApply1 { model.multiply(k, it) }
        }

        internal fun <T> AMatrix<T>.divAssign(k: T, model: UnitRing<T>) {
            inPlaceApply1 { model.exactDiv(it, k) }
        }

        internal fun <T> AMatrix<T>.mulRow(
            r: Int, k: T, colStart: Int = 0, colEnd: Int = this.column, model: MulSemigroup<T>
        ) {
            require(r in 0 until row)
            val d = toPos(r, 0)
            for (l in colStart until colEnd) {
                @Suppress("UNCHECKED_CAST")
                data[d + l] = model.multiply(k, data[d + l] as T)
            }
        }

        internal fun <T> AMatrix<T>.divRow(
            r: Int, k: T, colStart: Int = 0, colEnd: Int = this.column, model: UnitRing<T>
        ) {
            require(r in 0 until row)
            val d = toPos(r, 0)
            for (l in colStart until colEnd) {
                @Suppress("UNCHECKED_CAST")
                data[d + l] = model.exactDiv(data[d + l] as T, k)
            }
        }

        internal fun <T> AMatrix<T>.mulCol(
            c: Int, k: T, rowStart: Int = 0, rowEnd: Int = this.row, model: MulSemigroup<T>
        ) {
            require(c in 0 until column)
            for (r in rowStart until rowEnd) {
                val pos = toPos(r, c)
                @Suppress("UNCHECKED_CAST")
                data[pos] = model.multiply(k, data[pos] as T)
            }
        }

        internal fun <T> AMatrix<T>.divCol(
            c: Int, k: T, rowStart: Int = 0, rowEnd: Int = this.row, model: UnitRing<T>
        ) {
            require(c in 0 until column)
            for (r in rowStart until rowEnd) {
                val pos = toPos(r, c)
                @Suppress("UNCHECKED_CAST")
                data[pos] = model.exactDiv(k, data[pos] as T)
            }
        }

        internal fun <T> AMatrix<T>.negateRow(
            r: Int, colStart: Int = 0, colEnd: Int = this.column, model: AddGroup<T>
        ) {
            require(r in 0 until row)
            val d = toPos(r, 0)
            for (l in colStart until colEnd) {
                @Suppress("UNCHECKED_CAST")
                data[d + l] = model.negate(data[d + l] as T)
            }
        }

        internal fun <T> AMatrix<T>.negateCol(c: Int, rowStart: Int = 0, rowEnd: Int = this.row, model: AddGroup<T>) {
            require(c in 0 until column)
            for (r in rowStart until rowEnd) {
                val pos = toPos(r, c)
                @Suppress("UNCHECKED_CAST")
                data[pos] = model.negate(data[pos] as T)
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.addRowTo(
            r1: Int, r2: Int, colStart: Int = 0, colEnd: Int = this.column, model: AddSemigroup<T>
        ) {
            require(r1 in 0 until row && r2 in 0 until row)
            val s1 = toPos(r1, 0)
            val s2 = toPos(r2, 0)
            for (l in colStart until colEnd) {
                data[s2 + l] = model.add(data[s2 + l] as T, data[s1 + l] as T)
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.addColTo(
            c1: Int, c2: Int, rowStart: Int = 0, rowEnd: Int = this.row, model: AddSemigroup<T>
        ) {
            require(c1 in 0..<column && c2 in 0..<column)
            for (r in rowStart..<rowEnd) {
                val l = toPos(r, 0)
                data[l + c2] = model.add(data[l + c2] as T, data[l + c1] as T)
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.mulAddRow(
            r1: Int, r2: Int, k: T, colStart: Int = 0, colEnd: Int = this.column, model: Ring<T>
        ) {
            require(r1 in 0..<row && r2 in 0..<row)
            val s1 = toPos(r1, 0)
            val s2 = toPos(r2, 0)
            for (l in colStart until colEnd) {
                data[s2 + l] = model.eval { (data[s2 + l] as T) + k * (data[s1 + l] as T) }
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.mulAddCol(
            c1: Int, c2: Int, k: T, rowStart: Int = 0, rowEnd: Int = this.row, model: Ring<T>
        ) {
            require(c1 in 0..<column && c2 in 0..<column)
            for (r in rowStart..<rowEnd) {
                val l = toPos(r, 0)
                data[l + c2] = model.eval { (data[l + c2] as T) + k * (data[l + c1] as T) }
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.transformRows(
            r1: Int, r2: Int, a11: T, a12: T, a21: T, a22: T, colStart: Int = 0, colEnd: Int = this.column,
            model: Ring<T>
        ) {
            require(r1 in 0..<row && r2 in 0..<row)
            require(colStart in 0..colEnd && colEnd <= column)
            val s1 = toPos(r1, 0)
            val s2 = toPos(r2, 0)
            with(model) {
                for (l in colStart..<colEnd) {
                    val x = data[s1 + l] as T
                    val y = data[s2 + l] as T
                    data[s1 + l] = a11 * x + a12 * y
                    data[s2 + l] = a21 * x + a22 * y
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T> AMatrix<T>.transformCols(
            c1: Int, c2: Int, a11: T, a12: T, a21: T, a22: T, rowStart: Int = 0, rowEnd: Int = this.row, model: Ring<T>
        ) {
            with(model) {
                for (r in rowStart until rowEnd) {
                    val pos0 = toPos(r, 0)
                    val pos1 = pos0 + c1
                    val pos2 = pos0 + c2
                    val x = data[pos1] as T
                    val y = data[pos2] as T
                    data[pos1] = a11 * x + a12 * y
                    data[pos2] = a21 * x + a22 * y
                }
            }
        }


    }
}

open class TransposedMatrixView<T>(open val origin: Matrix<T>) : Matrix<T> {
    override val row: Int
        get() = origin.column
    override val column: Int
        get() = origin.row

    override fun get(i: Int, j: Int): T {
        require(i in 0..<row && j in 0..<column)
        return origin[j, i]
    }

    override fun transpose(): Matrix<T> {
        return origin
    }

    override fun rowAt(rowIdx: Int): Vector<T> {
        require(rowIdx in 0..<row)
        return origin.colAt(rowIdx)
    }

    override fun colAt(colIdx: Int): Vector<T> {
        require(colIdx in 0..<column)
        return origin.rowAt(colIdx)
    }

    override fun rowVectors(): List<Vector<T>> {
        return origin.colVectors()
    }

    override fun colVectors(): List<Vector<T>> {
        return origin.rowVectors()
    }

//    override fun det(): T {
//        return origin.det()
//    }
//
//    override fun rank(): Int {
//        return origin.rank()
//    }
//
//    override fun trace(): T {
//        return origin.trace()
//    }
//
//    override fun diag(): Vector<T> {
//        return origin.diag()
//    }
//
//    override fun sumAll(): T {
//        return origin.sumAll()
//    }

    override fun toString(): String {
        return MatrixImpl.formatString(this)
    }
}

open class SubMatrixView<T>(
    val origin: Matrix<T>,
    rowStart: Int = 0, rowEnd: Int, colStart: Int, colEnd: Int,
) : Matrix<T> {
    init {
        require(0 <= rowStart && rowEnd <= origin.row && rowStart <= rowEnd)
        require(0 <= colStart && colEnd <= origin.column && colStart <= colEnd)
    }

    final override val row = rowEnd - rowStart
    final override val column = colEnd - colStart
    val dRow = rowStart
    val dCol = colStart

    override fun get(i: Int, j: Int): T {
        require(i in 0 until row && j in 0 until column)
        return origin[i + dRow, j + dCol]
    }

    override fun subMatrix(rowStart: Int, colStart: Int, rowEnd: Int, colEnd: Int): Matrix<T> {
        return SubMatrixView(origin, rowStart + dRow, rowEnd + dRow, colStart + dCol, colEnd + dCol)
    }

    override fun toString(): String {
        return MatrixImpl.formatString(this)
    }
}


open class SlicedMatrixView<T>(
    val origin: Matrix<T>, val rowMap: IntArray, val colMap: IntArray,
) : Matrix<T> {
    init {
        require(rowMap.isNotEmpty() && colMap.isNotEmpty())
        require(rowMap.all { it in 0 until origin.row })
        require(colMap.all { it in 0 until origin.column })
    }

    override val row: Int
        get() = rowMap.size
    override val column: Int
        get() = colMap.size

    override fun get(i: Int, j: Int): T {
        require(i in 0 until row && j in 0 until column)
        return origin[rowMap[i], colMap[j]]
    }

    override fun slice(rows: IntArray, cols: IntArray): Matrix<T> {
        val newRows = IntArray(rows.size) { this.rowMap[rows[it]] }
        val newCols = IntArray(cols.size) { this.colMap[cols[it]] }
        return SlicedMatrixView(origin, newRows, newCols)
    }

    override fun toString(): String {
        return MatrixImpl.formatString(this)
    }
}

//TODO Mutable view

/**
 * Provides Matrix-related functionalities.
 *
 * Most of the methods in this object accept [Matrix]'s as inputs and a `model` should be provided to specify the operations.
 */
object MatrixImpl {

    fun <T> formatString(m: Matrix<T>): String {
        val sb = StringBuilder()
        sb.append("Matrix(${m.row}, ${m.column}):\n")
        m.joinTo(sb)
        return sb.toString()
    }

    fun <T> isEqual(x: Matrix<T>, y: Matrix<T>, predicate: EqualPredicate<T>): Boolean {
        if (x is AMatrix && y is AMatrix) {
            return AMatrix.isEqual(x, y, predicate)
        }
        return x.row == y.row && x.column == y.column && x.rowIndices.all { i ->
            x.colIndices.all { j ->
                predicate.isEqual(x[i, j], y[i, j])
            }
        }
    }

    fun <T> isZero(x: Matrix<T>, scalars: AddMonoid<T>): Boolean {
        if (x is AMatrix) {
            return AMatrix.isZero(x, scalars)
        }
        return x.rowIndices.all { i ->
            x.colIndices.all { j ->
                scalars.isZero(x[i, j])
            }
        }
    }

    private inline fun <T1, T2, N> apply2(
        x: Matrix<T1>, y: Matrix<T2>, f: (T1, T2) -> N,
    ): AMatrix<N> {
        require(x.shapeMatches(y))
        if (x is AMatrix && y is AMatrix) {
            return AMatrix.apply2(x, y, f) // flattened version
        }
        return AMatrix(x.row, x.column) { i, j -> f(x[i, j], y[i, j]) }
    }

    internal inline fun <T, N> apply1(x: Matrix<T>, f: (T) -> N): AMatrix<N> {
        if (x is AMatrix) {
            return AMatrix.apply1(x, f)// flattened version
        }
        return AMatrix(x.row, x.column) { i, j -> f(x[i, j]) }
    }


    fun <T> hadamard(x: Matrix<T>, y: Matrix<T>, model: MulSemigroup<T>): AMatrix<T> {
        return apply2(x, y, model::multiply)
    }

    fun <T> add(x: Matrix<T>, y: Matrix<T>, model: AddSemigroup<T>): AMatrix<T> {
        return apply2(x, y, model::add)
    }

    private inline fun <T> apply1InPlace(x: MutableMatrix<T>, f: (T) -> T) {
        if (x is AMatrix) {
            x.inPlaceApply1(f)
        } else {
            for (i in 0 until x.row) {
                for (j in 0 until x.column) {
                    x[i, j] = f(x[i, j])
                }
            }
        }
    }


    private inline fun <T, T2> apply2InPlace(x: MutableMatrix<T>, y: Matrix<T2>, f: (T, T2) -> T) {
        if (x is AMatrix && y is AMatrix) {
            AMatrix.apply2InPlace(x, y, f)
        } else {
            for (i in 0 until x.row) {
                for (j in 0 until x.column) {
                    x[i, j] = f(x[i, j], y[i, j])
                }
            }
        }
    }

    fun <T> addInPlace(x: MutableMatrix<T>, y: Matrix<T>, model: AddSemigroup<T>) {
        apply2InPlace(x, y, model::add)
    }

    fun <T> subtractInPlace(x: MutableMatrix<T>, y: Matrix<T>, model: AddGroup<T>) {
        apply2InPlace(x, y, model::subtract)
    }

    fun <T> multiplyNInPlace(x: MutableMatrix<T>, k: Long, model: AddSemigroup<T>) {
        apply1InPlace(x) { model.multiplyN(it, k) }
    }

    fun <T> scalarMulInPlace(x: MutableMatrix<T>, k: T, model: MulSemigroup<T>) {
        apply1InPlace(x) { model.multiply(k, it) }
    }

    fun <T> negate(x: Matrix<T>, model: AddGroup<T>): AMatrix<T> {
        return apply1(x, model::negate)
    }

    fun <T> subtract(x: Matrix<T>, y: Matrix<T>, model: AddGroup<T>): AMatrix<T> {
        return apply2(x, y, model::subtract)
    }

    fun <T> sum(mats: List<Matrix<T>>, model: AddSemigroup<T>): AMatrix<T> {
        require(mats.isNotEmpty())
        val row = mats.first().row
        val column = mats.first().column
        val result = AMatrix.copyOf(mats.first())
        for (i in 1 until mats.size) {
            val m = mats[i]
            require(m.row == row && m.column == column)
            addInPlace(result, m, model)
        }
        return result
    }


    fun <T> matmul(x: Matrix<T>, y: Matrix<T>, model: Ring<T>): AMatrix<T> {
        require(x.column == y.row) {
            "Shape mismatch in matmul: (${x.row}, ${x.column}) * (${y.row}, ${y.column})"
        }
        return AMatrix(x.row, y.column) { i, j ->
            var sum = model.zero
            for (k in 0 until x.column) {
                sum = model.eval { sum + x[i, k] * y[k, j] }
            }
            sum
        }
    }

    /**
     * Matrix-vector multiplication: `Ay`, where `y` is a column vector.
     *
     */
    fun <T> matmul(A: Matrix<T>, y: GenVector<T>, model: Ring<T>): AVector<T> {
        require(A.column == y.size) {
            "Shape mismatch in matmul: (${A.row}, ${A.column}) * (${y.size})"
        }
        return AVector(A.row) { i ->
            var sum = model.zero
            for (k in 0 until A.column) {
                sum = model.eval { sum + A[i, k] * y[k] }
            }
            sum
        }
    }

    /**
     * Matrix-vector multiplication: `v.T A`, where `v` is a column vector.
     *
     * The result will be a vector
     */
    fun <T> matmul(v: GenVector<T>, A: Matrix<T>, model: Ring<T>): AVector<T> {
        require(v.size == A.row) {
            "Shape mismatch in matmul: (${v.size}) * (${A.row}, ${A.column})"
        }
        return AVector(A.column) { j ->
            var sum = model.zero
            for (k in 0..<A.row) {
                sum = model.eval { sum + v[k] * A[k, j] }
            }
            sum
        }
    }

    fun <T> product(mats: List<Matrix<T>>, model: Ring<T>): Matrix<T> {
        require(mats.isNotEmpty())
        return ModelPatterns.reduceDP<Matrix<T>, IntArray>(0, mats.size,
            get = { i -> mats[i] },
            operation = { m1, m2 -> matmul(m1, m2, model) },
            toModel = { m -> intArrayOf(m.row, m.column) },
            modelOperation = { x, y -> intArrayOf(x[0], y[1]) },
            modelTimeCost = { x, y -> x[0] * y[0] * y[1] }
        )
    }


    fun <T> multiply(x: Matrix<T>, k: T, model: MulSemigroup<T>): AMatrix<T> {
        return apply1(x) { model.multiply(k, it) }
    }

    fun <T> multiplyLong(x: Matrix<T>, k: Long, model: AddGroup<T>): AMatrix<T> {
        return apply1(x) { model.multiplyN(it, k) }
    }

    fun <T> divide(x: Matrix<T>, k: T, model: MulGroup<T>): AMatrix<T> {
        return apply1(x) { model.divide(it, k) }
    }


    fun <T> transpose(x: Matrix<T>): TransposedMatrixView<T> {
        return TransposedMatrixView(x)
    }

    fun <T> zero(row: Int, column: Int, model: AddMonoid<T>): AMatrix<T> {
        val zero = model.zero
        return AMatrix.ofFlatten(row, column) { zero }
    }

    fun <T> identity(n: Int, model: UnitRing<T>): AMatrix<T> {
        return scalar(n, model, model.one)
    }

    fun <T> scalar(n: Int, model: AddMonoid<T>, k: T): AMatrix<T> {
        val A = zero(n, n, model)
        for (i in 0 until n) {
            A[i, i] = k
        }
        return A
    }

    fun <T> concatCol(a: Matrix<T>, b: Matrix<T>): AMatrix<T> {
        require(a.row == b.row)
        return AMatrix(a.row, a.column + b.column) { i, j ->
            if (j < a.column) a[i, j] else b[i, j - a.column]
        }
    }

    fun <T> concatRow(a: Matrix<T>, b: Matrix<T>): AMatrix<T> {
        require(a.column == b.column)
        return AMatrix(a.row + b.row, a.column) { i, j ->
            if (i < a.row) a[i, j] else b[i - a.row, j]
        }
    }

    fun <T> addDiagonal(m: MutableMatrix<T>, a: T, model: AddSemigroup<T>) {
        for (i in 0..<min(m.row, m.column)) {
            m[i, i] = model.add(m[i, i], a)
        }
    }


//    fun <T> minor(A: Matrix<T>, rows: IntArray, cols: IntArray): Matrix<T> {
//        val remRows = remainingIndices(A.row, rows)
//        val remCols = remainingIndices(A.column, cols)
//        return A.slice(remRows, remCols)
//    }


    private fun <T> colVectors(A: Matrix<T>): List<Vector<T>> {
        return A.colIndices.map { j ->
            AVector(A.row) { i -> A[i, j] }
        }
    }

    /**
     * Returns the kronecker product `C = A ⊗ B` of `A` and `B`.
     * The result matrix `C` has the shape `(A.row * B.row, A.column * B.column)` and its elements are computed as:
     * ```
     * C[i1 * B.row + i2, j1 * B.column + j2] = A[i1, j1] * B[i2, j2]
     * ```
     * Alternatively, `C` can be expressed in the block matrix form:
     * ```
     * C = [A[i,j] * B], where i = 0 until A.row, j = 0 until A.column
     * ```
     *
     * We have the following properties:
     * - **Associativity** - `(A ⊗ B) ⊗ C = A ⊗ (B ⊗ C)`
     * - **Distributivity** - `A ⊗ (B + C) = A ⊗ B + A ⊗ C`
     * - Mixed with matrix multiplication: `(A ⊗ B)(C ⊗ D) = (AC) ⊗ (BD)`
     */
    fun <T> kronecker(A: Matrix<T>, B: Matrix<T>, model: Ring<T>): AMatrix<T> {
        val res = zero(A.row * B.row, A.column * B.column, model)
        for (i in 0 until A.row) {
            for (j in 0 until A.column) {
                for (i2 in 0 until B.row) {
                    for (j2 in 0 until B.column) {
                        res[i * B.row + i2, j * B.column + j2] = model.eval { A[i, j] * B[i2, j2] }
                    }
                }
            }
        }
        return res
    }

    /*
    Statistics
     */
    fun <T> trace(A: Matrix<T>, model: AddSemigroup<T>): T {
        require(A.isSquare)
        var res = A[0, 0]
        for (i in 1 until A.row) {
            res = model.add(res, A[i, i])
        }
        return res
    }

    fun <T> diag(A: Matrix<T>): AVector<T> {
        require(A.isSquare)
        return AVector(A.row) { i -> A[i, i] }
    }

    fun <T> sumAll(A: Matrix<T>, model: AddSemigroup<T>): T {
        return A.elementSequence().reduce(model::add)
    }

    /*
    Inverse
     */

    fun <T> rank(A: Matrix<T>, model: Field<T>): Int {
        val pivots = toEchelon(AMatrix.copyOf(A), model)
        return pivots.size
    }

    fun <T> columnSpace(A: Matrix<T>, model: Field<T>): VectorSpace<T> {
        val mutable = AMatrix.copyOf(A)
        val pivots = toUpperTriangle(mutable, model)
        val indepVectors = pivots.map { mutable.colAt(it) }
        return VectorSpace.fromBasis(A.row, model, indepVectors)
    }


    fun <T> spanOf(vectors: List<Vector<T>>, vecLength: Int, model: Field<T>): VectorSpace<T> {
        if (vectors.isEmpty()) return VectorSpace.zero(vecLength, model)
        val mutable = AMatrix(vecLength, vectors.size) { i, j -> vectors[j][i] }
        val pivots = toUpperTriangle(mutable, model)
        val indepVectors = pivots.map { mutable.colAt(it) }
        return VectorSpace.fromBasis(model, indepVectors)
    }

    /**
     * Computes the 'inverse' of the given matrix over a unit ring. This method simply compute the adjugate matrix and
     * divide it with the determinant (so it is time-consuming).
     *
     * This method can be used to compute the modular inverse of a matrix on `Z/Zn`, where n is not necessarily a prime.
     */
    fun <T> inverseInRing(M: Matrix<T>, model: UnitRing<T>): AMatrix<T> {
        val (p, adj) = charPolyAndAdj(M, model)
        val det = p.getOrNull(0).let { t -> if (t != null) t.value else model.zero }
        if (!model.isUnit(det)) throw ArithmeticException("The determinant is not invertible")
        adj.divAssign(det, model)
        return adj
    }


    private fun <T> buildAugmentedI(m: Matrix<T>, model: UnitRing<T>): AMatrix<T> {
        val n = m.row
        val aug = zero(n, 2 * n, model)
        aug.setAll(0, 0, m)
        val one = model.one
        for (i in 0 until n) {
            aug[i, i + n] = one
        }
        return aug
    }

    fun <T> inverseInField(m: Matrix<T>, model: Field<T>): AMatrix<T> {
        val n = m.row
        val aug = buildAugmentedI(m, model)
        val pivots = toEchelon(aug, model, column = n)
        if (pivots.size != n) {
            throw ArithmeticException("The matrix is not invertible")
        }
        return AMatrix.copyOf(aug.subMatrix(0, n, n, 2 * n))
    }

//    /**
//     * Computes the inverse of the matrix on an Euclidean domain.
//     *
//     * It is required that the calculator of `M` is an instance of EUDCalculator.
//     */
//    fun <T> inverseInEUD(M: Matrix<T>, mc: EuclideanDomain<T>): Matrix<T> {
//        //TODO re-implement this method
//        M.requireSquare()
//        val n = M.column
//
//        val A =
////        Printer.printMatrix(A)
//        // to upper triangle
//        for (j in 0 until n) {
//            var i = j
//            while (mc.isZero(A[i, j]) && i < n) {
//                i++
//            }
//            if (i == n) throw ArithmeticException("The matrix is not invertible")
//            if (i != j) A.swapRow(i, j)
//
//            i++
//            outer@
//            while (true) {
//                val p = A[j, j]
//                while (i < n) {
//                    // gcd
//                    val (q, r) = mc.divideAndRemainder(A[i, j], p)
//                    A.multiplyAddRow(j, i, mc.negate(q), j)
////                    Printer.printMatrix(A)
//                    if (mc.isZero(r)) {
//                        i++
//                        continue
//                    }
//                    A.swapRow(j, i, j)
//                    continue@outer
//                }
//                if (!mc.isUnit(p)) {
//                    throw ArithmeticException("The matrix is not invertible")
//                }
//                A[j, j] = mc.one
//                A.divideRow(j, p, j + 1)
//                break
//            }
//
//        }
//
//        for (j1 in (n - 1) downTo 1) {
//            for (j2 in 0 until j1) {
//                val k = mc.negate(A[j2, j1])
//                A.multiplyAddRow(j1, j2, k, j1)
//            }
//        }
//        return A.subMatrix(0, n, A.row, A.column)
//    }


    fun <T> inverse(m: Matrix<T>, model: UnitRing<T>): AMatrix<T> {
        require(m.isSquare)
        return when (model) {
            is Field -> inverseInField(m, model)
//            else -> throw UnsupportedOperationException("The inverse of a matrix is not supported for the given model: $model")
            else -> inverseInRing(m, model)
        }
    }

    fun <T> isInvertible(m: Matrix<T>, model: UnitRing<T>): Boolean {
        require(m.isSquare)
        return model.isUnit(det(m, model))
    }

    fun <T> detSmall(m: Matrix<T>, model: Ring<T>): T {
        require(m.isSquare)
        return when (m.row) {
            1 -> m[0, 0]
            2 -> model.eval {
                m[0, 0] * m[1, 1] - m[0, 1] * m[1, 0]
            }

            3 -> model.eval {
                m[0, 0] * m[1, 1] * m[2, 2] + m[0, 1] * m[1, 2] * m[2, 0] + m[0, 2] * m[1, 0] * m[2, 1] -
                        m[0, 0] * m[1, 2] * m[2, 1] - m[0, 1] * m[1, 0] * m[2, 2] - m[0, 2] * m[1, 1] * m[2, 0]
            }

            else -> throw IllegalArgumentException("The matrix is too large")
        }
    }

    /**
     * A very time-consuming method to compute the determinant of a matrix by the definition:
     *
     *     det(A) = \sum_{σ ∈ S_n} (-1)^σ \prod_{i=1}^n A_{i, σ(i)}
     *
     * This method is provided to test the correctness of other determinant algorithms.
     */
    fun <T> detDefinition(m: Matrix<T>, model: Ring<T>): T {
        require(m.isSquare)
        var result = model.zero
        val n = m.row
        for ((idx, rev) in IterUtils.permRev(n, copy = false)) {
            var t = m[0, idx[0]]
            for (i in 1 until n) {
                t = model.eval { t * m[i, idx[i]] }
            }
            result = if (rev % 2 == 0) {
                model.add(result, t)
            } else {
                model.subtract(result, t)
            }
        }
        return result
    }

    /**
     * Computes the determinant of the given matrix using the Gauss-Bareiss algorithm.
     *
     * Complexity: `O(n^3)`
     */
    fun <T> detGaussBareiss(matrix: Matrix<T>, model: UnitRing<T>): T {
        // Created by lyc at 2020-03-05 19:18
        // Reimplemented by lyc at 2024-09-06
        /*
        Refer to 'A Course in Computational Algebraic Number Theory' Algorithm 2.2.6

        Explanation of the algorithm:
        We still use the primary transformation to eliminate elements in the matrix, but here we store the potential
        denominator and divide them only when necessary.

        Recall the vanilla elimination process, assuming the element at (k,k) is non-zero, we multiply a factor to
        the first row and subtract it from i-th row. The factor is equal to m[i,k] / m[k,k]. This row transformation
        will affect the i-th row, changing it element m[i,j] to m[i,j] - m[k,j] * m[i,k] / m[k,k].
        However, since the division m[i,k] / m[k,k] may not be exact, so we multiply m[k,k] to all rows i > k,
        which results in (m[k,k])^{n-k-1} in the denominator that we keep in mind.
        Then, the resulting element is just
            m[i,j]' = m[i,j] * m[k,k] - m[k,j] * m[i,k] = det([m[k,k], m[k,j]; m[i,k], m[i,j]])
        Indeed, we can keep all the denominators in mind and divide them at the end of the process, but we can improve it.
        In fact, for the (k+1)-th loop, we will encounter also the term det(X) = det([m[k+1,k+1]', m[k+1,j]'; m[i,k+1]', m[i,j]']).
        Now, by the update in the last loop, we have X = m[k,k] X0 - X1, where
        X0 = [ m[k+1,k+1], m[k+1,j]; m[i,k+1], m[i,j] ],
        X1 = [m[k,k+1] * m[k+1,k], m[k,j] * m[k+1,k];
              m[k,k+1] * m[i,  k], m[k,j] * m[i,  k]].
        Now, if det(B) = 0, det(rA + B) can be divided by r (using the definition of determinant).
        Also, it is easy to see that det(X1) = 0, and thus det(X) can be divided by m[k,k].
        Hence, for the (k+1)-th loop, for each row j > k+1, we can divide the resulting element by m[k,k],
        cancelling out (m[k,k])^{n-k-2} and leaving on one m[k,k] in the final det.

        Finally, we should divide the final det by m[k,k] for all k < n-1.
        Since det is the product of diagonal elements, we can just leave those m[k,k] and return m[n-1,n-1].
         */
        val mat = AMatrix.copyOf(matrix)
        val n: Int = mat.row
        var d = model.one // the denominator that we store
        var positive = true
        for (k in 0..<n) {
            //locate the top-left element used for elimination first, it must be non-zero
            if (model.isZero(mat[k, k])) {
                val r = (k + 1..<n).firstOrNull { i -> !model.isZero(mat[i, k]) } ?: return model.zero
                mat.swapRow(r, k, k) // swap i-th row and k-th row, changing the sign of the determinant
                positive = !positive
            }
            val p: T = mat[k, k]
            for (i in k + 1..<n) {
                val head = mat[i, k]
                for (j in k + 1..<n) { // j<=k are all zero, just ignore them
                    mat[i, j] = model.eval {
                        exactDiv(p * mat[i, j] - head * mat[k, j], d)
                    }
                }
            }
            d = p
        }
        val det = mat[n - 1, n - 1]
        return if (positive) det else model.negate(det)
    }

    fun <T> det(mat: Matrix<T>, mc: Ring<T>): T {
        require(mat.isSquare)
        if (mat.row <= 3) return detSmall(mat, mc)
        if (mc is UnitRing) {
            try {
                return detGaussBareiss(mat, mc)
            } catch (_: UnsupportedOperationException) {
                // fall back to the definition if exact division is not supported
            }
        }
        System.err.println(
            "Warning: Trying to compute the det of a matrix using the definition, which is very time-consuming."
        )
        return detDefinition(mat, mc)
    }

    fun <T> cofactor(m: Matrix<T>, i: Int, j: Int, model: Ring<T>): T {
        val t = det(m.minor(i, j), model)
        return if ((i + j) % 2 == 0) t else model.negate(t)
    }


    /**
     * Gets the characteristic polynomial of the given matrix, which is defined as `det(xI - A)`,
     * and the adjugate matrix `A^*`.
     */
    fun <T> charPolyAndAdj(matrix: Matrix<T>, mc: UnitRing<T>): Pair<Polynomial<T>, AMatrix<T>> {
        /* Written by Ezrnest at 2024/9/12
        f(x) = det(xI - A) = \sum a_r x^r = \prod (x - λ_i), a_n = 1, a_{n-1} = -Tr(A),..., a_0 = (-1)^n det(A)
        Let us denote by α a multi-index with distinct elements and λ^α = λ_{α_1} λ_{α_2} ... λ_{α_k}.
        Then, a_{n-k} = (-1)^k \sum_{|α| = k} λ^α.
        We recall the fact: Tr(A) = \sum λ_i,  Tr(g(A)) = \sum g(λ_i).
        We aim to obtain matrices A_k = g_k(A) with
            λ_i(A_k) = (-1)^k \sum_{|α| = k, i ∈ α} λ^α, => Tr(A_k) = k a_{n-k} (since |α| = k and each 'i' appears k times)
        Let us deduce the recursive formula for A_k: A_1 = -A
        λ_i(A_{k+1}) = (-1)^{k+1} \sum_{|α| = k+1, i ∈ α} λ^α = (-1)^{k+1} \sum_{|α| = k, i ∉ α} λ^α λ_i
                     = (-1)^{k+1} (\sum_{|α| = k} λ^α - \sum_{|α| = k, i ∈ α} λ^α) λ_i
                     = (- a_{n-k} + λ_i(A_k)) λ_i
        Therefore, we have the recursive formula:
            A_{k+1} = (A_k - a_{n-k} I) A, a_{n-k} = Tr(A_k) / k
            g_{k+1}(x) = (-a_{n-k} + g_k(x)) x
        Now, the recursive formula of g_k actually shows that g_{k+1} = -(a_{n-k} + a_{n-k+1} x + ... + a_n x^{n-k}) x
        Remark: The recurrence relation is also related to the Newton's identities.

        Now, for the adjugate A^*, we have A^* A = det(A) I, while f(A) = a_0 + a_1 A + ... + a_n A^n = 0, a_0 = (-1)^n det(A)
        so A (a_1 + a_2 A + ... + a_n A^{n-1}) = (-1)^{n+1} det(A) I, namely
        A^* = (-1)^{n+1} (a_1 + a_2 A + ... + a_n A^{n-1}) = (-1)^n (-a_1 + g_{n-1}(x))
         */
        require(matrix.isSquare)
        val n = matrix.row
        if (n == 1) return mc.eval { Polynomial.fromList(mc, listOf(-matrix[0, 0], one)) to identity(1, mc) }
        val A = matrix
        val aList = ArrayList<T>(n + 1) // a list of coefficients in reverse order
        aList += mc.one // a_n = 1

        var k = 1
        var Ak = negate(A, mc)
        var Bk: AMatrix<T> = Ak // B_{k+1} = A_k - a_{n-k} I, the initial value is discarded
        while (true) {
            val tr = trace(Ak, mc)
            val ak = mc.exactDiv(tr, k.toLong())
            aList += ak
            if (k == n) break
            Bk = Ak
            addDiagonal(Bk, mc.negate(ak), mc)
            Ak = matmul(Bk, A, mc)
            k++
        }
        val f = Polynomial.fromList(mc, aList.asReversed())
        val adj = if (n % 2 == 0) Bk else negate(Bk, mc)
        return f to adj
    }


    /**
     * Computes the adjugate matrix of the given matrix by the definition:
     *
     *     adj(A) = C^T, where C_{i,j} = (-1)^(i+j) det(A_{i,j})
     */
    fun <T> adjugateDefinition(matrix: Matrix<T>, mc: Ring<T>): AMatrix<T> {
        require(matrix.isSquare)
        val n = matrix.row
        val A = AMatrix.copyOf(matrix)
        val adj = AMatrix(n, n) { i, j -> cofactor(A, i, j, mc) }
        return adj
    }

    fun <T> adjugate(matrix: Matrix<T>, mc: Ring<T>): AMatrix<T> {
        if (mc is UnitRing) {
            return charPolyAndAdj(matrix, mc).second
        }
        return adjugateDefinition(matrix, mc)
    }

    fun <T> charPolyDefinition(matrix: Matrix<T>, mc: UnitRing<T>): Polynomial<T> {
        require(matrix.isSquare)
        val polyRing = Polynomial.over(mc)
        val n = matrix.row
        // det(xI - A)
        val matPoly = AMatrix(n, n) { i, j ->
            polyRing.constant(mc.negate(matrix[i, j]))
        }
        addDiagonal(matPoly, polyRing.x, polyRing)
        return detGaussBareiss(matPoly, polyRing)
    }

    fun <T> charPoly(matrix: Matrix<T>, mc: UnitRing<T>): Polynomial<T> {
//        return charPolyDefinition(matrix, mc) //
        return charPolyAndAdj(matrix, mc).first //
    }

    /*
    Decomposition
     */
    fun <T> decompRank(x: Matrix<T>, model: Field<T>): Pair<Matrix<T>, Matrix<T>> {
        val m = AMatrix.copyOf(x)
        val pivots = toEchelon(m, model)
        val L = AMatrix(x.row, pivots.size) { i, j -> x[i, pivots[j]] }
        val R = m.subMatrix(0, 0, pivots.size, m.column)
        return L to R
    }


    private fun <T> decompQR0(A: Matrix<T>, mc: Reals<T>): Pair<AMatrix<T>, AMatrix<T>> {
        //Re-written by lyc at 2021-04-30 13:00
        require(A.isSquare) { "Only square matrix is supported for QR decomposition." }
        val vs = colVectors(A)
        val R = zero(A.row, A.column, mc)
        val ws = ArrayList<Vector<T>>(A.row)
        val Q = zero(A.row, A.column, mc)
        val vectors = Vector.over(mc, A.row)
        with(vectors) {
            for (i in 0..<A.row) {
                val u = MutableVector.copyOf(vs[i])
                for (j in 0 until i) {
                    val k = u dot ws[j]
                    u.minusAssignTimes(k, ws[j])
                    R[j, i] = k
                }
                if (!isZero(u)) {
                    val length = u.norm()
                    R[i, i] = length
                    u.divAssign(length)
                }
                ws += u
            }
        }

//        val Q = Matrix.fromColumns(ws)
        return Q to R
    }


    /**
     * Returns the QR-decomposition of a square matrix `A = QR`, where `Q` is an orthogonal matrix and `R` is an
     * upper-triangle matrix. If this matrix is invertible, there is only one decomposition.
     *
     * @return `(Q, R)` as a pair
     */
    fun <T> decompQR(A: Matrix<T>, model: Reals<T>): Pair<AMatrix<T>, AMatrix<T>> {
        return decompQR0(A, model)
    }

    /**
     *
     */
    fun <T> decompKAN(A: Matrix<T>, model: Reals<T>): Triple<AMatrix<T>, Vector<T>, AMatrix<T>> {
        //Created by lyc at 2021-05-11 20:25
        //TODO: re-implement this method for general cases
        val (Q, R) = decompQR0(A, model)
        val d = R.diag()
        val one = model.one
        for (i in 0 until R.row) {
            R[i, i] = one
            R.divRow(i, d[i], i + 1, model = model)
        }
        return Triple(Q, d, R)
    }

    private fun <T> checkSymmetric(A: Matrix<T>, mc: EqualPredicate<T>) {
        require(A.isSquare) {
            "The given matrix is not symmetric!: $A"
        }
        for (i in 0..<A.row) {
            for (j in 0..<i) {
                require(mc.isEqual(A[i, j], A[j, i])) { "The given matrix is not symmetric!: $A" }
            }
        }
    }


    /**
     * Computes the PLU decomposition of a square matrix `A`, returning `(P, L, U)` such that
     *
     * ```
     * P * A = L * U
     * ```
     *
     * where `P` is a permutation matrix, `L` is lower triangular with unit diagonal, and `U` is upper triangular.
     */
    fun <T> decompPLU(m: Matrix<T>, mc: Field<T>): PLUDecompositionResult<T> {
        require(m.isSquare) {
            "The matrix must be square!"
        }
        val n = m.row
        val upper = AMatrix.copyOf(m)
        val lower = zero(n, n, mc)
        val permutation = identity(n, mc)
        val pivots = mutableListOf<Int>()

        for (k in 0 until n) {
            val pivotRow = (k until n).firstOrNull { !mc.isZero(upper[it, k]) } ?: continue

            if (pivotRow != k) {
                upper.swapRow(k, pivotRow)
                permutation.swapRow(k, pivotRow)
                if (k > 0) {
                    lower.swapRow(k, pivotRow, 0, k)
                }
            }

            lower[k, k] = mc.one
            pivots += k
            for (i in (k + 1) until n) {
                val lambda = mc.eval {
                    upper[i, k] / upper[k, k]
                }
                lower[i, k] = lambda
                upper[i, k] = mc.zero
                upper.mulAddRow(k, i, mc.negate(lambda), k + 1, model = mc)
            }
        }
        return PLUDecompositionResult(permutation, lower, upper, pivots)
    }

    /**
     * Computes the LU decomposition of the given matrix `A` , returns a pair of matrices `(L,U)` such that
     * `A = LU`, where `L` is lower triangular with `1` as diagonal elements and `U` is upper triangular.
     *
     * It is required that the matrix is invertible.
     *
     * **Note**: This method is not designed for numerical computation.
     *
     * @return
     */
    fun <T> decompLU(m: Matrix<T>, mc: Field<T>): Pair<Matrix<T>, Matrix<T>> {
        require(m.isSquare) {
            "The matrix must be square!"
        }
        val n = m.row
        val upper = AMatrix.copyOf(m)
        val lower = zero(n, n, mc)
        for (k in 0 until m.row) {
            lower[k, k] = mc.one
            for (i in (k + 1) until m.row) {
                val lambda = mc.eval {
                    upper[i, k] / upper[k, k]
                }
                lower[i, k] = lambda
                upper[i, k] = mc.zero
                upper.mulAddRow(k, i, mc.negate(lambda), k + 1, model = mc)
            }
        }
        return lower to upper
    }

    /**
     * Decomposes a symmetric semi-positive definite matrix `A = L L^T`, where
     * `L` is a lower triangular matrix.
     *
     * @return a lower triangular matrix `L`.
     */
    fun <T> decompCholesky(A: Matrix<T>, mc: Reals<T>): Matrix<T> {
        require(A.isSquare) {
            "The matrix must be square!"
        }
        val n = A.row

        val L = zero(n, n, mc)
        for (j in 0 until n) {
            var t = A[j, j]
            for (k in 0 until j) {
                t = mc.eval { t - L[j, k] * L[j, k] }
            }
            t = mc.sqrt(t)
            L[j, j] = t
            // l_{jj} = sqrt(a_{jj} - sum(0,j-1, l_{jk}^2))
            for (i in (j + 1) until n) {
                var a = A[i, j]
                for (k in 0 until j) {
                    a = mc.eval { a - L[i, k] * L[j, k] }
                }
                a = mc.eval { a / t }
                L[i, j] = a
                // l_{ij} = (a_{ij} - sum(0,j-1,l_{il}l_{jl}))/l_{jj}
            }
        }
        return L
    }

    /**
     * Returns the LDL decomposition of the given positive definite matrix:
     * ```
     * A = L D L.T
     * ```
     * where
     * - `L` is a lower triangular matrix whose diagonal elements are all `1`;
     * - `D` is a diagonal matrix with positive diagonal elements.
     *
     * @return `(L, diag(D))`, where `L` is a lower triangular matrix, `diag(D)` is a vector of diagonal elements
     * of `D`.
     */
    fun <T> decompLDL(A: Matrix<T>, mc: Field<T>): Pair<Matrix<T>, Vector<T>> {
        checkSymmetric(A, mc)
        val n = A.row

        val L = zero(n, n, mc)
        val d = ArrayList<T>(n)

        for (j in 0 until n) {
            var t = A[j, j]
            for (k in 0 until j) {
                t = mc.eval { t - L[j, k] * L[j, k] * d[k] }
            }

            d += t
            // d_j = a_{jj} - sum(0,j-1, l_{jk}^2)
            L[j, j] = mc.one
            // l_{jj} = a_{jj} - sum(0,j-1, l_{jk}^2)
            for (i in (j + 1) until n) {
                var a = A[i, j]
                for (k in 0 until j) {
                    a = mc.eval { a - L[i, k] * L[j, k] * d[k] }
                }
                L[i, j] = mc.eval { a / t }
                // l_{ij} = (a_{ij} - sum(0,j-1,d_k * l_{ik}l_{jk})) / d_j
            }
        }
        return L to Vector.of(d)
    }


    /*
    Normal forms
     */


    /**
     * Transforms the given matrix `M` over a field to an upper triangular form using row operations (Gaussian elimination).
     *
     *
     * @return a list of strictly increasing pivots of the column. The size of it is equal to the rank of the matrix.
     */
    fun <T> toUpperTriangle(M: AMatrix<T>, model: Field<T>, column: Int = M.column): List<Int> {
        //Created by lyc at 2021-04-29
        val row = M.row
        var r = 0
        val pivots = ArrayList<Int>(min(M.row, column))
        val zero = model.zero
        /*
        c = pivots[r] then M[r,c] is the first non-zero element in that row
         */
        for (c in 0 until column) {
            if (r >= row) break
            val rStart = (r..<row).firstOrNull { !model.isZero(M[it, c]) } ?: continue
            if (rStart != r) {
                M.swapRow(r, rStart)
            }
            val f = M[r, c]
            for (i in (rStart + 1)..<row) {
                if (model.isZero(M[i, c])) continue
                val k = model.eval { -M[i, c] / f }
                M[i, c] = zero
                M.mulAddRow(r, i, k, c + 1, model = model)
            }
            pivots += c
            r++
        }
        return pivots
    }

    /**
     *
     * @return a list of strictly increasing pivots of the column. The size of it is equal to the rank of the matrix.
     */
    fun <T> toEchelon(M: AMatrix<T>, mc: Field<T>, column: Int = M.column): List<Int> {
        //Created by lyc at 2021-04-29
        val pivots = toUpperTriangle(M, mc, column)
        val zero = mc.zero
        for (i in pivots.lastIndex downTo 0) {
            val j = pivots[i]
            if (!mc.isOne(M[i, j])) {
                M.divRow(i, M[i, j], j + 1, model = mc)
                M[i, j] = mc.one
            }
            for (k in (i - 1) downTo 0) {
                if (mc.isZero(M[k, j])) {
                    continue
                }
                val q = mc.eval { -M[k, j] }
                M.mulAddRow(i, k, q, j + 1, model = mc)
                M[k, j] = zero
            }
        }
        return pivots
    }

    /**
     * Transforms the given matrix `M` over a Euclidean domain to an upper triangular form.
     */
    internal fun <T> toUpperEUD0(M: AMatrix<T>, mc: EuclideanDomain<T>, column: Int = M.column): List<Int> {
        val row = M.row
        var r = 0
        val pivots = ArrayList<Int>(min(M.row, column))
        for (c in 0 until column) {
            if (r >= row) break
            if (reduceByRowEUD(M, mc, r, c) == 0) continue
            pivots += c
            r++
        }
        return pivots
    }


    internal fun <T> toHermitForm0(M: AMatrix<T>, mc: Integers<T>, column: Int = M.column): List<Int> {
        val pivots = toUpperEUD0(M, mc, column)
        for (i in pivots.lastIndex downTo 0) {
            val j = pivots[i]
            if (mc.isNegative(M[i, j])) {
                M.negateRow(i, model = mc)
            }
            val d = M[i, j]
            for (k in (i - 1) downTo 0) {
                if (mc.isZero(M[k, j])) {
                    continue
                }
                var q = mc.eval { -divToInt(M[k, j], d) }
                if (mc.isNegative(M[k, j])) {
                    q = mc.increase(q)
                }
                M.mulAddRow(i, k, q, j, model = mc)
            }
        }
        return pivots
    }


    /**
     * Transform this matrix to (row) Hermit Form.
     */
    fun <T> toHermitForm(A: Matrix<T>, mc: Integers<T>): Matrix<T> {
        val M = AMatrix.copyOf(A)
        toHermitForm0(M, mc)
        return M
    }

    /**
     * Transforms the matrix `M` over a Euclidean domain to its Smith Normal Form.
     * The Smith Normal Form is a diagonal matrix where the diagonal entries `d_i` satisfy:
     * * `d_i | d_{i+1}` for all `i`;
     * * `d_i` is unique up to multiplication by a unit.
     * * `d_i` is the greatest common divisor of all `i x i` minors of `M`.
     *
     *
     * The entries `d_i` are the invariant factors of the matrix.
     *
     * @param A The matrix to be transformed
     */
    fun <T> toSmithForm(A: Matrix<T>, mc: EuclideanDomain<T>): AMatrix<T> {
        val factors = invariantFactors(A, mc)
        // build a diagonal matrix
        val M = zero(A.row, A.column, mc)
        for (i in factors.indices) {
            M[i, i] = factors[i]
        }
        return M
    }

    /**
     * Computes the **non-zero** invariant factors of the given matrix `A` over a Euclidean domain.
     *
     * The `i`-th invariant factors are the greatest common divisors of all `i x i` minors of the matrix.
     */
    fun <T> invariantFactors(A: Matrix<T>, mc: EuclideanDomain<T>): List<T> {
        return toSmithForm0(AMatrix.copyOf(A), mc)
    }


    fun <T> detDivisors(A: Matrix<T>, mc: EuclideanDomain<T>): List<T> {
        val invFactors = invariantFactors(A, mc)
        // d_k = a_k * a_{k-1} * ... * a_1
        val result = ArrayList<T>(invFactors.size)
        var d = mc.one
        for (i in invFactors.indices) {
            d = mc.multiply(d, invFactors[i])
            result += d
        }
        return result
    }

    /**
     * Transforms the given matrix `M` over a Euclidean domain to its Smith Normal Form.
     * Returns a list of invariant factors (non-zero diagonal entries).
     *
     * @param M The matrix to be transformed (must implement `MutableMatrix<T>`).
     * @param mc The Euclidean domain calculator for the matrix entries.
     * @return List of invariant factors (non-zero diagonal entries) of the Smith Normal Form.
     */
    internal fun <T> toSmithForm0(M: AMatrix<T>, mc: EuclideanDomain<T>): List<T> {
        val rows = M.row
        val cols = M.column
        val invariants = mutableListOf<T>()
        var c = 0

        for (r in 0..<rows) {
            if (c < cols && reduceByRowEUD(M, mc, r, c) == 0) {
                // the elements in the column are all zero, move to the next column
                c++
            }
            if (c >= cols) break // all columns are reduced
            // M[r,c] is non-zero and the elements below it are all zero
            while (reduceByColEUD(M, mc, r, c, zeroed = true) == 2) {
                // the column is changed, so we have to reduce the by row again
                reduceByRowEUD(M, mc, r, c)
            }
            // now we have to reduce the rest of the elements with i >= r, j > c
            for (i in r..<rows) {
                for (j in (c + 1)..<cols) {
                    val (g, _, v) = mc.gcdUV(M[r, c], M[i, j])
                    if (mc.isZero(v)) continue // M[r,c] | M[i,j], continue
                    // make a gcd at M[r,j] by row and col trans
                    // then swap col c and j
                    M.mulAddRow(i, r, v, j + 1, model = mc)
                    // col trans can be omitted since elements below M[r,c] are all zero
                    M.swapCol(c, j, r + 1)
                    M[r, j] = mc.negate(M[r, c]) // negate so det is not changed by swapping
                    M[r, c] = g
                    // clean up the elements in the column c and row r
                    do {
                        reduceByRowEUD(M, mc, r, c)
                    } while (reduceByColEUD(M, mc, r, c, zeroed = true) == 2)
                }
            }
            invariants += M[r, c]
            c++
        }

        return invariants
    }

    /**
     *
     * @return 0 if the row is all zero, 1 if any row is reduced, 2 if the original row is changed.
     */
    private fun <T> reduceByRowEUD(
        M: AMatrix<T>, mc: EuclideanDomain<T>, r: Int, c: Int, zeroed: Boolean = false, colStart: Int = c + 1
    ): Int {
        return templateReduceInEUD(
            mc, M.row, r, c, zeroed, colStart, M::get, M::set,
            M::swapRow,
            mMulAddRow = { r1, r2, k, col -> M.mulAddRow(r1, r2, k, col, model = mc) },
            mMulRow = { r0, k, col -> M.mulRow(r0, k, col, model = mc) },
            mTransformRows = { r1, r2, a11, a12, a21, a22, col ->
                M.transformRows(r1, r2, a11, a12, a21, a22, col, model = mc)
            }
        )
    }


    private fun <T> reduceByColEUD(
        M: AMatrix<T>, mc: EuclideanDomain<T>, r: Int, c: Int, zeroed: Boolean = false, rowStart: Int = r + 1
    ): Int {
        return templateReduceInEUD(
            mc, M.column, c, r, zeroed, rowStart,
            mGet = { i, j -> M[j, i] },
            mSet = { i, j, v -> M[j, i] = v },
            M::swapCol,
            mMulAddRow = { r1, r2, k, col -> M.mulAddCol(r1, r2, k, col, model = mc) },
            mMulRow = { r0, k, col -> M.mulCol(r0, k, col, model = mc) },
            mTransformRows = { r1, r2, a11, a12, a21, a22, col ->
                M.transformCols(r1, r2, a11, a12, a21, a22, col, model = mc)
            }
        )
    }

    private inline fun <T> templateReduceInEUD(
        mc: EuclideanDomain<T>,
        rowEnd: Int, r: Int, c: Int,
        zeroed0: Boolean, colStart: Int,
        mGet: (Int, Int) -> T, mSet: (Int, Int, T) -> Unit,
        mSwapRow: (Int, Int, Int) -> Unit,
        mMulAddRow: (Int, Int, T, Int) -> Unit, mMulRow: (Int, T, Int) -> Unit,
        mTransformRows: (Int, Int, T, T, T, T, Int) -> Unit
    ): Int {
        val rStart = (r until rowEnd).firstOrNull { !mc.isZero(mGet(it, c)) } ?: return 0
        if (rStart != r) {
            mSwapRow(r, rStart, c)
        }
        // the rows in (r, rStart] are all zero
        var res = 1
        var zeroed = zeroed0
        for (i in (rStart + 1) until rowEnd) {
            val b = mGet(i, c)
            if (mc.isZero(b)) continue
            val h = mGet(r, c)
            val (gcd, u, v, hd, bd) = mc.gcdExtendedFull(h, b)
            // uh + vb = gcd(h, b)
            // uni-modular transform: [u, v; -b/d, h/d], det = (au - bv)/d = 1
            // M[r] = u M[r] + v M[i] = gcd(a,b), M[r,c] = gcd
            // M[i] = -b/d M[r] + a/d M[i], M[i,c] = 0
            if (mc.isZero(v)) {
                // h | b, so u = 1, v = 0, hd = 1, bd = b/d = b/h
                // just to optimize the computation: M[i] = M[i] - b/h M[r], while M[r] is not changed
                if (!zeroed) {
                    mMulAddRow(r, i, mc.negate(bd), colStart)
                }
            } else {
                if (zeroed) {
                    mMulAddRow(i, r, v, colStart) // M[r] = u M[r] + v M[i] = v M[i]
                    mMulRow(i, hd, colStart) // M[i] = hd M[i] - bd M[r] = hd M[i]
                } else {
                    mTransformRows(r, i, u, v, mc.negate(bd), hd, colStart)
                }
                res = 2
                zeroed = false
            }
            mSet(r, c, gcd)
            mSet(i, c, mc.zero)

        }
        return res
    }


    /**
     * Transforms a symmetric matrix `A` into its congruence diagonal normal form `Λ` and computes the transformation matrix `P`
     * such that:
     * ```
     *     P * A * P.T = Λ
     * ```
     * where `Λ` is a diagonal matrix and `P` is non-singular.
     *
     * The matrix `A` must be symmetric, and the matrix entries must be over a [Field].
     *
     * @return A pair `(Λ, P)` where `Λ` is the diagonal matrix and `P` is the transformation matrix.
     */
    fun <T> toCongDiagonalForm(A: Matrix<T>, mc: Field<T>): Pair<AVector<T>, AMatrix<T>> {
        //Re-written by lyc at 2021-04-30 13:00
        checkSymmetric(A, mc)
        val n = A.row
        // Create a matrix `x` of size (2n x n), where the first `n` rows are A and the next `n` rows form the identity matrix
        val aug = buildAugmentedI(A, mc)
        for (pos in 0..<n) {
            // Ensure the diagonal element at `pos, pos` is non-zero
            var pi = -1
            var pj = -1
            Outer@
            for (i in pos..<n) {
                for (j in i..<n) {
                    if (mc.isZero(aug[i, j])) continue
                    pi = i
                    pj = j
                    break@Outer
                }
            }
            if (pi == -1) break
            if (pj != pos) {
                aug.addRowTo(pj, pos, model = mc)
                aug.addColTo(pj, pos, model = mc)
            }
            if (pi != pos) {
                aug.addRowTo(pi, pos, model = mc)
                aug.addColTo(pi, pos, model = mc)
            }

            // Reduce all elements in the current row and column (except the diagonal) to zero
            val diagElement = aug[pos, pos]
            for (i in pos + 1 until n) {
                if (mc.isZero(aug[pos, i])) continue
                val k = mc.eval { -aug[pos, i] / diagElement }
                aug.mulAddRow(pos, i, k, model = mc)
                aug.mulAddCol(pos, i, k, model = mc)
            }
        }

        // Extract the diagonal matrix `Λ` and the transformation matrix `P`
        val lambda = AVector(n) { aug[it, it] }
        val p = AMatrix.copyOf(aug.subMatrix(0, n, n, 2 * n))
        return lambda to p
    }


    fun <T> toHessenberg(matrix: Matrix<T>, mc: Field<T>): Matrix<T> {
        require(matrix.isSquare)
        val H = AMatrix.copyOf(matrix)
        val n = matrix.row

        for (m in 0 until (n - 1)) {
            val i0 = (m + 2 until n).firstOrNull { !mc.isZero(H[it, m]) } ?: continue
            if (mc.isZero(H[m + 1, m])) {
                H.swapRow(i0, m + 1, m)
                H.swapCol(i0, m + 1)
            }

            val t = H[m + 1, m]
            for (i in (m + 2) until n) {
                if (mc.isZero(H[i, m])) continue
                val u = mc.eval { H[i, m] / t }
                H.mulAddRow(m + 1, i, mc.negate(u), m, model = mc)
                H[i, m] = mc.zero
                H.mulAddCol(i, m + 1, mc.reciprocal(u), model = mc)
            }
        }
        return H

    }


    /*
    Solve linear equations
     */


    private fun <T> nullSpaceOf(
        expanded: Matrix<T>, column: Int, pivots: List<Int>, mc: Field<T>
    ): VectorSpace<T> {

        val k = column - pivots.size
        if (k == 0) return VectorSpace.zero(column, mc)

        val basis = ArrayList<Vector<T>>(k)
        val minusOne = mc.negate(mc.one)
        var pivotIndex = 0

        for (j in 0 until column) {
            if (pivotIndex < pivots.size && pivots[pivotIndex] == j) {
                pivotIndex++
            } else {
                val v = Vector.zero(column, mc).apply { this[j] = minusOne }
                pivots.forEachIndexed { i, pivot -> v[pivot] = expanded[i, j] }
                basis += v
            }
        }
        return DVectorSpace(mc, column, basis)
    }

    private fun <T> specialSolutionOf(expanded: Matrix<T>, column: Int, pivots: List<Int>, mc: Field<T>): Matrix<T> {
        val special = zero(column, expanded.column - column, mc)
        for (k in pivots.indices) {
            val pk = pivots[k]
            for (j in special.colIndices) {
                special[pk, j] = expanded[k, j + column]
            }
        }
        return special
    }

    /**
     * Solve the linear equation `AX = B` with `expanded = (A, B)` in the augmented matrix form.
     * The column `colSep` is the column separating `A` and `B`, namely `A.column = colSep`.
     *
     */
    fun <T> solveLinear(augmented: AMatrix<T>, colSep: Int, mc: Field<T>):
            Pair<Matrix<T>?, VectorSpace<T>> {
        val pivots = toEchelon(augmented, mc, colSep)
        val r = pivots.size
        val solvable = (colSep until augmented.column).all { j ->
            (r until augmented.row).all { i ->
                mc.isZero(augmented[i, j])
            }
        }
        val basis = nullSpaceOf(augmented, colSep, pivots, mc)
        val special = if (solvable) specialSolutionOf(augmented, colSep, pivots, mc) else null
        return special to basis
    }

    fun <T> solveLinear(A: Matrix<T>, B: Matrix<T>, model: Field<T>): Pair<Matrix<T>, VectorSpace<T>>? {
        require(A.row == B.row)
        val aug = concatCol(A, B)
        val (special, basis) = solveLinear(aug, A.column, model)
        return if (special == null) null else special to basis
    }

    /**
     * Solves the linear equation `Ax = b` with the given matrix `A` and vector `b`.
     */
    fun <T> solveLinear(A: Matrix<T>, b: Vector<T>, model: Field<T>): LinearEquationSolution<T>? {
        require(A.row == b.size)
        val col = A.column
        val augmented = zero(A.row, col + 1, model)
        augmented.setAll(0, 0, A)
        augmented.setCol(col, b)
        val (special, basis) = solveLinear(augmented, col, model)
        return if (special == null) null else LinearEquationSolution(special.colAt(0), basis)
    }

    /**
     * Solves the homogeneous linear equation `Ax = 0` with the given matrix `A`.
     */
    fun <T> solveHomo(A: Matrix<T>, model: Field<T>): VectorSpace<T> {
        val expanded = AMatrix.copyOf(A)
        val pivots = toEchelon(expanded, model)
        return nullSpaceOf(expanded, A.column, pivots, model)
    }

}


//fun main() {
//    val Z = NumberModels.intAsIntegers()
//    val n = 4
//    val rng = Random(11)
//    val A = Matrix(n, Z) { i, j ->
//        rng.nextInt(10)
//    }
//    println(A)
//    println("Computing the invariant factors")
//    val invFactors = MatrixImpl.invariantFactors(A, Z)
//    println(invFactors)
//    val accProd = invFactors.scan(1) { acc, factor -> acc * factor }.drop(1)
//    println(accProd)
//    val gcds = mutableListOf<Int>()
//    for (k in 1..n) {
//        val rows = IterUtils.comb(A.row, k, false)
//        val cols = IterUtils.comb(A.column, k, false)
//        val minors = IterUtils.prod2(rows, cols).map { A.slice(it.first, it.second).det() }.toList().toIntArray()
//        val gcd = NTFunctions.gcd(*minors)
//        if (gcd == 0) {
//            break
//        }
//        gcds.add(gcd)
//    }
//    println(gcds)
//}

