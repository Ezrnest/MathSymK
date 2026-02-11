/**
 *
 */
package io.github.ezrnest.mathsymk.discrete

import io.github.ezrnest.mathsymk.model.Fraction
import io.github.ezrnest.mathsymk.numTh.NTFunctions.degFactorial
import io.github.ezrnest.mathsymk.util.MathUtils
import io.github.ezrnest.mathsymk.util.exceptions.NumberValueException
import java.math.BigInteger
import kotlin.math.abs

/**
 * A utility class providing some functions in combination mathematics.
 *
 * @author liyicheng
 */
object CombUtils {
    private const val MAX_FAC = 19
    private const val MAX_SUBFAC = 20

    //from 0 to 19 is in the range of long
    private val fac_temp = LongArray(MAX_FAC + 1)

    init {
        var n = 0
        var l: Long = 1
        do {
            fac_temp[n++] = l
            l *= n.toLong()
        } while (n < fac_temp.size)
    }

    private val subfac_temp = LongArray(MAX_SUBFAC + 1)

    init {
        subfac_temp[0] = 1
        subfac_temp[1] = 0
        for (i in 2..MAX_SUBFAC) {
            subfac_temp[i] = (i - 1) * (subfac_temp[i - 1] + subfac_temp[i - 2])
        }
    }

    private fun throwFor(n: Long) {
        throw ArithmeticException("n=$n is out of range")
    }

    private fun intOrTooBig(l: Long): Int {
        if (l <= Int.MAX_VALUE) {
            return l.toInt()
        }
        throw NumberValueException("Too big")
    }

    private fun combinationBExact(n: Int, m: Int): BigInteger {
        if (m == 0) {
            return BigInteger.ONE
        }
        if (m == 1) {
            return BigInteger.valueOf(n.toLong())
        }
        val t = n - m
        if (t < 0) {
            throw ArithmeticException("n<m")
        }
        val k = if (t < m) t else m
        var result = BigInteger.ONE
        for (i in 1..k) {
            val numer = BigInteger.valueOf((n - k + i).toLong())
            val denom = BigInteger.valueOf(i.toLong())
            result = result.multiply(numer).divide(denom)
        }
        return result
    }

    /**
     * Returns the factorial of n.
     * <pre>n!</pre>
     * Number `n` must be in [0,19], otherwise overflow will happen.
     * @param n a number, must be in [0,19]
     * @return the factorial of n.
     * @throws ArithmeticException if overflow occurred.
     */
    fun factorial(n: Int): Long {
        if (n < 0 || n > MAX_FAC) {
            throwFor(n.toLong())
        }
        return fac_temp[n]
    }


//    private fun multiplyPrimePowers(pr: Primes, pp: IntArray): BigInteger {
//        var result = BigInteger.ONE
//        for (i in pp.size - 1 downTo -1 + 1) {
//            when (pp[i]) {
//                0 -> {}
//                1 -> {
//                    result = result.multiply(BigInteger.valueOf(pr.getPrime(i)))
//                }
//
//                else -> {
//                    result = result.multiply(BigInteger.valueOf(pr.getPrime(i)).pow(pp[i]))
//                }
//            }
//        }
//        return result
//    }
//
//    /**
//     * Returns the factorial of n.
//     * <pre>n!</pre>
//     * @param n a number
//     * @return the factorial of n.
//     */
//    fun factorialX(n: Int): BigInteger {
//        if (n < 0) {
//            throwFor(n.toLong())
//        }
//        if (n <= MAX_FAC) {
//            return BigInteger.valueOf(fac_temp[n])
//        }
//        val pr: Primes = Primes.getInstance()
//        pr.enlargePrime(n)
//        val len: Int = pr.getCount(n)
//        val pp = IntArray(len)
//        for (i in 0 until len) {
//            pp[i] = intOrTooBig(MathUtils.degFactorial(pr.getPrime(i), n))
//        }
//        return multiplyPrimePowers(pr, pp)
//    }

    /**
     * Returns the subfactorial of n.
     * <pre>!n = n! * (Σ<sub>k=0</sub><sup>n</sup> (-1)^k / k!)</pre>
     * Number `n` must be in [0,20], otherwise overflow will happen.
     * The result of subfactorial(0) will be 1.
     * @param n a number, must be in [0,20]
     * @return the subfactorial of n.
     * @throws ArithmeticException if overflow occurred.
     */
    fun subfactorial(n: Int): Long {
        if (n < 0 || n > MAX_SUBFAC) {
            throwFor(n.toLong())
        }
        return subfac_temp[n]
    }

    /**
     * Returns the subfactorial of n.
     * <pre>!n = n! * (Σ<sub>k=0</sub><sup>n</sup> (-1)^k / k!)</pre>
     * The result of subfactorial(0) will be 1.
     * @param n a number, must be in [0,20]
     * @return the subfactorial of n.
     * @throws ArithmeticException if overflow occurred.
     */
    fun subfactorialB(n: Int): BigInteger {
        if (n < 0) {
            throwFor(n.toLong())
        }
        if (n <= MAX_SUBFAC) {
            return BigInteger.valueOf(subfac_temp[n])
        }
        var sn = BigInteger.valueOf(subfac_temp[MAX_FAC])
        var sn_1 = BigInteger.valueOf(subfac_temp[MAX_FAC - 1])
        var i = MAX_FAC
        while (i < n) {
            val t = BigInteger.valueOf(i.toLong()).multiply(sn.add(sn_1))
            sn_1 = sn
            sn = t
            i++
        }
        return sn
    }


    /**
     * Returns the permutation of `m,n`.<br></br>
     * P<sub>n</sub><sup>m</sup><br></br>
     *
     *
     * That is, the number of ways of selecting `m` integers from `1,2,...,n` with order.
     *
     *
     * This method will throw an exception if overflow occurred.
     *
     * @param n the count of number to choose from
     * @param m the count of number chosen
     * @return permutation of `n,m`.
     * @throws ArithmeticException if overflow occurred.
     */
    fun permutation(n: Int, m: Int): Long {
        require(m <= n) { "m>n" }
        //special cases 
        if (n == m) {
            return factorial(n)
        }
        var r = n.toLong()
        for (i in n - 1 downTo n - m + 1) {
            r *= i.toLong()
        }
        return r
    }

    /**
     * Returns the permutation of `m,n`.<br></br>
     * **P**<sub>n</sub><sup>m</sup><br></br>
     * Throws an exception if overflow occurred.
     * @param n
     * @param m
     * @return permutation of `m,n`.
     * @throws NumberValueException if the result is too big for BigInteger.
     */
    fun permutationB(n: Long, m: Long): BigInteger {
        val x = abs(n)
        val y = abs(m)
        require(y <= x) { "m>n" }
        var r = BigInteger.valueOf(x)
        for (i in x - 1 downTo x - y + 1) {
            r = r.multiply(BigInteger.valueOf(i))
        }
        return r
    }


    /**
     * Returns
     * `deg(p,**P**<sub>n</sub><sup>m</sup>)`
     * @param n
     * @param m
     * @param p the modular
     * @return
     */
    fun degPermutation(n: Long, m: Long, p: Long): Long {
        val n = abs(n)
        val m = abs(m)
        require(m <= n) { "m>n" }
        if (m == n) {
            return degFactorial(p, n)
        }
        return degFactorial(p, n) - degFactorial(p, m)
    }

//    /**
//     * Returns the combination of `m,n`.<br></br>
//     * **C**<sub>n</sub><sup>m</sup><br></br>
//     * @param n
//     * @param m
//     * @return combination of `m,n`.
//     */
//    fun combinationB(n: Int, m: Int): BigInteger {
//        if (m == 0) {
//            return BigInteger.ONE
//        }
//        if (m == 1) {
//            return BigInteger.valueOf(n.toLong())
//        }
//        // n! / m!(n-m)!
//        val t = n - m
//        if (t < 0) {
//            throw ArithmeticException("n<m")
//        }
//        val pr: Primes = Primes.getInstance()
//        pr.enlargePrime(n)
//        val len: Int = pr.getCount(n)
//        val pp = IntArray(len)
//        for (i in 0 until len) {
//            val p: Long = pr.getPrime(i)
//            pp[i] = intOrTooBig(degFactorial(p, n) - degFactorial(p, t) - degFactorial(p, m))
//        }
//        return multiplyPrimePowers(pr, pp)
//    }

    /**
     * Returns the combination of `m,n`.<br></br>
     * **C**<sub>n</sub><sup>m</sup><br></br>
     * @return combination of `m,n`.
     */
    fun combination(n: Int, m: Int): Long {
        if (m == 0) {
            return 1
        }
        if (m == 1) {
            return n.toLong()
        }
        // n! / m!(n-m)!
        val t = n - m
        if (t < 0) {
            throw ArithmeticException("n<m")
        }
        if (t < m) {
            return combination(n, t)
        }
        return try {
            permutation(n, m) / factorial(m)
        } catch (ae: ArithmeticException) {
            combinationBExact(n, m).longValueExact()
        }
    }

//    private fun combinationDeg(n: Int, m: Int, t: Int): Long {
//        val pr: Primes = Primes.getInstance()
//        pr.enlargePrime(n)
//        val len: Int = pr.getCount(n)
//        val pp = IntArray(len)
//        for (i in 0 until len) {
//            val p: Long = pr.getPrime(i)
//            pp[i] = intOrTooBig(degFactorial(p, n) - degFactorial(p, t) - degFactorial(p, m))
//        }
//        return MathUtils.fromFactorPowers(pp)
//    }

    /**
     * Returns the binomial of (n,k), which is
     * the coefficient of `x^k` in the expression
     * of `(x+1)^n`. This method also supports negative
     * `p,n` values.
     * @param n the power: (x+1)^n
     * @param k the power of x whose coefficient is required.
     * @return
     * @see .binomialB
     * @see .binomialD
     */
    fun binomial(n: Int, k: Int): Long {
        if (n < 0) {
            val re = multisetNumber(-n, k)
            return if (k % 2 == 0) re else -re
        } else if (n < k) {
            return 0
        }
        return combination(n, k)
    }

    /**
     * Returns the binomial of (n,k), which is
     * the coefficient of `x^k` in the expression
     * of `(x+1)^n`. This method also supports negative
     * `p,n` values.
     * @param n the power: (x+1)^n
     * @param k the power of x whose coefficient is required.
     * @return
     * @see CombUtils.binomial
     */
    fun binomialB(n: Int, k: Int): BigInteger {
        if (n < 0) {
            val re = multisetNumberB(-n, k)
            return if (k % 2 == 0) re else re.negate()
        } else if (n < k) {
            return BigInteger.ZERO
        }
//        return combinationB(n, k)
        return combinationBExact(n, k)
    }

    /**
     * Returns the expand of binomial coefficient.
     * <pre>α(α-1)(α-2)...(α-k+1)/k!</pre>
     * @param α
     * @param k
     * @return
     * @see CombUtils.binomial
     */
    fun binomialD(α: Double, k: Int): Double {
        var re = α
        for (i in 1 until k) {
            re *= α - i
        }
        re /= factorial(k).toDouble()
        return re
    }

    /**
     * Returns the multinomial of (p,ns), the
     * while is equal to
     * <pre>p!/n0!*n1!*...nm!</pre>
     * The sum of `ns` must NOT be bigger than `p`.
     * @param p
     * @param ns
     * @return
     */
    fun multinomial(p: Int, vararg ns: Int): Long {
        checkSumArray(p, ns.sum(), ns)
        var r = factorial(p)
        for (n in ns) {
            r /= factorial(n)
        }
        return r
    }

    private fun checkSumArray(p: Int, sum2: Int, ns: IntArray) {
        require(ns.size != 0) { "length==0" }
        require(sum2 <= p) { "ns>p" }
    }

    /**
     * Returns the multinomial of (p,ns), the
     * while is equal to
     * <pre>p!/n0!*n1!*...nm!</pre>
     * The sum of `ns` must NOT be bigger than `p`.
     * @param p
     * @param ns
     * @return
     */
    fun multinomialB(p: Int, vararg ns: Int): BigInteger {
        checkSumArray(p, ns.sum(), ns)
        var remaining = p
        var result = BigInteger.ONE
        for (n in ns) {
            result = result.multiply(combinationBExact(remaining, n))
            remaining -= n
        }
        return result
//        val pr: Primes = Primes.getInstance()
//        pr.enlargePrime(p)
//        val len: Int = pr.getCount(p)
//        val pp = IntArray(len)
//        for (i in 0 until len) {
//            val prime: Long = pr.getPrime(i)
//            var power: Long = degFactorial(prime, p)
//            for (n in ns) {
//                power -= degFactorial(prime, n)
//            }
//            pp[i] = intOrTooBig(power)
//        }
//        return multiplyPrimePowers(pr, pp)
    }

    /**
     * Returns the multiset number
     * <pre>((n,k))</pre>
     * It is the number of multisets of cardinality k,
     * with elements taken from a finite set of cardinality n.
     * `n*(n+1)*(n+2)*...*(n+k-1) / k!`
     * @param n
     * @param k
     * @return
     */
    fun multisetNumber(n: Int, k: Int): Long {
        require(!(n < 0 || k < 0))
        return combination(n + k - 1, k)
    }

    /**
     * Returns the multiset number
     * <pre>((n,k))</pre>
     * It is the number of multisets of cardinality k,
     * with elements taken from a finite set of cardinality n.
     * `n*(n+1)*(n+2)*...*(n+k-1) / k!`
     * @param n
     * @param k
     * @return
     */
    fun multisetNumberB(n: Int, k: Int): BigInteger {
        require(!(n < 0 || k < 0))
        return combinationBExact(n + k - 1, k)
    }

    /**
     * Returns the circle permutation: Take `m` elements from `n`
     * different elements and put them in a circle.
     * **P**<sub>n</sub><sup>m</sup>/m<br></br>
     * @param n
     * @param m
     * @return
     */
    fun circleP(n: Int, m: Int): Long {
        return permutation(n, m) / m
    }

    /**
     * Returns the circle permutation: Take `m` elements from `n`
     * different elements and put them in a circle.
     * **P**<sub>n</sub><sup>m</sup>/m<br></br>
     * @param n
     * @param m
     * @return
     */
    fun circlePB(n: Long, m: Long): BigInteger {
        return permutationB(n, m).divide(BigInteger.valueOf(m))
    }

    /**
     * Returns the polygon color number: Set `m` colors
     * to a polygon of `n` vertexes, and two vertexes
     * of any edge share different colors.
     * <pre>(m-1)*(-1)^n+(m-1)^n</pre>
     * @param n the number of vertexes, two or bigger.
     * @param m the number of colors, positive
     * @return
     */
    fun polyColor(n: Int, m: Int): Long {
        require(!(n < 2 || m < 1))
        val t = m - 1
        val re = (if (n % 2 == 0) t else -t).toLong()
        return re + MathUtils.pow(t.toLong(), n)
    }

//    /**
//     * Returns the number of different ways of passing a
//     * ball: Among `n` persons, a person holds a ball, now
//     * they pass the ball for `m` times, and after the final
//     * passing, the first person holds the ball again.
//     * <pre>[(n-1)*(-1)^m+(n-1)^m]/n</pre>
//     * @param n the number of people
//     * @param m times of passing
//     * @return
//     */
//    fun passBall(n: Int, m: Int): Long {
//        require(!(n < 2 || m < 0))
//        val t = n - 1
//        var re = (if (m % 2 == 0) t else -t).toLong()
//        re += MathUtils.pow(t.toLong(), m)
//        return re / n
//    }

    /**
     * Returns the number of partitions of the number `n`, in which the biggest number is `m`.
     * A partition is a set of integers
     * and their sum is equal to `n`.
     *
     *
     * For example, 4=4=1+3=2+2=1+1+2=1+1+1+1, so there are 5 different partitions and
     * `integerPartition(4,3)=1` and `integerPartition(4,2)=2`.
     * By convenience,
     * if `n==0`, this method will return `1`. If `n<0`, `0` will be returned.
     * If `m<1`, `0` will be returned.
     * @param n an integer
     * @param m a positive integer
     * @return the number of partitions
     */
    @JvmOverloads
    fun integerPartition(n: Long, m: Long = n): Long {
        var m = m
        if (n == 0L) {
            return 1
        }
        if (n < 0) {
            return 0
        }
        if (m < 1) {
            return 0
        }
        if (m > n) {
            m = n
        }
        //		if(m>PARTITION_RECUR_THREHOLD) {
//			return integerPartitionDp(n, m);
//		}
        return integerPartitionRecur(n, m)
    }

    //	static final long PARTITION_RECUR_THREHOLD = 100; 
    fun integerPartitionRecur(n: Long, m: Long): Long {
        if (n <= 0 || m <= 0) {
            return 0
        }
        if (n == 1L || m == 1L) {
            return 1
        }
        if (m == 2L) {
            return n / 2 + 1
        }
        if (n < m) {
            return integerPartitionRecur(n, n)
        }
        if (n == m) {
            return integerPartitionRecur(n, m - 1) + 1
        }
        return integerPartitionRecur(n, m - 1) +
                integerPartitionRecur(n - m, m)
    }

//    /**
//     * Returns a progression `a(m) = C(n,m)`
//     */
//    fun binomialsOf(n: Int): Progression<Long> {
//        //C(n,m) = n!/(m!*(n-m)!) = n! / (m-1)!*(n-m+1)! * (n-m+1)/m
//        return Progression.createProgressionRecur1WithIndex({ with ->
//            val prev: Long = with.getObj()
//            val m: Long = with.getLong()
//            prev * (n - m + 1) / m
//        }, n + 1, Calculators.longCal(), 1L)
//    }
//
//    /**
//     * Returns a progression `a(m) = C(n,m)`
//     */
//    fun binomialsBigOf(n: Int): Progression<BigInteger> {
//        return Progression.createProgressionRecur1WithIndex({ with ->
//            val prev: BigInteger = with.getObj()
//            val m: Long = with.getLong()
//            val t = BigInteger.valueOf(n - m + 1)
//            val mBig = BigInteger.valueOf(m)
//            prev.multiply(t).divide(mBig)
//        }, n + 1, Calculators.bigInteger(), BigInteger.ONE)
//    }

    /**
     * Returns the reverse count of the array.
     */
    fun reverseCount(arr: IntArray): Int {
        var count = 0
        for (i in arr.indices) {
            val t = arr[i]
            for (j in i + 1 until arr.size) {
                if (arr[j] < t) {
                    count++
                }
            }
        }
        return count
    }


    private val EULER_NUMBER_EVEN_LONG = longArrayOf(
        1, -1, 5, -61, 1385, -50521, 2702765, -199360981, 19391512145L, -2404879675441L,
        370371188237525L, -69348874393137901L
    )

    /**
     * Returns the n-th Euler number. The leading several terms are
     * <pre>1, 0, -1, 0, 5, 0, -61, 0, 1385, 0, -50521, 0, 2702765 ...</pre>
     * Euler number is defined by the initial value E<sub>0</sub> = 1 and the recursive formula:
     * <pre>E<sub>2n</sub> + C(2n,2n-2)E<sub>2n-2</sub> + C(2n,2n-4)E<sub>2n-4</sub> + ... + C(2n,2)E<sub>2</sub> + E<sub>0</sub> = 0</pre>
     * @param n an integer, starting from 0, and should not exceed 12(because of overflow).
     * @return the n-th Euler number.
     */
    fun numEuler(n: Int): Long {
        var n = n
        if (n % 2 == 1) {
            return 0
        }
        n /= 2
        if (n >= EULER_NUMBER_EVEN_LONG.size) {
            throw ArithmeticException("Euler number exceeds long for index=" + n * 2)
        }
        return EULER_NUMBER_EVEN_LONG[n]
    }

    /**
     * Computes the n-th Katalan number. This method do not consider the overflow.
     */
    fun numKatalan(n: Int): Long {
        val katList = LongArray(n)
        katList[0] = 1
        return ck0(n - 1, katList)
    }

    private fun ck0(n: Int, katList: LongArray): Long {
        if (katList[n] != 0L) {
            return katList[n]
        }
        var sum: Long = 0
        for (i in 0 until n) {
            sum += ck0(
                i,
                katList
            ) * ck0(n - i - 1, katList)
        }
        katList[n] = sum
        return sum
    }


//    /**
//     * Returns a progression of Euler number of even index. The progression starts from 0 and has the length of
//     * `n`.
//     *
//     *
//     * The leading several terms are
//     * <pre>1, -1, 5, -61, 1385, -50521, 2702765, -199360981</pre>
//     *
//     * @param n the length of the progression
//     */
//    fun numEulerEvenBig(n: Int): Progression<BigInteger> {
////        if(n % 2 == 1){
////            return BigInteger.ZERO;
////        }
////        if(n < EULER_NUMBER_EVEN_LONG.length){
////            return BigInteger.valueOf(numEuler(n));
////        }
//        val initials: Array<BigInteger> = ArraySup.mapTo(
//            EULER_NUMBER_EVEN_LONG, BigInteger::valueOf,
//            BigInteger::class.java
//        )
//        return Progression.createProgressionRecur({ p: Progression<BigInteger?>, idx: Long ->
//            var sum = BigInteger.ZERO
//            val comb: Progression<BigInteger> = binomialsBigOf(Math.toIntExact(idx * 2))
//            for (i in 0 until idx) {
//                sum = sum.add(comb.get(i * 2L).multiply(p.get(i)))
//            }
//            sum.negate()
//        }, n, Calculators.bigInteger(), initials)
//        //        n = n/2;
////        BigInteger[] tempTable = new BigInteger[n+1];
////        for(int i=0;i<EULER_NUMBER_EVEN_LONG.length;i++){
////            tempTable =
////        }
////        for(int i=0;i<n;i++){
////            var combs = binomialsOf()
////        }
////        return null;
//    }

    private var BernoulliNumbers: Array<Fraction>? = null

    @Synchronized
    private fun initBernoulli() {
        if (BernoulliNumbers == null) {
            BernoulliNumbers = arrayOf<Fraction>(
                Fraction.ONE,
                Fraction.of(1, 6),
                Fraction.of(-1, 30),
                Fraction.of(1, 42),
                Fraction.of(-1, 30),
                Fraction.of(5, 66),
                Fraction.of(-691, 2730),
                Fraction.of(7, 6),
                Fraction.of(-3617, 510),
                Fraction.of(43867, 798),
                Fraction.of(-174611, 330),
                Fraction.of(854513, 138),
                Fraction.of(-236364091, 2730),
                Fraction.of(8553103, 6),
                Fraction.of(-23749461029L, 870),
                Fraction.of(8615841276005L, 14322),
                Fraction.of(-7709321041217L, 510),
                Fraction.of(2577687858367L, 6),
            )
        }
    }

    /*
    1,  1,  -1,  1,  -1,  5,  -691,  7,  -3617,  43867,  -174611,  854513,  -236364091,  
    8553103,  -23749461029,  8615841276005,  -7709321041217,  2577687858367,  -26315271553053477373,  2929993913841559,  
     1, 6,  30,  42,  30,  66,  2730,  6,  510,  798,  330,  138,  2730,  6,  870,  14322,
      510,  6,  1919190,  6,  13530,  1806,  690,  282,  46410,  66,  1590,  798,  870,  354,  56786730
     */
    /**
     * Returns the n-th Bernoulli number. The first several terms are:
     * <pre>1, -1/2, 1/6, 0, -1/30, 0, 1/42, 0, -1/30, 0, 5/66 ...</pre>
     * The Bernoulli number is defined by the initial value B<sub>0</sub>=1 and
     * the recursive formula:
     * <pre>C(n+1,0)B<sub>0</sub> + C(n+1,1)B<sub>1</sub> + C(n+1,2)B<sub>2</sub> + ... + C(n+1,n)B<sub>n</sub> = 0</pre>
     * @param n the index, starting from 0 and it should not exceed 17(because of overflow).
     * @return the n-th Bernoulli number
     */
    fun numBernoulli(n: Int): Fraction {
        var n = n
        if (n == 1) {
            return -Fraction.HALF
        }
        if (n % 2 == 1) {
            return Fraction.ZERO
        }
        n /= 2
        initBernoulli()
        if (n >= BernoulliNumbers!!.size) {
            throw ArithmeticException("Bernoulli number overflow long for index = " + n * 2)
        }
        return BernoulliNumbers!![n]
    }

//    /**
//     * Returns a progression containing Bernoulli number of even index. The progression's index starts from 0 and
//     * is smaller than `n`.
//     *
//     *
//     * The leading several terms are
//     * <pre>1, 1/6, -1/30, 1/42, -1/30, 5/66, -691/2730, 7/6, -3617/510, 43867/798 ...</pre>
//     * @param n the length of the progression
//     */
//    fun numBernoulliEvenBig(n: Int): Progression<BigFraction> {
//        initBernoulli()
//        val initials: Array<BigFraction> = ArraySup.mapTo(
//            BernoulliNumbers, BigFraction::fromFraction,
//            BigFraction::class.java
//        )
//        return Progression.createProgressionRecur({ p, k ->
//            var sum: BigFraction = BigFraction.ZERO
//            val comb: Progression<BigInteger> = binomialsBigOf(Math.toIntExact((2 * k + 1).toLong()))
//            for (i in 0 until k) {
//                val term: Unit = p.get(i).multiply(comb.get(2 * i))
//                sum = sum.add(term)
//            }
//            if (k >= 1) {
//                val term: Unit = BigFraction.fromFraction(numBernoulli(1)).multiply(comb.get(1))
//                sum = sum.add(term)
//            }
//            val coe: Unit = comb.get(2 * k)
//            sum = sum.divide(coe)
//            sum.negate()
//        }, n, BigFraction.getCalculator(), initials)
//    }
// 	static long integerPartitionDp(long n,long m) {
    //		
    //	}
    //    public static void main(String[] args) {
    //
    ////        print(numBernoulli(6));
    ////        print(numBernoulli(20));
    ////        print(BernoulliNumbers.length);
    ////        initBernoulli();
    ////        for(int i=0;i<10;i++){
    ////            printnb(BernoulliNumbers[i]+", ");
    ////        }
    //        numBernoulliEvenBig(20).forEach(Printer::print);
    //    }
    ////		final int n = 20;
    ////		long sum = 0,t = n;
    ////		long f = MathFunctions.power(-1, n);
    ////		sum += f;
    ////		for(int i=n-1;i>=0;i--){
    ////			f = -f;
    ////			sum += f*t;
    ////			t *= i;
    ////		}
    ////		Timer t = new Timer();
    ////		t.start();
    ////		BigInteger re = BigInteger.ONE;
    ////		for(long i=1;i<1000000;i++){
    ////			re = re.multiply(BigInteger.valueOf(i));
    ////		}
    ////		print("MUL "+t.end());
    ////		NumberFormat nf = SNFSupport.dfByDigit(4);
    ////		for(int i=0;i<20;i++){
    ////			print(nf.format(distributionBinomialB(i,20,0.5d).doubleValue()));
    ////		}
    //		
    //	}
}
