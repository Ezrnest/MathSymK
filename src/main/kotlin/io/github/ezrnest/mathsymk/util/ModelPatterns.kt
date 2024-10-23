package io.github.ezrnest.mathsymk.util

import java.util.ArrayList
import java.util.Collections
import java.util.function.*
import java.util.function.Function

/**
 * Some useful abstract models or patterns that is used in programming.
 *
 * @author liyicheng
 */
object ModelPatterns {
    /**
     * Operates a binary search. This method is a long version of [.binarySearch]
     *
     * @param fromIndex  the lower bound, inclusive
     * @param toIndex    the upper bound, exclusive
     * @param comparator a comparator
     * @return the index of the key or (-(insertion point) - 1).
     * @see .binarySearch
     */
    fun binarySearchL(fromIndex: Long, toIndex: Long, comparator: LongToIntFunction): Long {
        require(!(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex))
        // the code copied from Arrays.binarySearch
        var low = fromIndex
        var high = toIndex - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp: Int = comparator.applyAsInt(mid)
            if (cmp < 0) low = mid + 1
            else if (cmp > 0) high = mid - 1
            else return mid // key found
        }
        return -(low + 1) // key not found.
    }

    /**
     * Operates a binary search. For example, a binary search
     * for a sorted array can be done as follow:
     * <pre>
     * final int key = ... ;
     * int index = binarySearch(0,arr.length, x-> arr[x ] < key ? -1 : arr[x ] == key ? 0 : -1 );
     *
    </pre> *
     * Note that this method doesn't supports negative values for `fromIndex` or `toIndex`,
     * and `fromIndex` should not be bigger than `toIndex`
     *
     * @param fromIndex  the lower bound, inclusive
     * @param toIndex    the upper bound, exclusive
     * @param comparator a comparator that determines whether the key is "in front of" the given index
     * (return -1), is at the index(return 0) or "behind" it (return 1), which is equal to
     * `arr[i ].compareTo(key)`
     * @return index of the search key,
     * if it is contained in the array within the specified range;
     * otherwise, (-(insertion point) - 1).
     * The insertion point is defined as the point at which the key would be inserted into the "array":
     * the index of the first element in the range greater than the key,
     * or toIndex if all elements in the range are less than the specified key.
     * Note that this guarantees that the return value will be >= 0 if and only if the key is found.
     */
    fun binarySearch(fromIndex: Int, toIndex: Int, comparator: IntUnaryOperator): Int {
        require(!(fromIndex < 0 || toIndex < 0 || fromIndex > toIndex))
        // the code copied from Arrays.binarySearch
        var low = fromIndex
        var high = toIndex - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp: Int = comparator.applyAsInt(mid)
            if (cmp < 0) low = mid + 1
            else if (cmp > 0) high = mid - 1
            else return mid // key found
        }
        return -(low + 1) // key not found.
    }

    /**
     * Solve a 'problem' with binary search method. For example, to find a function's zero point,
     * assuming the function is `f(x)` and the range to search is `[0,1]`, then
     * `binarySolve(0d,1d,(a,b)->(a+b)/2,x->signum(f(x)),100)` will try to find the zero point and iterate for 100
     * times.
     *
     * @param low        the initial downer bound
     * @param high       the initial upper bound
     * @param middle     an operator to computes the middle value of low and high
     * @param comparator to determine how the current result deviates from the desired result
     * @param maxTime    the max times to iterate
     */
    fun <T> binarySolve(low: T, high: T, middle: BinaryOperator<T>, comparator: ToIntFunction<T>, maxTime: Int): T {
        var low = low
        var high = high
        var mid: T = middle.apply(low, high)
        val cl: Int = comparator.applyAsInt(low)
        val ch: Int = comparator.applyAsInt(high)
        if (cl == 0) {
            return low
        }
        if (ch == 0) {
            return high
        }
        require(!MathUtils.isSameSign(cl, ch)) { "Sign numbers are the identity!" }

        val downerNegative = cl < 0
        for (i in 0 until maxTime) {
            val t: Int = comparator.applyAsInt(mid)
            if (t == 0) {
                return mid
            }
            if (downerNegative xor (t < 0)) {
                high = mid
            } else {
                low = mid
            }
            mid = middle.apply(low, high)
        }
        return mid
    }

    /**
     * Solve a 'problem' with binary search method. For example, to find a function's zero point,
     * assuming the function is `f(x)` and the range to search is `[0,1]`, then
     * `binarySolve(0d,1d,(a,b)->(a+b)/2,x->signum(f(x)),100)` will try to find the zero point and iterate for 100
     * times.
     *
     * @param low        the initial downer bound
     * @param high       the initial upper bound
     * @param middle     an operator to computes the middle value of low and high
     * @param comparator to determine how the current result deviates from the desired result
     * @param next       accepts the current lower bound and the higher bound to determine whether to iterate further
     */
    fun <T> binarySolve(
        low: T, high: T, middle: BinaryOperator<T>, comparator: ToIntFunction<T>,
        next: BiPredicate<T, T>
    ): T {
        var low = low
        var high = high
        var mid: T = middle.apply(low, high)
        val cl: Int = comparator.applyAsInt(low)
        val ch: Int = comparator.applyAsInt(high)
        if (cl == 0) {
            return low
        }
        if (ch == 0) {
            return high
        }
        require(!MathUtils.isSameSign(cl, ch)) { "Sign numbers are the identity!" }

        val downerNegative = cl < 0
        while (next.test(low, high)) {
            val t: Int = comparator.applyAsInt(mid)
            if (t == 0) {
                return mid
            }
            if (downerNegative xor (t < 0)) {
                high = mid
            } else {
                low = mid
            }
            mid = middle.apply(low, high)
        }
        return mid
    }

//    /**
//     * Performs an operation like computing `exp(x,p)`.
//     *
//     * @param p        a non-negative number
//     * @param unit     the unit value, such as 1.
//     * @param x        the base of the operation
//     * @param square   computes the 'square of x' formally
//     * @param multiply computes the 'multiplication' formally
//     */
//    fun <T> binaryProduce(p: Long, unit: T, x: T, square: Function<T, T>, multiply: BinaryOperator<T>): T {
//        var p = p
//        var x = x
//        require(p >= 0) { "p<0" }
//        if (p == 0L) {
//            return unit
//        }
//        var re = unit
//        while (p > 0) {
//            if ((p and 1L) != 0L) {
//                re = multiply.apply(x, re)
//            }
//            x = square.apply(x)
//            p = p shr 1
//        }
//        return re
//    }

    inline fun <T, S> binaryProductGen(
        pow: S, start: T, x: T,
        mul: (T, T) -> T,
        isPositive: (S) -> Boolean,
        isOdd: (S) -> Boolean,
        shr1: (S) -> S,
    ): T {
        var p = pow
        var cumulated = x
        var r = start
        while (isPositive(p)) {
            if (isOdd(p)) {
                r = mul(cumulated, r)
            }
            cumulated = mul(cumulated, cumulated)
            p = shr1(p)
        }
        return r

    }

    /**
     * Performs an operation like computing `exp(x,p)`.
     *
     * @param pow        a non-negative number
     * @param start     the unit value, such as 1.
     * @param x        the base of the operation
     * @param multiply computes the 'multiplication' formally
     */
    @JvmStatic
    inline fun <T> binaryProduce(pow: Long, start: T, x: T, multiply: (T, T) -> T): T {
        require(pow >= 0) { "p>0 is required, given $pow" }
        if (pow == 0L) {
            return start
        }
        var p = pow
        var cumulated = x
        var r = start
        while (p > 0) {
            if ((p and 1L) != 0L) {
                r = multiply(cumulated, r)
            }
            cumulated = multiply(cumulated, cumulated)
            p = p shr 1
        }
        return r
    }

    /**
     * Performs an operation like computing `exp(x,p)`.
     *
     * @param p        a positive number
     * @param x        the base of the operation
     * @param multiply computes the 'multiplication' formally
     */
    @JvmStatic
    inline fun <T> binaryProduce(p: Long, x: T, multiply: (T, T) -> T): T {
        require(p > 0) { "p>0 is required, given $p" }
        return binaryProduce(p - 1, x, x, multiply)
    }

//    /**
//     * Performs an operation like computing `exp(x,p)`.
//     *
//     * @param pow        a non-negative number
//     * @param start     the unit value, such as 1.
//     * @param x        the base of the operation
//     * @param multiply computes the 'multiplication' formally
//     */
//    @JvmStatic
//    fun <T> binaryProduce(pow: Long, start: T, x: T, multiply: BinaryOperator<T>): T {
//        return binaryProduce(pow, start, x) { a, b -> multiply.apply(a, b) }
//    }
//
//    /**
//     * Performs an operation like computing `exp(x,p)`.
//     *
//     * @param p        a positive number
//     * @param x        the base of the operation
//     * @param multiply computes the 'multiplication' formally
//     */
//    @JvmStatic
//    fun <T> binaryProduce(p: Long, x: T, multiply: BinaryOperator<T>): T {
//        return binaryProduce(p, x) { a, b -> multiply.apply(a, b) }
//    }

    private fun checkStartSmallerThanEnd(startInclusive: Int, endExclusive: Int) {
        require(startInclusive < endExclusive) { "startInclusive>=endExclusive" }
    }

    //    private static void checkStartSmallerThanOrEqualToEnd(int startInclusive, int endExclusive) {
    //        if (startInclusive > endExclusive) {
    //            throw new IllegalArgumentException("startInclusive>endExclusive");
    //        }
    //    }
    /**
     * Performs a binary reducing operation. It is required that `startInclusive < endExclusive`.
     * This method will divide the task to halves and compute recursively.
     * For example, assuming there is a list of int named `list`, then
     * `binaryReduce(0,list.size(),list::get,(a,b)->a+b)` computes the sum of elements in this list.
     *
     * @param startInclusive an integer, must be smaller than `endExclusive`
     * @param endExclusive   an integer, must be bigger than `startInclusive`
     * @param get            a function to get the value to reduce
     * @param operation      a binary operation to reduce two values to one
     */
    fun <T> binaryReduce(startInclusive: Int, endExclusive: Int, get: IntFunction<T>, operation: BinaryOperator<T>): T {
        checkStartSmallerThanEnd(startInclusive, endExclusive)
        return binaryReduce0(startInclusive, endExclusive, get, operation, null)
    }

    /**
     * Performs a binary reducing operation. It is required that `startInclusive <= endExclusive`.
     * This method will divide the task to halves and compute recursively. If
     * `startInclusive >= endExclusive`, then `identity.get()` will be
     * returned.
     * For example, assuming there is a list of int named `list`, then
     * `binaryReduce(0,list.size(),list::get,(a,b)->a+b,()->0)` computes the sum of elements in this list.
     *
     * @param startInclusive an integer, must be smaller than or equal to `endExclusive`
     * @param endExclusive   an integer, must be bigger than or equal to `startInclusive`
     * @param get            a function to get the value to reduce
     * @param operation      a binary operation to reduce two values to one
     * @param identity       supplies identity element
     */
    fun <T> binaryReduceWithIdentity(
        startInclusive: Int, endExclusive: Int, get: IntFunction<T>,
        operation: BinaryOperator<T>, identity: Supplier<T>
    ): T {
        return binaryReduce0(startInclusive, endExclusive, get, operation, identity)
    }

    private fun <T> binaryReduce0(
        startInclusive: Int,
        endExclusive: Int,
        get: IntFunction<T>,
        operation: BinaryOperator<T>,
        identity: Supplier<T>?
    ): T {
        if (startInclusive == endExclusive) {
            return identity?.get()
                ?: throw IllegalArgumentException("An identity is not provided when startInclusive==endExclusive")
        }
        if (startInclusive == endExclusive - 1) {
            return get.apply(startInclusive)
        }
        if (startInclusive == endExclusive - 2) {
            return operation.apply(get.apply(startInclusive), get.apply(startInclusive + 1))
        }
        val mid = (startInclusive + endExclusive) / 2
        return operation.apply(
            binaryReduce0<T>(startInclusive, mid, get, operation, identity),
            binaryReduce0<T>(mid, endExclusive, get, operation, identity)
        )
    }


    /**
     * Performs reducing operation from the left(start). It is required that `startInclusive < endExclusive`.
     *
     * @param startInclusive an integer, must be smaller than `endExclusive`
     * @param endExclusive   an integer, must be bigger than `startInclusive`
     * @param get            a function to get the value to reduce
     * @param operator       a binary operation to reduce two values to one
     */
    fun <T> reduceLeft(startInclusive: Int, endExclusive: Int, get: IntFunction<T>, operator: BinaryOperator<T>): T {
        checkStartSmallerThanEnd(startInclusive, endExclusive)
        var re: T = get.apply(startInclusive)
        for (i in startInclusive + 1 until endExclusive) {
            re = operator.apply(re, get.apply(i))
        }
        return re
    }

    /**
     * Performs reducing operation from the left(start) with an identity element provided.
     * Returns the identity if `startInclusive >= endExclusive`.
     *
     * @param startInclusive an integer
     * @param endExclusive   an integer
     * @param get            a function to get the value to reduce
     * @param operator       a binary operation to reduce two values to one
     */
    fun <T> reduceLeftWithIdentity(
        startInclusive: Int, endExclusive: Int, get: IntFunction<T>,
        operator: BinaryOperator<T>, identity: T
    ): T {
        var re = identity
        for (i in startInclusive until endExclusive) {
            re = operator.apply(re, get.apply(i))
        }
        return re
    }

    /**
     * Performs reducing operation from the right (end).
     * It is required that `startInclusive < endExclusive`.
     *
     * @param startInclusive an integer, must be smaller than `endExclusive`
     * @param endExclusive   an integer, must be bigger than `startInclusive`
     * @param get            a function to get the value to reduce
     * @param operator       a binary operation to reduce two values to one
     */
    fun <T> reduceRight(startInclusive: Int, endExclusive: Int, get: IntFunction<T>, operator: BinaryOperator<T>): T {
        checkStartSmallerThanEnd(startInclusive, endExclusive)
        var re: T = get.apply(endExclusive - 1)
        for (i in endExclusive - 2 downTo startInclusive) {
            re = operator.apply(get.apply(i), re)
        }
        return re
    }

    /**
     * Performs reducing operation from the right (end) with an identity element provided.
     * Returns the identity if `startInclusive >= endExclusive`.
     *
     * @param startInclusive an integer
     * @param endExclusive   an integer
     * @param get            a function to get the value to reduce
     * @param operator       a binary operation to reduce two values to one
     */
    fun <T> reduceRightWithIdentity(
        startInclusive: Int, endExclusive: Int, get: IntFunction<T>,
        operator: BinaryOperator<T>, identity: T
    ): T {

        var re = identity
        for (i in endExclusive - 1 downTo startInclusive) {
            re = operator.apply(get.apply(i), re)
        }
        return re
    }

    /**
     * Performs folding operation from the left(start).
     * It is required that `startInclusive < endExclusive`.
     *
     * @param initial   initial value of `R` to accumulate.
     * @param get       a function to get the value to fold
     * @param operation a binary function to fold
     */
    fun <T, R> foldLeft(
        startInclusive: Int, endExclusive: Int, initial: R, get: IntFunction<T>,
        operation: BiFunction<R, T, R>
    ): R {
        var re = initial
        for (i in startInclusive until endExclusive) {
            re = operation.apply(re, get.apply(i))
        }
        return re
    }

    /**
     * Performs folding operation from the right(end).
     * It is required that `startInclusive < endExclusive`.
     *
     * @param initial   initial value of `R` to accumulate.
     * @param get       a function to get the value to fold
     * @param operation a binary function to fold
     */
    fun <T, R> foldRight(
        startInclusive: Int, endExclusive: Int, initial: R, get: IntFunction<T>,
        operation: BiFunction<T, R, R>
    ): R {
        var re = initial
        for (i in endExclusive - 1 downTo startInclusive) {
            re = operation.apply(get.apply(i), re)
        }
        return re
    }

    /**
     * Performs reduction operation on the given range of objects using dynamic programming to
     * minimize the time cost. This method computes the time cost via 'model', which is defined by the user and
     * given by `toModel`. The user should also provide the function `modelOperation` to
     * reduce on the model and the function `modelTimeCost` to tell the time cost of a model.
     *
     * @param startInclusive an integer
     * @param endExclusive   an integer
     * @param get            a function to get the value to reduce
     * @param operation      a binary operation to reduce two values to one, must be associative
     * @param toModel        a function to covert a real object to an abstract model for computing time cost
     * @param modelTimeCost  a function to compute the time cost of a model
     */
    fun <T, R> reduceDP(
        startInclusive: Int, endExclusive: Int, get: IntFunction<T>,
        operation: BinaryOperator<T>,
        toModel: Function<T, R>,
        modelOperation: BinaryOperator<R>,
        modelTimeCost: ToIntBiFunction<R, R>
    ): T {
        //dynamic programming
        val size = endExclusive - startInclusive
        require(size > 0) { "startInclusive >= endExclusive" }
        if (size == 1) return get.apply(startInclusive)
        if (size == 2) return operation.apply(get.apply(startInclusive), get.apply(startInclusive + 1))

        val models = Array(size) {
            ArrayList<R?>(size).also { it.addAll(Collections.nCopies(size, null)) }
        }
        for (i in 0 until size) {
            models[i][i] = toModel.apply(get.apply(i))
        }
        val partitions = computeTimeCost(size, models, modelOperation, modelTimeCost)
        return recurReduce(partitions, 0, size - 1, get, operation)
    }

    /**
     * Computes the time cost and returns an array contains the best partition.
     */
    private fun <R> computeTimeCost(
        size: Int, models: Array<ArrayList<R?>>,
        modelOp: BinaryOperator<R>, timeCost: ToIntBiFunction<R, R>
    ): Array<IntArray> {
        val partitions = Array(size) { IntArray(size) }
        val costs = Array(size) { IntArray(size) }

        //costs : cost[x][y] = min cost of get(x)get(x+1)...get(y)
        //partitions : partitions[x][y] = split = split point of get(x)get(x+1)...get(y) ->
        // (get(x)get(x+1)...get(x+split))(get(x+split+1)...get(y))
        for (d in 1 until size) {
            for (i in 0 until size - d) {
                val j = i + d
                //find the cost
                var minCost = Int.MAX_VALUE
                var minSplit = 0
                var minModel: R? = null
                for (r in 0 until d) {
                    val modelLeft = models[i][i + r]!!
                    val modelRight = models[i + r + 1][j]!!
                    val cost: Int = timeCost.applyAsInt(modelLeft, modelRight) + costs[i][i + r] + costs[i + r + 1][j]
                    if (minModel == null || cost < minCost) {
                        val combined: R = modelOp.apply(modelLeft, modelRight)
                        minCost = cost
                        minSplit = r
                        minModel = combined
                    }
                }
                partitions[i][j] = minSplit
                models[i][j] = minModel!!
                costs[i][j] = minCost
            }
        }
        //        Printer.printMatrix(costs);
//        Printer.printMatrix(partitions);
//        Printer.printMatrix(ArraySup.mapTo2(models, x -> Arrays.toString((int[])x),String.class));
        return partitions
    }

    private fun <T> recurReduce(
        partitions: Array<IntArray>,
        startInclusive: Int,
        endInclusive: Int,
        get: IntFunction<T>,
        combine: BinaryOperator<T>
    ): T {
        // TODO: rewrite this method to avoid recursion
        if (startInclusive == endInclusive) {
            return get.apply(startInclusive)
        }
        if (startInclusive == endInclusive - 1) {
            return combine.apply(get.apply(startInclusive), get.apply(endInclusive))
        }
        val split = partitions[startInclusive][endInclusive]
        val left = recurReduce(partitions, startInclusive, startInclusive + split, get, combine)
        val right = recurReduce(partitions, startInclusive + split + 1, endInclusive, get, combine)
        return combine.apply(left, right)
    }

//    /**
//     * Performs a recursively reducing operation with caching. The results of function `reducing`
//     * will be cached and when the same input appears again, the cached value will be used instead of calling
//     * `reducing` again.
//     *
//     *
//     * For example, the following code computes fibonacci number of n:
//     * <pre>
//     * BiFunction&lt;Integer,Function&lt;Integer,Long>,Long> fib = (x,f) ->{
//     * if(x == 1 || x == 2) {
//     * return 1L;
//     * }
//     * return f.apply(x-1) + f.apply(x-2);
//     * };
//     * long result = cachedReduce(n,fib);</pre>
//     *
//     * @param x        the input of the problem
//     * @param reducing a reducing function that may call recursively by its second argument
//     * @param <T>      the input of the problem
//     * @param <R>      the result of the problem
//     * @return the result of computation
//    </R></T> */
//    fun <T, R> cachedReduce(x: T, reducing: BiFunction<T, Function<T, R>?, R>?): R {
//        val cached: Function<T, R> = FunctionSup.cachedRecurFunction(reducing)
//        return cached.apply(x)
//    } //    public static <T> T binarySearchFloor(int fromIndex, int toIndex, IntUnaryOperator comparator){
    //
    //    }
    //    public <T,C extends Collection<T>> C recurBuild(int index, T initial, C collection, )
    //    public static class TimeCostModel<R>{
    //        private final int timeCost;
    //        private final R model;
    //        public TimeCostModel(int timeCost, R model){
    //            this.timeCost = timeCost;
    //            this.model = model;
    //        }
    //
    //        public int getTimeCost() {
    //            return timeCost;
    //        }
    //
    //        public R getModel() {
    //            return model;
    //        }
    //    }
    //		DoubleUnaryOperator f = d -> d*d-d;
    //		print(binarySolve(0.5d, 3d, (a,b)->(a+b)/2,x-> {
    //			double t = f.applyAsDouble(x);
    //			return t < 0 ? -1 : t == 0 ? 0 : 1;
    //		}, 10));
    //        int[][] matrix = new int[][]{
    //                {4,3},
    //                {3,5},
    //                {5,4},
    //                {4,6},
    //                {6,7},
    //                {7,2},
    //                {2,3}
    //        };
    //        final int size = matrix.length;
    //        BinaryOperator<int[]> compose = (x,y)->new int[]{x[0],y[1]};
    //        int[] re = reduceDP(0,size,
    //                x -> matrix[x],compose,
    //                Function.identity(),compose,(x,y)->x[0]*y[0]*y[1] );
    //        Printer.print(re);
}
