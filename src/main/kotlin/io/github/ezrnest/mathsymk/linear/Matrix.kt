package io.github.ezrnest.mathsymk.linear

import io.github.ezrnest.mathsymk.linear.AMatrix.Companion.mulRow
import io.github.ezrnest.mathsymk.linear.Matrix.Companion.invoke
import io.github.ezrnest.mathsymk.model.Polynomial
import io.github.ezrnest.mathsymk.structure.*
import io.github.ezrnest.mathsymk.util.IterUtils
import kotlin.math.min

/**
 * Represents a matrix of elements of type [T] with shape [row] × [column].
 * The elements can be accessed by [get(i,j)][Matrix.get].
 *
 *
 * To create a matrix, you can use the constructor-like functions [Matrix.invoke] or other factory methods like [Matrix.zero].
 *
 * This interface only defines the **data structure** of a matrix and **does not** provide any mathematical operations like addition, multiplication, etc.
 * To operate on matrices, you should use the extension functions provided in the context returned by [Matrix.over],
 * which has many overloads for different models, such as [Ring] and [Field].
 *
 * Generally speaking, the applicable mathematical operations on matrices depend on the model of the elements in the matrix.
 * For example, matrices can be added over an [AddGroup] and matrix products can be computed over a [Ring].
 * Also, the identity matrix can be created with [eye(n)][MatOverURing.eye] over a [UnitRing].
 *
 * Here is a table of the basic operations on matrices over different models:
 *
 * | Model | Operations |
 * | --- | --- |
 * | [EqualPredicate] | Equality check |
 * | [AddMonoid] | Addition, [zero matrix][MatOverAddMonoid.zero] |
 * | [AddGroup] | Addition, subtraction, negation |
 * | [Ring] | [matrix multiplication][MatOverRing.matmul], [scalar multiplication][MatOverRing.scalarMul], [determinant][MatOverRing.det] |
 * | [UnitRing] | [identity matrix][MatOverURing.eye] |
 * | [Field] | [rank][MatOverField.rank], [kernel][MatOverField.kernel], [image][MatOverField.image] |
 *
 * @param T the type of the elements in the matrix, which must belong to a ring or a field.
 * @see Matrix.over
 * @see MutableMatrix
 * @see Vector
 */
interface Matrix<T> : GenTuple<T> {


    /**
     * The count of rows in this matrix.
     */
    val row: Int

    /**
     * The count of columns in this matrix.
     */
    val column: Int

    /**
     * The total count of elements in this matrix, which is equal to `row * column`.
     */
    override val size: Int
        get() = row * column

    /**
     * Gets the shape of this matrix: `(row, column)`.
     */
    val shape: Pair<Int, Int>
        get() = row to column

    /**
     * Gets the element at the `i`-th row and `j`-th column in this matrix.
     *
     * It is required that `0 <= i < row` and `0 <= j < column`.
     */
    operator fun get(i: Int, j: Int): T


    /**
     * Creates a new matrix of the same shape by applying the given function [mapping] to each element in this matrix.
     */
    override fun <S> map(mapping: (T) -> S): Matrix<S> {
        return MatrixImpl.apply1(this, mapping)
    }


    /**
     * Gets the elements in this generic matrix, iterating row first and then column as:
     * ```
     * for(i in 0 until row){
     *     for(j in 0 until column){
     *         yield(this[i, j])
     *     }
     * }
     * ```
     */
    override fun elementSequence(): Sequence<T> {
        return IterUtils.prodIdx(intArrayOf(row, column)).map { (i, j) -> this[i, j] }
    }


    /**
     * Gets the row at the given index as a vector.
     */
    fun rowAt(rowIdx: Int): Vector<T> {
        return Vector(column) { colIdx -> this[rowIdx, colIdx] }
    }

    /**
     * Gets the column at the given index as a vector.
     */
    fun colAt(colIdx: Int): Vector<T> {
        return Vector(row) { rowIdx -> this[rowIdx, colIdx] }
    }

    /**
     * Returns a list of row vectors.
     */
    fun rowVectors(): List<Vector<T>> {
        return rowIndices.map { rowAt(it) }
    }

    /**
     * Returns a list of column vectors.
     */
    fun colVectors(): List<Vector<T>> {
        return colIndices.map { colAt(it) }
    }


    /**
     * Gets the diagonal of this matrix as a vector.
     */
    fun diag(): Vector<T> {
        return MatrixImpl.diag(this)
    }


    /**
     * Returns a transposed view of this matrix.
     */
    fun transpose(): Matrix<T> {
        return TransposedMatrixView(this)
    }

    /**
     * Returns a transposed view of this matrix.
     */
    val T: Matrix<T>
        get() = transpose()


    /**
     * Gets a sub-matrix in this matrix as a view.
     *
     * @param rowStart the start index of the row, inclusive.
     * @param rowEnd the end index of the row, exclusive.
     * @param colStart the start index of the column, inclusive.
     * @param colEnd the end index of the column, exclusive.
     *
     */
    fun subMatrix(rowStart: Int = 0, colStart: Int = 0, rowEnd: Int = row, colEnd: Int = column): Matrix<T> {
//        require(0 <= rowStart && rowEnd <= row && rowStart < rowEnd)
//        require(0 <= colStart && colEnd <= column && colStart < colEnd)
        // checked in SubMatrixView
        return SubMatrixView(this, rowStart, rowEnd, colStart, colEnd)
    }

    /**
     * Returns a sliced matrix view of this matrix with the given row and column indices.
     */
    fun slice(rows: IntArray, cols: IntArray): Matrix<T> {
        return SlicedMatrixView(this, rows, cols)
    }

    /**
     * Returns the minor of this matrix at the position `(i, j)`,
     * namely the sub-matrix obtained by deleting the `i`-th row and `j`-th column.
     */
    fun minor(i: Int, j: Int): Matrix<T> {
        val rows = IntArray(this.row - 1) { t -> if (t < i) t else t + 1 }
        val cols = IntArray(this.column - 1) { t -> if (t < j) t else t + 1 }
        return slice(rows, cols)
    }

    /**
     * Returns sub-matrix obtained by deleting the given `rows` and `cols`.
     */
    fun minor(rows: IntArray, cols: IntArray): Matrix<T> {
        val remRows = remainingIndices(row, rows)
        val remCols = remainingIndices(column, cols)
        return slice(remRows, remCols)
    }


    companion object {
        /**
         * Creates a new matrix `A` with the given row and column count, the model and the initializer function [init],
         * such that `A[i, j] = init(i, j)`.
         */
        operator fun <T> invoke(row: Int, column: Int, init: (Int, Int) -> T): Matrix<T> {
            return AMatrix.of(row, column, init)
        }

        /**
         * Creates a new square matrix `A` with row and column count being `n`, the model and the initializer function [init],
         * such that `A[i, j] = init(i, j)`.
         */
        operator fun <T> invoke(n: Int, init: (Int, Int) -> T): Matrix<T> {
            return AMatrix.of(n, n, init)
        }

        /**
         * An alias for [invoke].
         */
        fun <T> mat(row: Int, col: Int, init: (Int, Int) -> T): Matrix<T> {
            return AMatrix.of(row, col, init)
        }

        /**
         * An alias for [invoke].
         */
        fun <T> mat(n: Int, init: (Int, Int) -> T): Matrix<T> {
            return AMatrix.of(n, n, init)
        }

        /**
         * Creates a new matrix `A` with the given row and column count, the model and the flattened elements
         * in row-major order.
         *
         * For example, `of(2, 3, model, 1, 2, 3, 4, 5, 6)` creates a matrix:
         * ```
         * 1 2 3
         * 4 5 6
         * ```
         *
         */
        fun <T> of(row: Int, col: Int, vararg elements: T): Matrix<T> {
            require(row * col == elements.size)
            return AMatrix.of(row, col, *elements)
        }

        fun <T> fromFlat(row: Int, col: Int, elements: List<T>): Matrix<T> {
            require(row * col == elements.size)
            return AMatrix.fromFlattenList(row, col, elements)
        }

        fun <T> fromFlat(row : Int, col : Int, init : (Int) -> T) : Matrix<T> {
            return AMatrix.ofFlatten(row, col, init)
        }


        /**
         * Creates a new matrix from a list of column vectors.
         */
        fun <T> fromColumns(columns: List<Vector<T>>): Matrix<T> {
            val row = columns.first().size
            val column = columns.size
            require(columns.all { it.size == row })
            return Matrix(row, column) { i, j -> columns[i][j] }
        }

        /**
         * Creates a new matrix from a list of vectors as rows.
         */
        fun <T> fromRows(rows: List<Vector<T>>): Matrix<T> {
            val row = rows.size
            val column = rows.first().size
            require(rows.all { it.size == column })
            return Matrix(row, column) { i, j -> rows[i][j] }
        }

        /**
         * Creates a zero matrix.
         */
        fun <T> zero(row: Int, column: Int, model: AddMonoid<T>): Matrix<T> {
            return MatrixImpl.zero(row, column, model)
        }

        /**
         * Creates a zero square matrix.
         */
        fun <T> zero(n: Int, model: AddMonoid<T>): Matrix<T> {
            return zero(n, n, model)
        }

        /**
         * Creates a diagonal matrix with the given diagonal elements.
         * The elements off the diagonal are zero.
         */
        fun <T> diag(model: AddMonoid<T>, elements: List<T>): Matrix<T> {
            val n = elements.size
            val zero = MatrixImpl.zero(n, n, model)
            for (i in 0 until n) {
                zero[i, i] = elements[i]
            }
            return zero
        }

        /**
         * Creates a diagonal matrix with the given diagonal elements.
         * The elements off the diagonal are zero.
         */
        fun <T> diag(model: AddMonoid<T>, vararg elements: T): Matrix<T> {
            return diag(model, elements.asList())
        }

        /**
         * Creates a diagonal matrix with the given vector as the diagonal elements.
         */
        fun <T> diag(model: AddMonoid<T>, v: Vector<T>): Matrix<T> {
            val n = v.size
            val A = MatrixImpl.zero<T>(n, n, model)
            for (i in 0 until n) {
                A[i, i] = v[i]
            }
            return A
        }

        /**
         * Creates a scalar matrix with the given scalar `k`, namely a diagonal matrix with all diagonal elements being `k`.
         */
        fun <T> scalar(n: Int, model: AddMonoid<T>, k: T): Matrix<T> {
            return MatrixImpl.scalar(n, model, k)
        }

        /**
         * Creates an identity matrix with the given size `n`.
         */
        fun <T> identity(n: Int, model: UnitRing<T>): Matrix<T> {
            return MatrixImpl.identity(n, model)
        }

        /**
         * Concatenates two matrix `A, B` to a new matrix `(A, B)`.
         *
         * It is required that `A` and `B` have that same row count.
         */
        fun <T> concatColumn(a: Matrix<T>, b: Matrix<T>): Matrix<T> {
            return MatrixImpl.concatCol(a, b)
        }

        /**
         * Concatenates two matrix `A, B` to a new matrix
         *
         *     [A]
         *     [B]
         *
         * It is required that `A` and `B` have that same column count.
         */
        fun <T> concatRow(a: Matrix<T>, b: Matrix<T>): Matrix<T> {
            return MatrixImpl.concatRow(a, b)
        }


        private fun remainingIndices(n: Int, indices: IntArray): IntArray {
            val set = indices.toMutableSet()
            return (0 until n).filter { it !in set }.toIntArray()
        }

        /**
         * Computes the product of the given matrices.
         *
         * The order of multiplication is automatically optimized.
         */
        fun <T> product(model: Ring<T>, vararg matrices: Matrix<T>): Matrix<T> {
            return product(model, matrices.asList())
        }

        /**
         * Computes the product of the given matrices.
         *
         * The order of multiplication is automatically optimized.
         */
        fun <T> product(model: Ring<T>, matrices: List<Matrix<T>>): Matrix<T> {
            return MatrixImpl.product(matrices, model)
        }


        /*
        Matrix models
         */


        fun <T> over(model: EqualPredicate<T>, row: Int, col: Int): MatOverEqualPredicate<T> {
            return MatOverEqualPredicateImpl(model, row, col)
        }

        fun <T> over(model: EqualPredicate<T>, n: Int = 0): MatOverEqualPredicate<T> {
            return MatOverEqualPredicateImpl(model, n, n)
        }

        fun <T> over(model: AddMonoid<T>, row: Int, col: Int): MatOverAddMonoid<T> {
            return MatOverAddMonoidImpl(model, row, col)
        }

        fun <T> over(model: AddMonoid<T>, n: Int = 0): MatOverAddMonoid<T> {
            return MatOverAddMonoidImpl(model, n, n)
        }

        fun <T> over(model: AddGroup<T>, row: Int, col: Int): MatOverAddGroup<T> {
            return MatOverAddGroupImpl(model, row, col)
        }

        fun <T> over(model: AddGroup<T>, n: Int = 0): MatOverAddGroup<T> {
            return MatOverAddGroupImpl(model, n, n)
        }

        fun <T> over(model: Ring<T>, row: Int, col: Int): MatOverRing<T> {
            return MatOverRingImpl(model, row, col)
        }

        fun <T> over(model: Ring<T>, n: Int = 0): MatOverRing<T> {
            return MatOverRingImpl(model, n, n)
        }

        fun <T> over(model: UnitRing<T>, row: Int, col: Int): MatOverURing<T> {
            return MatOverURingImpl(model, row, col)
        }

        fun <T> over(model: UnitRing<T>, n: Int = 0): MatOverURing<T> {
            return MatOverURingImpl(model, n, n)
        }

        fun <T> over(model: EuclideanDomain<T>, row: Int, col: Int): MatOverEUD<T> {
            return MatOverEUDImpl(model, row, col)
        }

        fun <T> over(model: EuclideanDomain<T>, n: Int = 0): MatOverEUD<T> {
            return MatOverEUDImpl(model, n, n)
        }

        fun <T> over(model: Field<T>, row: Int, col: Int): MatOverField<T> {
            return MatOverFieldImpl(model, row, col)
        }

        fun <T> over(model: Field<T>, n: Int = 0): MatOverField<T> {
            return MatOverFieldImpl(model, n, n)
        }


    }
}

/**
 * Result of a PLU decomposition:
 *
 * ```
 * P * A = L * U
 * ```
 *
 * where
 * - [P] is a permutation matrix,
 * - [L] is lower triangular with unit diagonal,
 * - [U] is upper triangular.
 *
 * [pivots] stores the pivot column indices and [rank] is its size.
 */
data class PLUDecompositionResult<T>(
    val P: Matrix<T>,
    val L: Matrix<T>,
    val U: Matrix<T>,
    val pivots: List<Int>,
) {
    val rank: Int
        get() = pivots.size
}

/**
 * Determines whether this matrix is the same shape as [y].
 */
fun Matrix<*>.shapeMatches(y: Matrix<*>): Boolean {
    return row == y.row && column == y.column
}

/**
 * Determines whether this matrix is a square matrix.
 */
inline val Matrix<*>.isSquare: Boolean get() = (row == column)

/**
 * Gets the row indices of this matrix.
 */
inline val Matrix<*>.rowIndices: IntRange get() = 0..<row

/**
 * Gets the column indices of this matrix.
 */
inline val Matrix<*>.colIndices: IntRange get() = 0..<column

/**
 * Gets a read-only-traversable sequence of the indices of this matrix, iterating row first and then column as:
 * ```
 * for(i in 0 until row){
 *     for(j in 0 until column){
 *         yield(i to j)
 *     }
 * }
 * ```
 */
inline val Matrix<*>.indices: Sequence<Pair<Int, Int>>
    get() = IterUtils.prod2(rowIndices, colIndices)


fun <T, A : Appendable> Matrix<T>.joinTo(
    buffer: A, sepRow: CharSequence = "\n ", sepCol: CharSequence = ", ",
    prefixRow: CharSequence = "[", postfixRow: CharSequence = "]",
    prefixCol: CharSequence = "[", postfixCol: CharSequence = "]",
    limitRow: Int = Int.MAX_VALUE, truncatedRow: CharSequence = "...",
    limitCol: Int = Int.MAX_VALUE, truncatedCol: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): A {
    val (row, col) = shape
    buffer.append(prefixRow)
    for (i in 0..<min(row, limitRow)) {
        if (i > 0) buffer.append(sepRow)
        buffer.append(prefixCol)
        for (j in 0..<min(col, limitCol)) {
            if (j > 0) buffer.append(sepCol)
            buffer.append(transform?.invoke(this[i, j]) ?: this[i, j].toString())
        }
        if (col > limitCol) {
            buffer.append(sepCol)
            buffer.append(truncatedCol)
        }
        buffer.append(postfixCol)
    }
    if (row > limitRow) {
        buffer.append(sepRow)
        buffer.append(truncatedRow)
    }
    buffer.append(postfixRow)
    return buffer
}


fun <T> Matrix<T>.joinToString(
    sepRow: CharSequence = "\n ", sepCol: CharSequence = ", ",
    prefixRow: CharSequence = "[", postfixRow: CharSequence = "]",
    prefixCol: CharSequence = "[", postfixCol: CharSequence = "]",
    limit: Int = Int.MAX_VALUE, truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String {
    val builder = StringBuilder()
//    rowIndices.joinTo(builder, prefix = "[", postfix = "]", separator = "\n ", limit = limit) { i ->
//        colIndices.joinTo(builder, prefix = "[", postfix = "]", separator = ", ", limit = limit) { j ->
//            this[i, j].toString()
//        }
//        ""
//    }
    joinTo(
        builder, sepRow, sepCol, prefixRow, postfixRow, prefixCol, postfixCol,
        limit, truncated, limit, truncated, // limit for row and column
        transform
    )
    return builder.toString()
}





@JvmRecord
data class VectorAsColMatrix<T>(val v: Vector<T>) : Matrix<T> {
    override val row: Int
        get() = v.size
    override val column: Int
        get() = 1

    override fun get(i: Int, j: Int): T {
        require(j == 0)
        return v[i]
    }
}

@JvmRecord
data class VectorAsRowMatrix<T>(val v: Vector<T>) : Matrix<T> {
    override val row: Int
        get() = 1
    override val column: Int
        get() = v.size

    override fun get(i: Int, j: Int): T {
        require(i == 0)
        return v[j]
    }
}


val <T> Vector<T>.asMatrix: Matrix<T> get() = VectorAsColMatrix(this)
val <T> RowVector<T>.asMatrix: Matrix<T> get() = VectorAsRowMatrix(this.v)


/**
 * Describes a mutable matrix of elements of type [T].
 */
interface MutableMatrix<T> : Matrix<T> {
    operator fun set(i: Int, j: Int, value: T)

    /**
     * Sets the row `i` with the given row vector.
     */
    fun setRow(i: Int, row: GenVector<T>) {
        for (j in 0..<column) {
            this[i, j] = row[j]
        }
    }

    /**
     * Sets the row `i` with the given element `v`.
     */
    fun setRow(i: Int, v: T) {
        for (j in colIndices) {
            this[i, j] = v
        }
    }

    fun setCol(j: Int, col: GenVector<T>) {
        for (i in 0..<row) {
            this[i, j] = col[i]
        }
    }

    /**
     * Sets the column `j` with the given element `v`.
     */
    fun setCol(j: Int, v: T) {
        for (i in rowIndices) {
            this[i, j] = v
        }
    }

    fun setAll(row: Int, col: Int, matrix: Matrix<T>) {
        for (i in 0..<matrix.row) {
            for (j in 0..<matrix.column) {
                this[i + row, j + col] = matrix[i, j]
            }
        }
    }

    fun copy(): MutableMatrix<T>


    /**
     * Swaps the rows `r1` and `r2` with the given column range `[colStart, colEnd)`.
     */
    fun swapRow(r1: Int, r2: Int, colStart: Int = 0, colEnd: Int = column)

    /**
     * Swaps the columns `c1` and `c2` with the given row range `[rowStart, rowEnd)`.
     */
    fun swapCol(c1: Int, c2: Int, rowStart: Int = 0, rowEnd: Int = row)

    /**
     * Transforms this matrix by applying the function `f` to each element, taking the row and column indices, and the element itself.
     */
    fun transform(f: (Int, Int, T) -> T) {
        for (i in 0 until row) {
            for (j in 0 until column) {
                this[i, j] = f(i, j, this[i, j])
            }
        }
    }

//    operator fun plusAssign(y: Matrix<T>) {
//        val model = model as AddSemigroup
//        transform { i, j, t -> model.add(t, y[i, j]) }
//    }
//
//    operator fun timesAssign(k: T) {
//        val model = model as MulSemigroup
//        transform { _, _, t -> model.multiply(t, k) }
//    }
//
//    operator fun divAssign(k: T) {
//        val model = model as UnitRing
//        transform { _, _, t -> model.exactDivide(t, k) }
//    }
//
//    /**
//     * Negates the row `r` with the given column range `[colStart, colEnd)`.
//     */
//    fun negateRow(r: Int, colStart: Int = 0, colEnd: Int = column)
//
//    /**
//     * Negates the column `c` with the given row range `[rowStart, rowEnd)`.
//     */
//    fun negateCol(c: Int, rowStart: Int = 0, rowEnd: Int = row)
//
//
//    /**
//     * Negates all elements in this matrix.
//     */
//    fun negateInPlace()
//
//    /**
//     * Multiplies the row `r` by `k` with the given column range `[colStart, colEnd)`.
//     */
//    fun mulRow(r: Int, k: T, colStart: Int = 0, colEnd: Int = column)
//
//    /**
//     * Divides the row `r` by `k` with the given column range `[colStart, colEnd)`.
//     */
//    fun divRow(r: Int, k: T, colStart: Int = 0, colEnd: Int = column)
//
//    /**
//     * Multiplies the column `c` by `k` with the given row range `[rowStart, rowEnd)`.
//     */
//    fun mulCol(c: Int, k: T, rowStart: Int = 0, rowEnd: Int = row)
//
//    /**
//     * Divides the column `c` by `k` with the given row range `[rowStart, rowEnd)`.
//     */
//    fun divCol(c: Int, k: T, rowStart: Int = 0, rowEnd: Int = row)
//
//    /**
//     * Adds the row `r1` to the row `r2` with the given column range `[colStart, colEnd)`.
//     */
//    fun addRowTo(r1: Int, r2: Int, colStart: Int = 0, colEnd: Int = column)
//
//    /**
//     * Adds the column `c1` to the column `c2` with the given row range `[rowStart, rowEnd)`.
//     */
//    fun addColTo(c1: Int, c2: Int, rowStart: Int = 0, rowEnd: Int = row)
//
//    /**
//     * Adds the row `r1` multiplied by `k` to the row `r2` with the given column range `[colStart, colEnd)`:
//     *
//     *    this[r2,j] = this[r2,j] + k * this[r1,j]     for j in [colStart, colEnd)
//     */
//    fun mulAddRow(r1: Int, r2: Int, k: T, colStart: Int = 0, colEnd: Int = column)
//
//    /**
//     * Adds the column `c1` multiplied by `k` to the column `c2` with the given row range `[rowStart, rowEnd)`:
//     *
//     *    this[i,c2] = this[i,c2] + k * this[i,c1]     for i in [rowStart, rowEnd)
//     */
//    fun mulAddCol(c1: Int, c2: Int, k: T, rowStart: Int = 0, rowEnd: Int = row)
//
//    /**
//     * Performs a row transformation described as:
//     * ```
//     *     v1 = this[r1,:], v2 = this[r2,:]
//     *     this[r1,:] = a11 * v1 + a12 * v2
//     *     this[r2,:] = a21 * v1 + a22 * v2
//     * ```
//     */
//    fun transformRows(
//        r1: Int, r2: Int, a11: T, a12: T, a21: T, a22: T,
//        colStart: Int = 0, colEnd: Int = column
//    )
//
//    /**
//     * Performs a column transformation described as:
//     * ```
//     *    v1 = this[:,c1], v2 = this[:,c2]
//     *    this[:,c1] = a11 * v1 + a12 * v2
//     *    this[:,c2] = a21 * v1 + a22 * v2
//     * ```
//     */
//    fun transformCols(
//        c1: Int, c2: Int, a11: T, a12: T, a21: T, a22: T,
//        rowStart: Int = 0, rowEnd: Int = row
//    ) {
//        val model = model
//        val A = this
//        for (i in rowStart until rowEnd) {
//            val v1 = A[i, c1]
//            val v2 = A[i, c2]
//            with(model) {
//                A[i, c1] = a11 * v1 + a12 * v2
//                A[i, c2] = a21 * v1 + a22 * v2
//            }
//        }
//    }

    companion object {
        operator fun <T> invoke(
            row: Int, column: Int, init: (Int, Int) -> T
        ): MutableMatrix<T> {
            return AMatrix.of(row, column, init)
        }

        fun <T> copyOf(matrix: Matrix<T>): MutableMatrix<T> {
            return AMatrix.copyOf(matrix)
        }

        fun <T> zero(row: Int, column: Int, model: AddMonoid<T>): MutableMatrix<T> {
            return MatrixImpl.zero(row, column, model)
        }

        fun <T> zero(n: Int, model: AddMonoid<T>): MutableMatrix<T> {
            return zero(n, n, model)
        }

        fun <T> identity(n: Int, model: UnitRing<T>): MutableMatrix<T> {
            return MatrixImpl.identity(n, model)
        }

        fun <T> concatColumn(a: Matrix<T>, b: Matrix<T>): MutableMatrix<T> {
            return MatrixImpl.concatCol(a, b)
        }
    }
}

/**
 * Returns a mutable copy of this matrix.
 */
fun <T> Matrix<T>.toMutable(): MutableMatrix<T> {
    return MutableMatrix.copyOf(this)
}

/*
 Matrix models
 */

interface MatricesShaped {
    /**
     * The row count of the matrix considered.
     *
     * It can be `0` if we do not restrict the row count.
     */
    val row: Int

    /**
     * The column count of the matrix considered.
     *
     * It can be `0` if we do not restrict the column count.
     */
    val column: Int
}

interface MatOverEqualPredicate<T> : EqualPredicate<Matrix<T>>, MatricesShaped {
    val model: EqualPredicate<T>

    override fun isEqual(x: Matrix<T>, y: Matrix<T>): Boolean {
        require(x.shapeMatches(y))
        return MatrixImpl.isEqual(x, y, model)
    }
}


interface MatOverAddMonoid<T> : MatOverEqualPredicate<T>, AddMonoid<Matrix<T>> {

    override val model: AddMonoid<T>

    /**
     * Gets the zero matrix of the prescribed shape [row] and [column].
     *
     * Use `zero(row, col)` or `zero(n)` instead.
     */
    override val zero: Matrix<T>
        get() = Matrix.zero(row, column, model)

    override fun contains(x: Matrix<T>): Boolean {
        return true
    }

    /**
     * Creates a zero matrix with the given [row] and [column].
     */
    fun zero(row: Int, column: Int): Matrix<T> {
        return Matrix.zero(row, column, model)
    }

    /**
     * Creates a zero square matrix with shape `n × n`.
     */
    fun zero(n: Int): Matrix<T> {
        return zero(n, n)
    }

    fun diag(elements: List<T>): Matrix<T> {
        return Matrix.diag(model, elements)
    }

    fun diag(vararg elements: T): Matrix<T> {
        return Matrix.diag(model, *elements)
    }

    fun diag(v: Vector<T>): Matrix<T> {
        return Matrix.diag(model, v)
    }

    fun scalar(n: Int, k: T): Matrix<T> {
        return Matrix.scalar(n, model, k)
    }

    override fun isZero(x: Matrix<T>): Boolean {
        return MatrixImpl.isZero(x, model)
    }

    override fun add(x: Matrix<T>, y: Matrix<T>): Matrix<T> {
        require(x.shapeMatches(y))
        return MatrixImpl.add(x, y, model)
    }

    override fun sumOf(elements: List<Matrix<T>>): Matrix<T> {
        if(elements.isEmpty()) return zero
        return MatrixImpl.sum(elements, model)
    }

    override fun multiplyN(x: Matrix<T>, n: Long): Matrix<T> {
        return MatrixImpl.apply1(x) { model.multiplyN(it, n) }
    }


    /*
    Matrix-related
     */

    /**
     * Returns the trace if this matrix, that is, the sum of diagonal elements.
     *
     * It is required that this matrix is square.
     *
     */
    fun Matrix<T>.trace(): T {
        return MatrixImpl.trace(this, model as AddSemigroup<T>)
    }

    fun Matrix<T>.sum(): T {
        return MatrixImpl.sumAll(this, model)
    }

    /*
    Mutable matrix
     */

    operator fun MutableMatrix<T>.plusAssign(y: Matrix<T>) {
        return MatrixImpl.addInPlace(this, y, model)
    }

    operator fun MutableMatrix<T>.timesAssign(n: Long) {
        return MatrixImpl.multiplyNInPlace(this, n, model)
    }
}


interface MatOverAddGroup<T> : AddGroup<Matrix<T>>, MatOverAddMonoid<T> {
    override val model: AddGroup<T>

    override fun negate(x: Matrix<T>): Matrix<T> {
        return MatrixImpl.negate(x, model)
    }

    override fun subtract(x: Matrix<T>, y: Matrix<T>): Matrix<T> {
        require(x.shapeMatches(y))
        return MatrixImpl.subtract(x, y, model)
    }

    override fun multiplyN(x: Matrix<T>, n: Long): Matrix<T> {
        return super<MatOverAddMonoid>.multiplyN(x, n)
    }

    operator fun MutableMatrix<T>.minusAssign(y: Matrix<T>) {
        return MatrixImpl.subtractInPlace(this, y, model)
    }
}

interface MatOverRing<T> : MatOverAddGroup<T>, Ring<Matrix<T>>, RingModule<T, Matrix<T>> {
    override val model: Ring<T>

    override val scalars: Ring<T>
        get() = model

    override val zero: Matrix<T>
        get() = Matrix.zero(row, column, model)

    override fun contains(x: Matrix<T>): Boolean {
        return true
    }

    override fun scalarMul(k: T, v: Matrix<T>): Matrix<T> {
        return MatrixImpl.multiply(v, k, model)
    }

    /*
    Matrix related operations
     */

    /**
     * Returns the matrix product of this matrix and the given matrix.
     * It is required that `this.column == y.row`.
     *
     *
     * Let `C = A * B`, then `C[i, j] = sum(k; A[i, k] * B[k, j])` for all `i` and `j`.
     *
     */
    infix fun Matrix<T>.matmul(y: Matrix<T>): Matrix<T> {
        return MatrixImpl.matmul(this, y, model)
    }

    /**
     * Returns the matrix product of two matrices.
     */
    override fun multiply(x: Matrix<T>, y: Matrix<T>): Matrix<T> {
        return MatrixImpl.matmul(x, y, model)
    }

    /**
     * The matrix product of two matrices.
     *
     * @see [Matrix.matmul]
     */
    override fun Matrix<T>.times(y: Matrix<T>): Matrix<T> {
        return this.matmul(y)
    }

    /**
     * Returns the matrix product of this matrix and the given column vector as a column vector.
     *
     * Let `v = A * x`, then `v[i ] = sum(k; A[i, k] * x[k ])` for all `i`.
     */
    infix fun Matrix<T>.matmul(v: Vector<T>): Vector<T> {
        return MatrixImpl.matmul(this, v, model)
    }

    /**
     * Returns the matrix product of this matrix and the given column vector as a column vector.
     */
    operator fun Matrix<T>.times(v: Vector<T>): Vector<T> {
        return this.matmul(v)
    }

    /**
     * Returns the matrix product of this row vector and the matrix, resulting in a row vector.
     */
    infix fun RowVector<T>.matmul(m: Matrix<T>): RowVector<T> {
        return RowVector(MatrixImpl.matmul(this.v, m, model))
    }

    /**
     * Returns the matrix product of this row vector and the matrix, resulting in a row vector.
     *
     * @see [RowVector.matmul]
     */
    operator fun RowVector<T>.times(m: Matrix<T>): RowVector<T> {
        return this.matmul(m)
    }

    infix fun RowVector<T>.matmul(v: Vector<T>): T {
        return VectorImpl.inner(this.v, v, model)
    }

    operator fun RowVector<T>.times(v: Vector<T>): T {
        return this.matmul(v)
    }

    /**
     * Computes the determinant of this matrix.
     *
     * The determinant is defined as:
     *
     *     det(A) = \sum_{σ ∈ S_n} sign(σ) \prod_{i=1}^n A_{i, σ(i)},
     *
     * where `S_n` is the symmetric group of degree `n`.
     *
     * It is required that this matrix is square.
     *
     * This method can be defined for matrices over a ring,
     * but a faster implementation (not being `O(n!)`) is available for matrices over a [field][Field] or
     * a [unit ring][UnitRing] supporting [exact division][UnitRing.exactDiv].
     */
    fun Matrix<T>.det(): T {
        return MatrixImpl.det(this, model)
    }

    /**
     * Returns the cofactor of this matrix at the position `(i, j)`.
     * The cofactor is the determinant of the minor matrix at `(i, j)` with a sign determined by `(-1)^(i+j)`.
     * ```
     *     C(i, j) = (-1)^(i+j) * det(minor(i, j))
     * ```
     *
     * @see [Matrix.det]
     */
    fun Matrix<T>.cofactor(i: Int, j: Int): T {
        return MatrixImpl.cofactor(this, i, j, model)
    }

    /**
     * Returns the adjugate of this matrix, which is defined as the transpose of the matrix of cofactors:
     *
     *    adj(A) = (C(i, j))_{i,j}^T
     *
     * If `A` is invertible, then `A * adjugate(A) = det(A) * I`.
     *
     *
     * @see [Matrix.det]
     */
    fun Matrix<T>.adjugate(): Matrix<T> {
        return MatrixImpl.adjugate(this, model)
    }

    /**
     * Returns the hadamard (element-wise) product of two matrices.
     */
    fun Matrix<T>.hadamard(other: Matrix<T>): Matrix<T> {
        return MatrixImpl.hadamard(this, other, model)
    }

    /**
     * Alias for [Matrix.hadamard].
     */
    infix fun Matrix<T>.odot(other: Matrix<T>): Matrix<T> {
        return MatrixImpl.hadamard(this, other, model)
    }

    /**
     * Returns the kronecker product `C = A ⊗ B`.
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
     *
     *
     */
    fun kronecker(A : Matrix<T>, B: Matrix<T>): Matrix<T> {
        return MatrixImpl.kronecker(A, B, model)
    }

    /**
     * Alias for [kronecker].
     */
    infix fun Matrix<T>.kron(other: Matrix<T>): Matrix<T> {
        return MatrixImpl.kronecker(this, other, model)
    }

    /*
    Mutable matrix
     */

    operator fun MutableMatrix<T>.timesAssign(k: T) {
        return MatrixImpl.scalarMulInPlace(this, k, model)
    }

    // row and column operations

    /**
     * Multiplies the row `r` by `k` within the given column range `[colStart, colEnd)`.
     */
    fun MutableMatrix<T>.mulRow(r: Int, k: T, colStart: Int = 0, colEnd: Int = column) {
        if (this is AMatrix) {
            this.mulRow(r, k, colStart, colEnd, model)
        } else {
            for (j in colStart until colEnd) {
                this[r, j] = model.multiply(this[r, j], k)
            }
        }
    }

    /**
     * Multiplies the column `c` by `k` within the given row range `[rowStart, rowEnd)`.
     */
    fun MutableMatrix<T>.mulCol(c: Int, k: T, rowStart: Int = 0, rowEnd: Int = row) {
        for (i in rowStart until rowEnd) {
            this[i, c] = model.multiply(this[i, c], k)
        }
    }
}



interface MatOverURing<T> : MatOverRing<T>, UnitRing<Matrix<T>>, UnitRingModule<T, Matrix<T>> {
    override val model: UnitRing<T>

    /**
     * Returns the identity matrix with the prescribed shape.
     * It is required that [row] `==` [column], otherwise an exception is thrown.
     *
     * Use `eye(n)` instead.
     */
    override val one: Matrix<T>
        get() {
            require(row == column) { "The matrix must be square" }
            return Matrix.identity(row, model)
        }

    /**
     * Returns the identity matrix of size `n` with diagonal elements being `1`.
     */
    fun eye(n: Int): Matrix<T> {
        return Matrix.identity(n, model)
    }


    override fun scalarMul(k: T, v: Matrix<T>): Matrix<T> {
        return MatrixImpl.multiply(v, k, model)
    }

    operator fun Matrix<T>.div(k: T): Matrix<T> {
        return MatrixImpl.apply1(this) { model.exactDiv(it, k) }
    }

    override fun isUnit(x: Matrix<T>): Boolean {
        return MatrixImpl.isInvertible(x, model)
    }

    fun inverse(x: Matrix<T>): Matrix<T> {
        return MatrixImpl.inverse(x, model)
    }

    /**
     * Computes the inverse of this matrix.
     *
     * It is required that this matrix is square.
     *
     * @throws ArithmeticException if this matrix is not invertible.
     */
    fun Matrix<T>.inv(): Matrix<T> {
        return MatrixImpl.inverse(this, model)
    }

    /**
     * Returns the characteristic polynomial of the given square matrix.
     * The characteristic polynomial is defined as `f(λ) = det(λI-A)`.
     *
     *
     * The characteristic polynomial is a polynomial of degree `n` where `n` is the row count (= column count) of the matrix.
     * The leading coefficient is `1`, and the constant term is the determinant of the matrix.
     *
     * The roots of the characteristic polynomial are defined to be the eigenvalues of the matrix.
     *
     * It is required that the `this.model` is actually a [UnitRing].
     *
     * @see [Matrix.det]
     *
     */
    fun Matrix<T>.charPoly(): Polynomial<T> {
        return MatrixImpl.charPoly(this, model)
    }
}


interface MatOverEUD<T> : MatOverURing<T> {
    override val model: EuclideanDomain<T>

    /**
     * Transforms this matrix to Smith normal form, a diagonal matrix with the following property:
     *
     *     m[i,i] | m[i+1,i+1]  for i <= r,
     *     m[i,i] = 0, for i > r
     *
     *
     * It is required that the `model` of this matrix is an [EuclideanDomain].
     *
     * For example, the Smith normal form of matrix `[[1 2 3][4 5 6][7 8 9]]` can be
     * `diag(1,3,0)`
     *
     * The method [Matrix.invariantFactors] might be more useful if you only need the invariant factors.
     *
     * @see [Matrix.invariantFactors]
     */
    fun Matrix<T>.toSmithForm(): Matrix<T> {
        //Created by lyc at 2020-03-10 14:54
        return MatrixImpl.toSmithForm(this, model)
    }

    /**
     * Returns the list of non-zero invariant factors of this matrix in order.
     *
     * To introduce invariant factors, we first define the determinantal divisors `d_k` of a matrix `A`
     * as the greatest common divisor of all `k × k` minors of `A`.
     * For example, the first determinant divisors is the gcd of all elements of the matrix,
     * while the `n`-th  determinant divisors is just the determinant of the matrix.
     * It is easy to see that `d_1 | d_2 | ... | d_n`.
     *
     * Then, the **invariant factors** `α_k` of a matrix `A` are defined by `α_k = d_k / d_{k-1}`, where we take `d_0 = 1`,
     * and take `α_k = 0` if `d_k = 0`.
     *
     * The invariant factors have the following properties:
     * * They are unique up to multiplication by units.
     * * `α_{r+1} = α_{r+2} = ... = α_n = 0`, where `r` is the rank of the matrix.
     * * `α_1 | α_2 | ... | α_r`.
     *
     *
     * It is required that the `model` of this matrix is an [EuclideanDomain].
     *
     * @return the list of non-zero invariant factors `a_1, a_2, ..., a_r`
     *
     */
    fun Matrix<T>.invariantFactors(): List<T> {
        return MatrixImpl.invariantFactors(this, model)
    }

    /**
     * Returns the list of non-zero determinant divisors of this matrix.
     *
     * The `k`-th determinant divisor of a matrix `A` is the greatest common divisor of all `k × k` minors of `A`.
     * For example, the first determinant divisor is the gcd of all elements of the matrix,
     * while the `n`-th determinant divisor is just the determinant of the matrix.
     *
     * The determinant divisors have the following properties:
     * * They are unique up to multiplication by units.
     * * Let `r` be the rank of the matrix. Then `d_k = 0` for `k > r`.
     * * `d_1 | d_2 | ... | d_n`, while the quotient `α_k = d_k / d_{k-1}` is referred to as the `k`-th invariant factor.
     *
     * @return the list of non-zero determinant divisors `d_1, d_2, ..., d_r`
     */
    fun Matrix<T>.detDivisors(): List<T> {
        return MatrixImpl.detDivisors(this, model)
    }

}

interface MatOverField<T> : Algebra<T, Matrix<T>>, MatOverEUD<T> {
    override val model: Field<T>

    override val scalars: Field<T>
        get() = model

    override fun scalarDiv(x: Matrix<T>, k: T): Matrix<T> {
        return MatrixImpl.divide(x, k, model)
    }

    override operator fun Matrix<T>.div(k: T): Matrix<T> {
        return MatrixImpl.divide(this, k, model)
    }

    /*
    Matrix-related
     */

    /**
     * Computes the rank of this matrix.
     *
     * The rank of a matrix is the maximum number of linearly independent rows or columns in the matrix.
     *
     * It is required that this matrix is a matrix of elements in a field.
     */
    fun Matrix<T>.rank(): Int {
        return MatrixImpl.rank(this, model)
    }

    /**
     * Returns the null space of this matrix, which is the set of all vectors `x` such that `Ax = 0`.
     *
     * This is the same as [Matrix.kernel].
     *
     * @see [Matrix.kernel]
     */
    fun Matrix<T>.nullSpace(): VectorSpace<T> {
        return kernel()
    }

    /**
     * Returns the kernel space of `A = this`:
     * ```
     * Ker(A) = { x | Ax = 0 }
     * ```
     *
     * It is required that the `model` of this matrix is a [Field].
     */
    fun Matrix<T>.kernel(): VectorSpace<T> {
        return MatrixImpl.solveHomo(this, model)
    }

    /**
     * Returns the column space of this matrix, which is the vector space spanned by the columns of the matrix.
     */
    fun Matrix<T>.columnSpace(): VectorSpace<T> {
        return MatrixImpl.columnSpace(this, model)
    }

    /**
     * Returns the image space of `A = this`:
     * ```
     * Im(A) = { Ax | x ∈ V }
     * ```
     */
    fun Matrix<T>.image(): VectorSpace<T> {
        return columnSpace()
    }

    /**
     * Returns the rank decomposition of a matrix `A = LR`, where `L` is a column full-rank matrix and `R` is a row full-rank matrix.
     *
     * Let `A` be a matrix of shape `(n,m)` and rank `r`.
     * Then, `L` is of shape `(n,r)` and `R` is of shape `(r,m)` with `rank(L) = rank(R) = r`.
     *
     *
     *
     * @return a pair of `(L, R)`.
     * @throws ArithmeticException if the rank of the given matrix is zero
     */
    fun Matrix<T>.decompRank(): Pair<Matrix<T>, Matrix<T>> {
        return MatrixImpl.decompRank(this, model)
    }

    /**
     * Returns the LU decomposition of this matrix: `A = LU`.
     *
     * The LU decomposition is defined as `A = LU` where `L` is a lower triangular matrix and `U` is an upper triangular matrix.
     *
     * It is required that the `model` of this matrix is a [Field].
     *
     * @return a pair of `(L, U)`.
     */
    fun Matrix<T>.decompLU(): Pair<Matrix<T>, Matrix<T>> {
        return MatrixImpl.decompLU(this, model)
    }

    /**
     * Returns the PLU decomposition of this matrix:
     *
     * ```
     * P * A = L * U
     * ```
     *
     * where `P` is a permutation matrix, `L` is unit lower triangular and `U` is upper triangular.
     */
    fun Matrix<T>.decompPLU(): PLUDecompositionResult<T> {
        return MatrixImpl.decompPLU(this, model)
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
    fun Matrix<T>.decompLDL(): Pair<Matrix<T>, Vector<T>> {
        return MatrixImpl.decompLDL(this, model)
    }


    /**
     * Returns the (row) echelon form of this matrix and the indices of the pivot columns.
     *
     * The row echelon form of a matrix is a matrix in which
     * * all zero rows are at the bottom of the matrix;
     * * the leading coefficient of each row is to the right of the leading coefficient of the row above it;
     * * all entries in the column below a leading coefficient are zeros.
     *
     *
     * An example of a row echelon form of a 4x4 matrix `A` is like
     *
     *    [1 2 3 4]
     *    [0 0 1 2]
     *    [0 0 0 1]
     *    [0 0 0 0]
     *
     * Here, the pivot columns are `0, 2, 3`.
     *
     *
     * It is required that the `model` of this matrix is a [Field].
     *
     * @return a pair of `(E, pivots)`, where `E` is the echelon form and `pivots` is the list of pivot columns.
     */
    fun Matrix<T>.toEchelonForm(): Pair<Matrix<T>, List<Int>> {
        val m = AMatrix.copyOf(this)
        val pivots = MatrixImpl.toEchelon(m, model)
        return m to pivots
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
    fun Matrix<T>.toCongDiagForm(): Pair<Vector<T>, Matrix<T>> {
        return MatrixImpl.toCongDiagonalForm(this, model)
    }

    /**
     * Transforms this matrix to (upper) Hessenberg form.
     *
     * The Hessenberg form of a matrix `A` is a matrix `H` such that all elements below the first subdiagonal are zero.
     * For example, the Hessenberg form of a 4x4 matrix `A` is like
     *
     *    [a b c d]
     *    [e f g h]
     *    [0 i j k]
     *    [0 0 l m]
     *
     *
     */
    fun Matrix<T>.toHessenbergForm(): Matrix<T> {
        return MatrixImpl.toHessenberg(this, model)
    }
}

interface MatOverReals<T> : MatOverField<T> {
    override val model: Reals<T>


    /**
     * Returns the Cholesky decomposition of this matrix: `A = L L.T`, where `L` is a lower triangular matrix.
     *
     * It is required that the `model` of this matrix is a [Reals].
     */
    fun Matrix<T>.decompCholesky(): Matrix<T> {
        return MatrixImpl.decompCholesky(this, model)
    }


    /**
     * Returns the QR decomposition of this matrix: `A = QR`, where `Q` is an orthogonal matrix and `R` is an upper triangular matrix.
     *
     * It is required that the `model` of this matrix is a [Reals].
     *
     * @return a pair of `(Q, R)`.
     */
    fun Matrix<T>.decompQR(): Pair<Matrix<T>, Matrix<T>> {
        return MatrixImpl.decompQR(this, model)
    }


    /**
     * Returns the KAN decomposition (also known as Iwasawa decomposition) of this square matrix.
     *
     * Here, `M = K * A * N`:
     * * `K` is an orthogonal matrix,
     * * `A` is a diagonal matrix,
     * * `N` is an upper triangular matrix.
     *
     *
     * The KAN decomposition comes from factorization for Lie groups, particularly semisimple Lie groups.
     * The abbreviation KAN comes from:
     * * `K` for maximal compact subgroup;
     * * `A` for abelian subgroup;
     * * `N` for nilpotent subgroup.
     */
    fun Matrix<T>.decompKAN(): Triple<Matrix<T>, Vector<T>, Matrix<T>> {
        return MatrixImpl.decompKAN(this, model)
    }

}

internal open class MatricesShapedImpl(
    override val row: Int,
    override val column: Int
) : MatricesShaped

internal class MatOverEqualPredicateImpl<T>(override val model: EqualPredicate<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverEqualPredicate<T>

internal class MatOverAddMonoidImpl<T>(override val model: AddMonoid<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverAddMonoid<T>

internal class MatOverAddGroupImpl<T>(override val model: AddGroup<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverAddGroup<T>

internal open class MatOverRingImpl<T>(override val model: Ring<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverRing<T>

internal class MatOverURingImpl<T>(override val model: UnitRing<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverURing<T>

internal class MatOverEUDImpl<T>(override val model: EuclideanDomain<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverEUD<T>

internal class MatOverFieldImpl<T>(override val model: Field<T>, row: Int, column: Int) :
    MatricesShapedImpl(row, column), MatOverField<T>


//
//open class GeneralLinearGroup<T>(val n: Int, override val model: UnitRing<T>) :
//    MatOverModel<T>(n, n, model), MulGroup<Matrix<T>> {
//    override fun contains(x: Matrix<T>): Boolean {
//        return x.row == n && x.column == n && MatrixImpl.isInvertible(x, model)
//    }
//
//    override fun isEqual(x: Matrix<T>, y: Matrix<T>): Boolean {
//        require(x in this && y in this)
//        return MatrixImpl.isEqual(x, y, model)
//    }
//
//    override fun multiply(x: Matrix<T>, y: Matrix<T>): Matrix<T> {
//        require(x in this && y in this)
//        return MatrixImpl.matmul(x, y, model)
//    }
//
//    override fun reciprocal(x: Matrix<T>): Matrix<T> {
//        return MatrixImpl.inverse(x, model)
//    }
//
//    override val one: Matrix<T>
//        get() = Matrix.identity(n, model)
//
//    override fun product(ps: List<Matrix<T>>): Matrix<T> {
//        if (ps.isEmpty()) return one
//        require(ps.all { it in this })
//        return MatrixImpl.product(ps, model)
//    }
//}
