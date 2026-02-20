package linear

import TestUtils.assertEquals
import io.github.ezrnest.mathsymk.linear.*
import io.github.ezrnest.mathsymk.model.Multinomial
import io.github.ezrnest.mathsymk.model.Models
import io.github.ezrnest.mathsymk.model.Polynomial
import io.github.ezrnest.mathsymk.model.Polynomial.Companion.substitute
import io.github.ezrnest.mathsymk.numTh.NTFunctions
import io.github.ezrnest.mathsymk.util.IterUtils
import io.github.ezrnest.mathsymk.util.MathUtils
import io.github.ezrnest.mathsymk.util.pow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatrixTest {

    @Test
    fun fromColumnsKeepsColumnMajorPlacement() {
        val columns = List(3) { col ->
            Vector(3) { row -> 10 * col + row }
        }
        val matrix = Matrix.fromColumns(columns)

        for (row in 0 until matrix.row) {
            for (col in 0 until matrix.column) {
                assertEquals(columns[col][row], matrix[row, col])
            }
        }
    }

    @Test
    fun fromColumnsKeepsColumnMajorPlacementForRectangularMatrix() {
        val columns = List(3) { col ->
            Vector(2) { row -> 10 * col + row }
        }
        val matrix = Matrix.fromColumns(columns)

        assertEquals(2, matrix.row)
        assertEquals(3, matrix.column)
        for (row in 0 until matrix.row) {
            for (col in 0 until matrix.column) {
                assertEquals(columns[col][row], matrix[row, col])
            }
        }
    }


    val Z = Models.ints()
    val Zmod7 = Models.intModP(7)
    val Zmod97 = Models.intModP(97)
    val matZ = Matrix.over(Z)
    val matZmod7 = Matrix.over(Zmod7)
    val matZmod97 = Matrix.over(Zmod97)

    @Test
    fun testDetGB() {
        val mults = Multinomial.over(Z)
        (1..3).forEach { n ->
            val A = Matrix(n) { i, j -> mults.monomial("($i$j)") } // 3x3 matrix with variables (ij)
            val detGB = MatrixImpl.detGaussBareiss(A.toMutable(), mults)
            mults.assertEquals(MatrixImpl.detDefinition(A, mults), detGB)
            mults.assertEquals(MatrixImpl.detSmall(A, mults), detGB)
        }
        (4..5).forEach { n ->
            val A = Matrix(n) { i, j -> mults.monomial("($i$j)") } // 4x4 matrix with variables (ij)
            mults.assertEquals(MatrixImpl.detDefinition(A, mults), MatrixImpl.detGaussBareiss(A.toMutable(), mults))
        }
    }

    @Test
    fun scalarDivisionOfMatrix() {
        with(matZ) {
            val A = Matrix(2, 2) { i, j -> (i + j) * 2 }
            val scalar = 2
            val expected = Matrix(2, 2) { i, j -> i + j }
            assertEquals(expected, A / scalar)
        }
    }

    @Test
    fun additionOfMatricesWithDifferentDimensions() {
        with(matZ) {
            val A = Matrix(2, 2) { i, j -> i + j }
            val B = Matrix(3, 3) { i, j -> (i + 1) * (j + 1) }
            assertThrows<IllegalArgumentException> { A + B }
        }
    }

    @Test
    fun subtractionOfMatricesWithDifferentDimensions() {
        with(matZ) {
            val A = Matrix(2, 2) { i, j -> i + j }
            val B = Matrix(3, 3) { i, j -> (i + 1) * (j + 1) }
            assertThrows<IllegalArgumentException> { A - B }
        }
    }

    @Test
    fun multiplicationOfMatricesWithIncompatibleDimensions() {
        with(matZ) {
            val A = Matrix(2, 3) { i, j -> i + 1 }
            val B = Matrix(2, 2) { i, j -> j + 1 }
            assertThrows<IllegalArgumentException> { A * B }
        }
    }

    @Test
    fun inverseOfIdentityMatrix() {
        with(matZmod7) {
            val identity = Matrix.identity(3, Zmod7)
            val expected = Matrix.identity(3, Zmod7)
            assertEquals(expected, identity.inv())
        }
    }

    @Test
    fun inverseOfNonSingularMatrix() {
        with(matZmod7) {
            val A = Matrix(2) { i, j -> if (i == j) 1 else 2 }
            assertEquals(eye(2), A * A.inv())
            assertEquals(eye(2), A.inv() * A)
        }
    }

    @Test
    fun inverseOfSingularMatrixThrowsException() {
        with(matZmod7) {
            val singular = Matrix(2, 2) { _, _ -> 1 }
            assertThrows<ArithmeticException> { singular.inv() }
        }
    }

    @Test
    fun inverseOfNonSquareMatrixThrowsException() {
        with(matZmod7) {
            val nonSquare = Matrix(2, 3) { i, j -> i + j }
            assertThrows<IllegalArgumentException> { nonSquare.inv() }
        }
    }

    @Test
    fun testMatrixCharPoly() {
        val ℤ = Models.ints()
        val Zx = Polynomial.over(ℤ)
        val n = 4
        val Z44 = Matrix.over(Z,n)
        with(Z44) {
            with(Zx) {
                val A = Matrix(n) { i, j -> i + 2 * j }
                val p = A.charPoly() // the characteristic polynomial of A, p(λ) = det(λI - A)
                ℤ.assertEquals(A.trace(), -p[n - 1])
                ℤ.assertEquals(A.det(), (-1).pow(n) * p[0])
                // another way to compute the characteristic polynomial
                // sum of all principal minors of A
                val coef = (0..n).map { k ->
                    if (k == n) return@map ℤ.one
                    var res = ℤ.zero
                    for (rows in IterUtils.comb(A.row, n - k, false)) {
                        val major = A.slice(rows, rows).det()
                        res += major
                    }
                    res * MathUtils.powOfMinusOne(k)
                }
                val p2 = io.github.ezrnest.mathsymk.model.Polynomial.fromList(ℤ, coef)
                assertEquals(p, p2)
                assertTrue(isZero(p.substitute(A, Z44))) // p(A) = 0, a matrix of zeros
            }
        }

    }

    @Test
    fun testDet() {
        val mult = Multinomial.over(Z)
        val A = Matrix(3, 3) { i, j ->
            mult.monomial("($i$j)")
        }
        val det3 = MatrixImpl.detSmall(A, mult)
        val detGB = MatrixImpl.detGaussBareiss(A, mult)
        val detDef = MatrixImpl.detDefinition(A, mult)
        mult.assertEquals(det3, detGB)
        mult.assertEquals(det3, detDef)
    }

    @Test
    fun testInvariantFactorsOverIntegers() {
        // 3x3 matrix over integers
        val Z = Models.ints()
        val n = 4
//    val A = Matrix(n, Z) { i, j -> (i + 1) * (j + 2) }
        with(matZ) {
            for (seed in 10..12) {
                val rng = Random(seed)
                val A = Matrix(n) { i, j ->
                    rng.nextInt(10)
                }
                val invFactors = MatrixImpl.invariantFactors(A, Z)
                val accProd = invFactors.scan(1) { acc, factor -> acc * factor }.drop(1)
                val gcds = mutableListOf<Int>()
                for (k in 1..n) {
                    val rows = IterUtils.comb(A.row, k, false)
                    val cols = IterUtils.comb(A.column, k, false)
                    val minors =
                        IterUtils.prod2(rows, cols).map { A.slice(it.first, it.second).det() }.toList().toIntArray()
                    val gcd = NTFunctions.gcd(*minors)
                    if (gcd == 0) {
                        break
                    }
                    gcds.add(gcd)
                }
                assertEquals(gcds, accProd)
            }
        }
    }

    @Test
    fun testSolveLinear() {
        val F = Zmod97
        val matOverF = Matrix.over(F)
        with(matOverF) {
            val A = Matrix.zero(3, 5, F)
            val b = Vector.unitVector(3, 0, F)
            val sol = LinAlg.solveLinear(A, b, F)
            assertTrue(sol == null)
        }
        with(matOverF) {
            val rng = Random(10)
            val A = Matrix(4, 4) { i, j -> rng.nextInt(100) }
            val b = Vector(4) { i -> rng.nextInt(100) }
            val sol = LinAlg.solveLinear(A, b, F)!! // A is full rank
            assertEquals(A * sol.solution, b)
        }

        with(matOverF) {
            val rng = Random(10)
            val A = Matrix(4, 4) { i, j -> rng.nextInt(100) }
            val sol = LinAlg.solveHomo(A, F)
            assertEquals(0, sol.dim) // A is full rank
        }
    }

    @Test
    fun columnSpaceOfZeroMatrix() {
        val A = Matrix.zero(3, 3, Zmod7)
        val columnSpace = MatrixImpl.columnSpace(A, Zmod97)
        assertEquals(0, columnSpace.dim)
    }

    @Test
    fun columnSpaceOfFullRank() {
        val A = Matrix.identity(3, Zmod7)
        val columnSpace = MatrixImpl.columnSpace(A, Zmod97)
        assertEquals(3, columnSpace.dim)
    }


    @Test
    fun spaceDecomposition() {
        val rng = Random(10)
        with(matZmod97) {
            repeat(3) {
                val n = 5
                val A = Matrix(n) { _, _ -> rng.nextInt(97) }
                val kernel = A.kernel()
                val image = A.image()
                assertEquals(n, kernel.dim + image.dim)
            }
        }

    }

    @Test
    fun decompCholeskyDOfPositiveDefiniteMatrix() {
        val F = Zmod7
        val matOverF = Matrix.over(F)
        with(matOverF) {
            val A = Matrix(3, 3) { i, j -> if (i == j) 4 else 1 }
            val (L, D) = A.decompLDL()
            assertEquals(A, L * diag(D) * L.T)
        }

    }

    @Test
    fun testCholeskyDRandom() {
        val rng = Random(10)
        with(matZmod97) {
            val A = Matrix(4, 4) { i, j -> rng.nextInt(100) }.let { it + it.T }
            val (L, D) = A.decompLDL()
            println(L)
            println(D)
            assertEquals(A, L * diag(D) * L.T)
        }
    }

    @Test
    fun decompLUOfSquareMatrix() {
        with(matZmod97) {
            val rng = Random(10)
            val n = 5
            val A = Matrix(n) { _, _ -> rng.nextInt(97) }
            val (L, U) = A.decompLU()
            assertEquals(A, L * U)
        }
    }

    @Test
    fun decompPLUOfSquareMatrix() {
        with(matZmod97) {
            val rng = Random(13)
            val n = 5
            val A = Matrix(n) { _, _ -> rng.nextInt(97) }
            val result = A.decompPLU()
            assertEquals(result.P * A, result.L * result.U)
            assertEquals(result.rank, result.pivots.size)
        }
    }

    @Test
    fun decompPLUHandlesZeroLeadingPivotByRowSwap() {
        with(matZmod7) {
            val A = Matrix.of(
                3,
                3,
                0, 1, 2,
                1, 2, 3,
                2, 4, 1
            )
            val result = A.decompPLU()
            assertEquals(result.P * A, result.L * result.U)
            // a row swap should have happened, so P is not identity
            assertTrue(result.P != eye(3))
            assertEquals(3, result.rank)
        }
    }

    @Test
    fun decompRankOfFullRankMatrix() {
        with(matZmod97) {
            val A = Matrix(3, 3) { i, j -> if (i == j) 1 else 0 }
            val (L, R) = A.decompRank()
            assertEquals(3, L.column)
            assertEquals(3, R.row)
            assertEquals(A, L * R)
        }
    }

    @Test
    fun decompRankOfRankDeficientMatrix() {
        with(matZmod97) {
            val A = Matrix(3, 3) { _, _ -> 1 }
            val (L, R) = A.decompRank()
            assertEquals(1, L.column)
            assertEquals(1, R.row)
            assertEquals(A, L * R)
        }
    }

    @Test
    fun decompRankOfNonSquareMatrix() {
        with(matZmod97) {
            val A = Matrix(3, 2) { i, j -> i + j }
            val (L, R) = A.decompRank()
            assertEquals(2, L.column)
            assertEquals(2, R.row)
            assertEquals(A, L * R)
        }
    }

    @Test
    fun decompRankOfZeroMatrix() {
        with(matZmod97) {
            val A = Matrix.zero(3, 3, Zmod7)
            val (L, R) = A.decompRank()
            assertEquals(0, L.column)
            assertEquals(0, R.row)
            assertEquals(A, L * R)
        }
    }

    @Test
    fun toCongDiagFormOfIdentityMatrix() {
        with(matZmod97) {
            val A = Matrix.identity(3, Zmod7)
            val (Lambda, P) = A.toCongDiagForm()
            assertEquals(P.T * A * P, diag(Lambda))
        }
    }

    @Test
    fun toCongDiagFormOfZeroMatrix() {
        with(matZmod97) {
            val A = Matrix.zero(3, 3, Zmod7)
            val (Lambda, P) = A.toCongDiagForm()
            assertTrue(Lambda.all { it == 0 })
            assertEquals(Matrix.identity(3, Zmod7), P)
        }
    }

    @Test
    fun toCongDiagFormOfSymmetricMatrix() {
        with(matZmod97) {
            val A = Matrix(3, 3) { i, j -> if (i == j) 2 else 1 }
            val (Lambda, P) = A.toCongDiagForm()
            assertEquals(P * A * P.T, diag(Lambda))
        }
    }

    @Test
    fun toCongDiagFormRandom() {
        with(matZmod97) {
            val rng = Random(10)
            val A = Matrix(4, 4) { i, j -> rng.nextInt(100) }.let { it + it.T }
            val (Lambda, P) = A.toCongDiagForm()
            assertEquals(P * A * P.T, diag(Lambda))
        }
    }

    @Test
    fun toCongDiagFormRandom2() {
        with(matZmod97) {
            val rng = Random(10)
            val A = Matrix(4, 4) { i, j -> rng.nextInt(100) }.let { it + it.T }
                .let { it - diag(it.diag()) }
            val (Lambda, P) = A.toCongDiagForm()
            assertEquals(P * A * P.T, diag(Lambda))
        }
    }


    @Test
    fun toCongDiagFormOfNonSquareMatrixThrowsException() {
        with(matZmod97) {
            val A = Matrix(2, 3) { i, j -> i + j }
            assertThrows<IllegalArgumentException> { A.toCongDiagForm() }
        }
    }

    @Test
    fun toCongDiagFormOfNonSymmetricMatrixThrowsException() {
        with(matZmod97) {
            val A = Matrix(3, 3) { i, j -> if (i < j) 1 else 0 }
            assertThrows<IllegalArgumentException> { A.toCongDiagForm() }
        }

    }
}
