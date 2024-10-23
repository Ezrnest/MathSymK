package io.github.ezrnest.mathsymk.numTh

import io.github.ezrnest.mathsymk.model.isEven
import io.github.ezrnest.mathsymk.util.MathUtils
import java.math.BigInteger
import java.util.*
import kotlin.math.abs

object NTFunctions {
    /**
     * Returns the great common divisor (GCD) of two numbers, the result will always be non-negative.
     *
     *
     * Note: It follows from common conventions that `gcd(a, 0) = gcd(0, a) = a`
     *
     * @param a a number
     * @param b another number
     * @return `gcd(n1, n2)`
     */
    @JvmStatic
    fun gcd(a: Long, b: Long): Long {
        //use Euclidean gcd algorithm
        var n1 = a
        var n2 = b
        while (n2 != 0L) {
            val t = n2
            n2 = n1 % n2
            n1 = t
        }
        return abs(n1)
    }

    /**
     * Returns the great common divisor (GCD) of two numbers, the result will always be non-negative.
     *
     *
     * Note: It follows from common conventions that `gcd(a, 0) = gcd(0, a) = a`
     *
     * @param a a number
     * @param b another number
     * @return `gcd(n1, n2)`
     */
    @JvmStatic
    fun gcd(a: Int, b: Int): Int {
        //use Euclidean gcd algorithm
        var n1 = a
        var n2 = b
        while (n2 != 0) {
            val t = n2
            n2 = n1 % n2
            n1 = t
        }
        return abs(n1)
    }

    fun gcdReduce(a: Int, b: Int): Pair<Int, Int> {
        val gcd = gcd(a, b)
        if(a < 0){
            return -a / gcd to -b / gcd
        }
        return a / gcd to b / gcd
    }

    /*
    fun gcdUV0(a: Int, b: Int): IntArray {
        //Re-implemented by lyc at 2020-03-03 15:57
        /*
        Euclid's Extended Algorithms:
        Refer to Henri Cohen 'A course in computational algebraic number theory' Algorithm 1.3.6
         */
        if (b == 0) {
            return intArrayOf(a, 1, 0)
        }
        /*
        Explanation of the algorithm:
        we want to maintain the following equation while computing the gcd using the Euclid's algorithm
        let d0=a, d1=b, d2, d3 ... be the sequence of remainders in Euclid's algorithm,
        then we have
            a*1 + b*0 = d0
            a*0 + b*1 = d1
        let
            u0 = 1, v0 = 0
            u1 = 0, v1 = 1
        then we want to build a sequence of u_i, v_i such that
            a*u_i + b*v_i = d_i,
        when we find the d_n = gcd(a,b), the corresponding u_n and v_n is what we want.
        We have:
            d_i = q_i * d_{i+1} + d_{i+2}        (by Euclid's algorithm
        so
            a*u_i + b*v_i = q_i * (a*u_{i+1} + b*v_{i+1}) + (a*u_{i+2} + b*v_{i+2})
            u_i - q_i * u_{i+1} = u_{i+2}
            v_i - q_i * v_{i+1} = v_{i+2}
        but it is only necessary for us to record u_i, since v_i can be calculated from the equation
            a*u_i + b*v_i = d_i
         */
        var d0 = a
        var d1 = b
        var u0 = 1
        var u1 = 0
        while (d1 > 0) {
            val q = d0 / d1
            val d2 = d0 % d1
            d0 = d1
            d1 = d2
            val u2 = u0 - q * u1
            u0 = u1
            u1 = u2
        }
        val v = (d0 - a * u0) / b
        return intArrayOf(d0, u0, v)
    }
     */

    private inline fun <T, R> gcdUV0Template(
        a: T, b: T,
        zero: T, one: T,
        isZero: (T) -> Boolean,
        div: (T, T) -> T, rem: (T, T) -> T,
        sub: (T, T) -> T, multiply: (T, T) -> T,
        buildRes: (T, T, T) -> R,
    ): R {
        //Re-implemented by lyc at 2020-03-03 15:57
        // use a template to avoid duplicate code for int and long
        /*
        Euclid's Extended Algorithms:
        Refer to Henri Cohen 'A course in computational algebraic number theory' Algorithm 1.3.6
         */
        if (isZero(b)) {
            return buildRes(a, one, zero)
        }
        /*
        Explanation of the algorithm:
        we want to maintain the following equation while computing the gcd using the Euclid's algorithm
        let d0=a, d1=b, d2, d3 ... be the sequence of remainders in Euclid's algorithm,
        then we have
            a*1 + b*0 = d0
            a*0 + b*1 = d1
        let
            u0 = 1, v0 = 0
            u1 = 0, v1 = 1
        then we want to build a sequence of u_i, v_i such that
            a*u_i + b*v_i = d_i,
        when we find the d_n = gcd(a,b), the corresponding u_n and v_n is what we want.
        We have:
            d_i = q_i * d_{i+1} + d_{i+2}        (by Euclid's algorithm
        so
            a*u_i + b*v_i = q_i * (a*u_{i+1} + b*v_{i+1}) + (a*u_{i+2} + b*v_{i+2})
            u_i - q_i * u_{i+1} = u_{i+2}
            v_i - q_i * v_{i+1} = v_{i+2}
        but it is only necessary for us to record u_i, since v_i can be calculated from the equation
            a*u_i + b*v_i = d_i
         */
        var d0 = a
        var d1 = b
        var u0 = one
        var u1 = zero
        while (!isZero(d1)) {
            val q = div(d0, d1)
            val d2 = rem(d0, d1)
            d0 = d1
            d1 = d2
            val u2 = sub(u0, multiply(q, u1))
            u0 = u1
            u1 = u2
        }
//        val v = (d0 - a * u0) / b
        val v = div(sub(d0, multiply(a, u0)), b)
        return buildRes(d0, u0, v)
    }


    /**
     * Computes the greatest common divisor of two numbers and a pair of number `(u,v)` such that
     * `ua+vb = gcd(a,b)`.
     *
     *
     * The result `gcd(a,b)` will always be positive.
     * It follows from common conventions that `gcd(a, 0) = gcd(0, a) = a`
     *
     * @return an int array of `[gcd(a,b), u, v]`.
     */
    @JvmStatic
    fun gcdUV(a: Int, b: Int): IntArray {
        val result = gcdUV0Template(
            Math.absExact(a), Math.absExact(b), 0, 1,
            { it == 0 }, Int::div, Int::rem, Int::minus, Int::times,
            { a, b, c -> intArrayOf(a, b, c) }
        )
        if (a < 0) {
            result[1] = -result[1]
        }
        if (b < 0) {
            result[2] = -result[2]
        }
        return result
    }

    /**
     * Computes the greatest common divisor of two numbers and a pair of number `(u,v)` such that
     * <pre>ua+vb=gcd(a,b)</pre>
     *
     *
     * And it satisfies that `|u|,|v|` is minimal, that is, if `|a| != 0 `, then
     * <pre>0 <= |v| < |a|/d</pre>
     *
     *
     *
     *
     * The result `gcd(a,b)` will always be positive.
     * It follows from common conventions that `gcd(a, 0) = gcd(0, a) = a`
     *
     * @return an int array of `[gcd(a,b), u, v]`.
     */
    @JvmStatic
    fun gcdUVMin(a: Int, b: Int): IntArray {
        val result = gcdUV(a, b)
        if (a == 0 || b == 0) {
            return result
        }
        val d = result[0]
        val u = result[1]
        val v = result[2]

        val a1 = a / d
        val b1 = b / d
        val k = v / a1
        val v1 = v % a1

        val u1 = u + b1 * k
        result[1] = u1
        result[2] = v1
        return result
    }


    /**
     * Computes the greatest common divisor of two numbers and a pair of number (u,v) such that
     * <pre>ua+vb=gcd(a,b)</pre>
     *
     *
     * The result `gcd(a,b)` will always be positive.
     * It follows from common conventions that `gcd(a, 0) = gcd(0, a) = a`
     *
     * @return an array of `[gcd(a,b), u, v]`.
     */
    @JvmStatic
    fun gcdUV(a: Long, b: Long): LongArray {
        val result: LongArray = gcdUV0Template(
            Math.abs(a), Math.abs(b), 0L, 1L,
            { it == 0L }, Long::div, Long::rem, Long::minus, Long::times,
            { x, y, z -> longArrayOf(x, y, z) }
        )
        //deal with negative values
        if (a < 0) {
            result[1] = -result[1]
        }
        if (b < 0) {
            result[2] = -result[2]
        }
        return result
    }


    /**
     * Computes the two numbers' least common multiple(LCM). The result will always be positive.
     *
     *
     * Note: It follows from common conventions that `lcm(a, 0) = lcm(0, a) = 0`
     *
     * @param n1 a number
     * @param n2 another number
     * @return `lcm(n1, n2)`
     */
    @JvmStatic
    fun lcm(n1: Long, n2: Long): Long {
        val gcd = gcd(n1, n2)
        return abs(n1 / gcd * n2)
    }

    /**
     * Calculate the two numbers' least common multiple(LCM). The result will always be positive.
     *
     *
     * Note: It follows from common conventions that `lcm(a, 0) = lcm(0, a) = 0`
     *
     * @param n1 a number
     * @param n2 another number
     * @return `lcm(n1, n2)`
     */
    @JvmStatic
    fun lcm(n1: Int, n2: Int): Int {
        val gcd = gcd(n1, n2)
        return abs(n1 / gcd * n2)
    }

    /**
     * Returns the greatest common divisor of all the given numbers, the result will always be positive.
     *
     * @param ls an array of numbers with at least one element.
     * @return the GCD of `ls`
     */
    @JvmStatic
    fun gcd(vararg ls: Long): Long {
        return ls.reduce(NTFunctions::gcd)
//        if (ls.size < 2) {
//            return ls[0]
//        }
//        var gcd = gcd(ls[0], ls[1])
//        for (i in 2 until ls.size) {
//            gcd = gcd(gcd, ls[i])
//        }
//        return gcd
    }

    fun gcd(vararg ls: Int): Int {
        return ls.reduce(NTFunctions::gcd)
    }


    /**
     * Calculate the least common multiplier (LCM) of the given numbers' LCM, the result will always be positive.
     *
     * @param ls an array of numbers with at least one element.
     * @return the LCM of `ls`
     */
    @JvmStatic
    fun lcm(vararg ls: Long): Long {
        return ls.reduce(NTFunctions::lcm)
    }

    /**
     * Calculate the two numbers' least common multiple(LCM). The result will always be positive.
     *
     *
     * Note: It follows from common conventions that `lcm(a, 0) = lcm(0, a) = 0`
     *
     * @param n1 a number
     * @param n2 another number
     * @return `lcm(n1, n2)`
     */
    @JvmStatic
    fun lcm(n1: BigInteger, n2: BigInteger?): BigInteger {
        val gcd = n1.gcd(n2)
        return n1.divide(gcd).multiply(n2)
    }

    /**
     * Returns the max number `k` that `|b|%|a|^k==0` while `|b|%|a|^(k+1)!=0`, that
     * is, the degree of of `a` in `b`.
     *
     * @param a a number except `0,1,-1`.
     * @param b another number
     * @return deg(a, b)
     */
    @JvmStatic
    fun deg(a: Long, b: Long): Int {
        val a1 = abs(a)
        var b1 = abs(b)

        require(!(a1 == 0L || a1 == 1L))
        var k = 0
        while (b1 % a1 == 0L) {
            k++
            b1 /= a1
        }
        return k
    }

    /**
     * Returns the max number `k` that `|b|%|a|^k==0` while `|b|%|a|^(k+1)!=0`, that
     * is, the degree of of `a` in `b`.
     *
     * @param a a number except `0,1,-1`.
     * @param b another number
     * @return deg(a, b)
     */
    @Suppress("NAME_SHADOWING")
    @JvmStatic
    fun deg(a: Int, b: Int): Int {
        var a = a
        var b = b
        a = abs(a)
        b = abs(b)
        require(!(a == 0 || a == 1))
        var k = 0
        while (b % a == 0) {
            k++
            b /= a
        }
        return k
    }

    /**
     * Returns the result of `deg(|p|,|n|!)`,
     *
     * @param p a number except `0,1,-1`.
     * @param n another number
     * @return the result
     */
    @Suppress("NAME_SHADOWING")
    @JvmStatic
    fun degFactorial(p: Long, n: Long): Long {
        val p = abs(p)
        var n = abs(n)
        require(!(p == 0L || p == 1L))
        var re: Long = 0
        while (((n / p).also { n = it }) != 0L) {
            re += n
        }
        return re
    }


    /**
     * Returns a non-negative integer of `a mod m`, it is required that
     * `m` is positive.
     *
     * @param m a positive integer
     */
    @JvmStatic
    fun mod(a: Int, m: Int): Int {
        var re = a % m
        if (re < 0) {
            re += m
        }
        return re
    }

    /**
     * Returns a non-negative integer of `a mod m`, it is required that
     * `m` is positive.
     *
     * @param m a positive integer
     */
    @JvmStatic
    fun mod(a: Long, m: Int): Int {
        var re = a % m
        if (re < 0) {
            re += m
        }
        return re.toInt()
    }

    /**
     * Returns a non-negative integer of `a mod m`, it is required that
     * `m` is positive.
     *
     * @param m a positive integer
     */
    @JvmStatic
    fun mod(a: Long, m: Long): Long {
        var re = a % m
        if (re < 0) {
            re += m
        }
        return re
    }

    /**
     * Returns `(a^n) % mod`, that is, the power of `a` modulo `m`,
     * the result will always be in `[0, mod)`
     *
     *
     * For example, `powMod(2,2,3) = 1`.
     *
     *
     *
     * This method will not check for overflow.
     *
     * @param a   an integer
     * @param n   the power
     * @param mod a positive modular
     * @return `(a^n) % mod`
     */
    @JvmStatic
    @Suppress("NAME_SHADOWING")
    fun powMod(a: Long, n: Long, mod: Long): Long {
        var a = a
        var n = n
        a = mod(a, mod)
        if (a == 0L || a == 1L) {
            return a
        }
        var ans: Long = 1
        a %= mod
        while (n > 0) {
            if ((n and 1L) == 1L) {
                ans = (a * ans) % mod
            }
            a = (a * a) % mod
            n = n shr 1
        }
        return ans
    }

    /**
     * Returns `(a^n) % mod`, that is, the power of `a` modulo `m`,
     * the result will always be in `[0, mod)`
     *
     *
     * For example, `powMod(2,2,3) = 1`.
     *
     *
     *
     * @param a   an integer
     * @param n   the power
     * @param mod a positive modular
     * @return `(a^n) % mod`
     */
    @JvmStatic
    fun powMod(a: Int, n: Int, mod: Int): Int {
        return powMod(a, n.toLong(), mod)
    }

    /**
     * Returns `(a^n) % mod`, that is, the power of `a` modulo `m`,
     * the result will always be in `[0, mod)`
     *
     *
     * For example, `powMod(2,2,3) = 1`.
     *
     *
     *
     * This method will not check for overflow.
     *
     * @param a   an integer
     * @param n   the power
     * @param mod a positive modular
     * @return `(a^n) % mod`
     */
    @JvmStatic
    fun powMod(a: Int, n: Long, mod: Int): Int {
        var a1 = a
        var n1 = n
        a1 = mod(a1, mod)
        if (a1 == 0 || a1 == 1) {
            return a1
        }
        var res: Long = 1 // prevent overflow
        a1 %= mod
        while (n1 > 0) {
            if ((n1 and 1L) == 1L) {
                res = (a1 * res) % mod
            }
            a1 = (a1 * a1) % mod
            n1 = n1 shr 1
        }
        return res.toInt()
    }


    /**
     * Returns the mod inverse of `a` mod `p`. That is, a number `u` such that
     * `a * u = 1 (mod p)`. It is required that `a` and `p` are co-prime.
     */
    @JvmStatic
    fun modInverse(a: Int, p: Int): Int {
        //Created by lyc at 2020-03-03 16:50
        val arr = gcdUV(a, p)
        // au + pv = 1
        if (arr[0] != 1) {
            throw ArithmeticException("a and p is not coprime: a=$a, p=$p")
        }
        return arr[1]
    }

    /**
     * Returns the mod inverse of `a` mod `p`. That is, a number `u` such that
     * `a * u = 1 (mod p)`. It is required that `a` and `p` are co-prime.
     */
    @JvmStatic
    fun modInverse(a: Long, p: Long): Long {
        //Created by lyc at 2020-03-03 16:50
        val arr = gcdUV(a, p)
        // au + pv = 1
        if (arr[0] != 1L) {
            throw ArithmeticException("a and p is not coprime: a=$a, p=$p")
        }
        return arr[1]
    }

    /**
     * Returns a solution for the modular equations: <pre>x mod m_i = r_i,</pre> where
     * `m_i` are co-prime integers.
     * The result is guaranteed to be minimal non-negative solution.
     *
     * @param mods       an array of modular, `m_i`
     * @param remainders an array of remainders,
     * @return the solution of the modular equation
     */
    @JvmStatic
    fun chineseRemainder(mods: LongArray, remainders: LongArray): Long {
//        long M = product(mods);
//        long x = 0;
//        for (int i = 0; i < mods.length; i++) {
//            var m = mods[i];
//            var r = remainders[i];
//            var t = M / m;
//            var inv = modInverse(t, m);
//            x += r * t * inv;
//            x %= M;
//        }
//        return x;
        //Created by lyc at 2021-04-20 20:31
        /*
        Proof of this algorithm:
        Invariant: x satisfies: x = rem[j] mod m[j] for j < i
         */
        var m = mods[0]
        var x = remainders[0]
        for (i in 1 until mods.size) {
            val t = gcdUV(m, mods[i])
            val u = t[1]
            val v = t[2]
            // um + v m[i] = 1
            x = u * m * remainders[i] + v * mods[i] * x
            // x mod m[i] = um*rem[i] mod m[i] =(1-v m[i])rem[i] mod m[i] = rem[i]
            // for j < i, x mod m[j] = v * m[i] * x mod m[j] = (1-um)x mod m[j] = x
            m *= mods[i]
            x %= m
            // x in (-m,m)
        }
        if (x < 0) {
            x += m //make it non-negative
        }
        return x
    }


    /**
     * Produces Miller-Rabin prime number test algorithm for the number x.If this
     * method returns true, then the number has the possibility of (1/4)^round of not
     * being a prime.
     *
     * @param x     a number,positive
     * @param round round number for test, positive
     * @return true if the number passes the test.
     */
    @JvmOverloads
    fun doMillerRabin(x: Long, round: Long = 25): Boolean {
        //basic check
        if (x == 2L || x == 3L || x == 5L || x == 7L || x == 11L || x == 13L || x == 17L || x == 19L || x == 23L || x == 29L || x == 31L) {
            return true
        }
        require(x >= 2) { "x<=1" }
        require(round >= 1) { "round < 1" }
        val x_1 = x - 1
        var d = x_1
        var s = 0
        while ((d and 1L) == 0L) {
            d = d shr 1
            s++
        }
        val rd = Random()
        for (i in 0 until round) {
            val a = rd.nextLong(x)
            var t = powMod(a, d, x)
            if (t == 1L) {
                return false
            }
            var r = 0
            while (r < s) {
                if (t == x_1) {
                    return false
                }
                r++
                t = (t * t) % x
            }
        }
        return true
    }

    /**
     * Describes a prime factor and its corresponding power.
     *
     * @param prime the prime factor
     * @param power the power of the prime factor
     */
    data class Factor(val prime: Long, val power: Int) {
        override fun toString(): String {
            return "$prime^$power"
        }
    }

    /**
     * Describes a prime factor and its corresponding power.
     *
     * @param prime the prime factor
     * @param power the power of the prime factor
     */
    data class FactorBig(val prime: BigInteger, val power: Int) {
        override fun toString(): String {
            return "$prime^$power"
        }
    }

    /**
     * Factorize the number `n` into a list of prime factors and their corresponding powers.
     */
    fun factorize(n: Long): List<Factor> {
        return factorizeEnumerate(abs(n))
    }

    private fun factorizeEnumerate(n_: Long): List<Factor> {
        var n = n_
        val factors = arrayListOf<Factor>()
        run {
            var count = 0
            while (n % 2 == 0L) {
                count++
                n /= 2
            }
            if (count > 0) {
                factors.add(Factor(2, count))
            }
        }
        for (i in 3..n step 2) {
            var count = 0
            while (n % i == 0L) {
                count++
                n /= i
            }
            if (count > 0) {
                factors.add(Factor(i, count))
            }
            if (i * i > n) {
                break
            }
        }
        if (n > 1) {
            factors.add(Factor(n, 1))
        }
        return factors
    }

    /**
     * Factorize the number `|n|` into a list of prime factors and their corresponding powers.
     */
    fun factorize(n: BigInteger): List<FactorBig> {
        return factorizeEnumerate(n.abs())
    }

    private fun factorizeEnumerate(n_: BigInteger): List<FactorBig> {
        var n = n_
        val factors = arrayListOf<FactorBig>()
        run {
            var count = 0
            while (n.isEven()) {
                count++
                n = n.shiftRight(1)
            }
            if (count > 0) {
                factors.add(FactorBig(BigInteger.TWO, count))
            }
        }
        var i = BigInteger.valueOf(3)
        do {
            var count = 0
            while (n % i == BigInteger.ZERO) {
                count++
                n /= i
            }
            if (count > 0) {
                factors.add(FactorBig(i, count))
            }
            i += BigInteger.TWO
        } while (i * i <= n)
        if (n > BigInteger.ONE) {
            factors.add(FactorBig(n, 1))
        }
        return factors
    }


    /**
     * The radical of n, `rad(n)`,
     * is the product of distinct prime factors of n.
     * For example, `504 = 2^3 × 3^2 × 7`, so `rad(504) = 2 × 3 × 7 = 42`.
     *
     * @return rad(n)
     */
    fun rad(n: Long): Long {
        val factors = factorize(n)
        return factors.fold(1L) { acc, factor -> acc * factor.prime }
    }

    /**
     * Find the maximal integer `a` such that `a^n | x` and returns the pair `(a, x1 = x / a^n)`.
     *
     * It can be used to compute:
     * ```
     * x^(1/n) = a * x1^(1/n)
     * ```
     *
     * If `x` is negative, then it is required that `n` is odd and `a` will also be negative.
     */
    fun nrootFactor(x: Long, n: Int): Pair<Long, Long> {
        require(n > 0) { "It is required that n != 0" }
        if (x == 0L) return 0L to 1L
        val xAbs = if (x < 0) {
            if (n % 2 == 0) throw ArithmeticException("x is negative and n is even")
            -x
        } else x
        val factors = factorize(xAbs)
        var a = 1L
        for (factor in factors) {
            val p = factor.prime
            val k = factor.power / n
            if(k > 0) {
                a *= MathUtils.pow(p, k)
            }
        }
        val x1 = xAbs / MathUtils.pow(a, n)
        if(x < 0) {
            a = -a
        }
        return a to x1
    }


    fun nrootFactor(x : BigInteger, n : Int) : Pair<BigInteger, BigInteger> {
        require(n > 0) { "It is required that n > 0, but n = $n" }
        if (x == BigInteger.ZERO) return BigInteger.ZERO to BigInteger.ONE
        val xAbs = if (x.signum() < 0) {
            if (n % 2 == 0) throw ArithmeticException("x is negative and n is even")
            x.negate()
        } else x
        val factors = factorize(xAbs)
        var a = BigInteger.ONE
        for (factor in factors) {
            val p = factor.prime
            val k = factor.power / n
            if(k > 0) {
                a = a.multiply(p.pow(k))
            }
        }
        val x1 = xAbs.divide(a.pow(n))
        if(x.signum() < 0) {
            a = a.negate()
        }
        return a to x1
    }

    /**
     * Let `p/q = p_/q_` be the reduced form of `p/q`.
     * Returns `a, x1` such that
     * ```
     * x^(p/q) = a * x1^(1/q)
     * ```
     */
    fun nrootFactor(x : BigInteger, p_ : Int, q_ : Int) : Pair<BigInteger,BigInteger>{
        val (p,q) = gcdReduce(p_,q_)
        require(p >= 0){"p must be non-negative, but p = $p"}
        if (x == BigInteger.ZERO) {
            if (p == 0) throw ArithmeticException("0^0 is undefined")
            return BigInteger.ZERO to BigInteger.ONE
        }
        if(p == 0) return BigInteger.ONE to BigInteger.ONE
        val xAbs = if (x.signum() < 0) {
            if (q % 2 == 0) throw ArithmeticException("x is negative and q is even")
            x.negate()
        } else x
        val factors = factorize(xAbs)
        var a = BigInteger.ONE
        var x1 = BigInteger.ONE
        for (factor in factors) {
            val prime = factor.prime
            val power = factor.power * p
            val k = power / q
            val r = power % q
            if(k > 0) {
                a = a.multiply(prime.pow(k))
            }
            if(r > 0){
                x1 = x1.multiply(prime.pow(r))
            }
        }
        if(x.signum() < 0) {
            a = a.negate()
        }
        return a to x1
    }

//    /**
//     * Returns the primitive root modulo `p`, that is,
//     * an integer `a` that generates the modular multiplication
//     * group `(Z_p)^*`.
//     *
//     *
//     * The order of `a` in `(Z_p)^*` is exactly `p-1`.
//     *
//     * @return a primitive root of `p`.
//     */
//    fun primitiveRoot(p: Long): Long {
//        require(p >= 2) { "p must be an odd prime!" }
//        if (p == 2L) {
//            return 1
//        }
//        val q = p - 1
//        val factors = factorize()
//        Outer@ for (a in 2 until p) {
//            for (i in 1 until factors.size) {
//                val e = powMod(a, q / factors[i], p)
//                if (e == 1L) {
//                    continue@Outer
//                }
//            }
//            return a
//        }
//        throw ArithmeticException("No primitive root for p!")
//    }

    /*

    /**
     * Returns the number of factors of the integer.
     *
     * @return a positive integer
     */
    fun factorCount(n: Long): Long {
        val factors = factorReduce(n)
        return factorCount0(factors)
    }

    private fun factorCount0(factors: Array<LongArray?>): Long {
        var num: Long = 1
        for (factor in factors) {
            num *= factor!![1] + 1
        }
        return num
    }

    /**
     * Returns an array containing all the factors of `n` in order.
     *
     * @param n a positive integer
     */
    fun factors(n: Long): LongArray {
        require(n >= 1) { "n<1" }
        //two ways
        val pr: Primes = Primes.getInstance()
        return if (n <= FACTOR_ENUMERATE_THRESHOLD && !pr.isPrimesAvailable(n / 2 + 1)) {
            factorsEnumerate(n)
        } else {
            factorsUsingPrimes(n)
        }
    }

    /**
     * Returns the proper divisors of [n], which are its factors except [n] itself.
     */
    fun properDivisors(n: Long): LongArray {
        val factors = factors(n)
        return factors.copyOf(factors.size - 1)
    }

    private const val FACTOR_ENUMERATE_THRESHOLD: Long = 10000

    private fun factorsEnumerate(n: Long): LongArray {
        var factors = LongArray(16)
        factors[0] = 1
        var idx = 1
        for (t in 2..n) {
            if (n % t == 0L) {
                factors = ensureCapacityAndAdd(factors, t, idx)
                idx++
            }
        }
        return factors.copyOf(idx)
    }

    private fun factorsUsingPrimes(n: Long): LongArray {
        val fr = factorReduce(n)
        val factors = LongArray(Math.toIntExact(factorCount0(fr)))
        addFactor(fr, factors, 0, 1, 0)
        Arrays.sort(factors)
        return factors
    }

    /**
     * @param order order in fr
     * @param base  multiplied previously
     * @param index index in factors
     */
    private fun addFactor(fr: Array<LongArray?>, factors: LongArray, order: Int, base: Long, index: Int): Int {
        var index = index
        if (order == fr.size) {
            factors[index] = base
            return index + 1
        }
        val pFactor = fr[order]
        val maxPower = Math.toIntExact(pFactor!![1])
        for (i in 0..maxPower) {
            index = addFactor(fr, factors, order + 1, base * pow(pFactor[0], i), index)
        }
        return index
    }


    /**
     * Returns a two-dimension array representing the
     * number's prime factors and the corresponding powers.
     * <P>For example, <text> factorReduce(6)={{2,1},{3,1}} </text>
    </P> */
    fun factorReduce(n: Long): Array<LongArray?> {
        var n = n
        require(n >= 1) { "n<1" }
        if (n == 1L) {
            return arrayOf(longArrayOf(1, 1))
        }

        val pr: Primes = Primes.getInstance()
        var factors = arrayOfNulls<LongArray>(16)
        var count = 0
        var index = 0

        while (true) {
            val p: Long = pr.getPrime(index++)
            if (n < p) {
                break
            }
            if (n % p == 0L) {
                val pair = longArrayOf(p, 0)
                factors = ensureCapacityAndAdd(factors, pair, count)
                do {
                    pair[1]++
                    n /= p
                } while (n % p == 0L)
                count++
            }
        }


        if (n != 1L) {
            factors = ensureCapacityAndAdd(factors, longArrayOf(n, 1), count)
            count++
        }
        if (factors.size > count) {
            factors = factors.copyOf(count)
        }
        return factors
    }

    /**
     * Returns the sum of
     * `factorsAndPower[i][0] ^ factorsAndPower[i][1]`
     *
     * @param factorsAndPower an two-dimension array containing the factors and its corresponding power.
     */
    fun fromFactors(factorsAndPower: Array<LongArray>): Long {
        var re: Long = 1
        for (f in factorsAndPower) {
            re *= pow(f[0], Math.toIntExact(f[1]))
        }
        return re
    }

    /**
     * Returns the sum of
     * `p[i] ^ factorPower[i]`, where p[i] is the
     * i-th prime number starting from p[0] = 2.
     */
    fun fromFactorPowers(factorPower: IntArray): Long {
        if (factorPower.size == 0) {
            return 1
        }
        var re: Long = 1
        val pr: Primes = Primes.getInstance()
        pr.ensurePrimesNumber(factorPower.size - 1)
        for (i in factorPower.indices) {
            re *= pow(pr.getPrime(i), factorPower[i])
        }
        return re
    }


    /**
     * Returns the sum of factors of `n`.
     *
     * @param n a positive integer
     */
    fun factorSum(n: Long): Long {
        val factors = factorReduce(n)
        var sum: Long = 1
        for (factor in factors) {
            if (factor!![0] == 1L) {
                sum += 1
                continue
            }
            //1+a+a^2+...+a^p = (a^(p+1)-1)/(a-1)
            var t = pow(factor[0], Math.toIntExact(factor[1] + 1)) - 1
            t /= (factor[0] - 1)
            sum *= t
        }
        return sum
    }




    /**
     * Computes the biggest factor `result` of `n` that satisfies `p^exp = result`, where
     * `p` is an integer. This method will return an array composed of the biggest factor described ahead,
     * the integer `p`.
     *
     * For example, `integerExp(81,1/3) = (27,3)`, because `3^3 = 27`.
     *
     * @param n   a non-negative number
     * @param exp a positive fraction
     * @return an array of the biggest factor `result` and the base `p`
     */
    fun integerExpFloor(n: Long, exp: Fraction): LongArray {
        val t = integerExpCheck(n, exp)
        if (t != null) {
            return t
        }
        val factors = factorReduce(n)
        var base: Long = 1
        var result: Long = 1
        for (factor in factors) {
            val pow = factor!![1]
            val basePow = Math.toIntExact(exp.multiply(pow).floor())
            base *= pow(factor[0], basePow)
            val rePow: Int = Fraction.of(basePow).divide(exp).toInt()
            result *= pow(factor[0], rePow)
        }
        return longArrayOf(result, base)
    }

    private fun integerExpCheck(n: Long, exp: Fraction): LongArray? {
        require(n >= 0) { "n<0" }
        require(exp.isPositive()) { "exp < 0" }
        if (n == 0L) {
            if (exp.isZero) {
                ExceptionUtil.zeroExponent()
                return null
            }
            return longArrayOf(0, 0)
        }
        if (n == 1L) {
            return longArrayOf(1, 1)
        }
        return null
    }

    /**
     * Computes the smallest multiple `result` of `n` that satisfies `p^exp = result`, where
     * `p` is an integer. This method will return an array composed of the smallest multiple described ahead,
     * the integer `p`.
     *
     * For example, `integerExp(81,1/3) = (3^6,3)`.
     *
     * @param n   a non-negative number
     * @param exp a positive fraction
     * @return an array of the biggest factor `result` and the base `p`
     */
    fun integerExpCeil(n: Long, exp: Fraction): LongArray {
        val t = integerExpCheck(n, exp)
        if (t != null) {
            return t
        }
        val factors = factorReduce(n)
        var base: Long = 1
        var result: Long = 1
        for (factor in factors) {
            val pow = factor!![1]
            val basePow = Math.toIntExact(exp.multiply(pow).ceil())
            base *= pow(factor[0], basePow)
            val rePow: Int = Fraction.of(basePow).divide(exp).toInt()
            result *= pow(factor[0], rePow)
        }
        return longArrayOf(result, base)
    }
     */
}

//
fun main() {
//    println(NTFunctions.nrootFactor(-3000, 3))
    val x = BigInteger.valueOf(4 * 3)
    println(NTFunctions.nrootFactor(x, 4, 5))
    println(NTFunctions.nrootFactor(x.pow(4), 5))
//    BigInteger.valueOf(2 * 3 * 5 * 7 * 8).let {
//        println(NTFunctions.factorize(it))
//    }
}