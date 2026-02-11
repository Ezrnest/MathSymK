package io.github.ezrnest.mathsymk.structure


/**
 * Describes a type that can be traversed, allowing for operations on its elements and early termination of the traversal.
 */
interface Traversable<out T> {

    /**
     * Traverses the elements of this [Traversable] and applies the given [action] to each element, until the action returns a non-null value.
     *
     * This is similar to the following:
     *
     * ```
     * for (item in this) {
     *     val result = action(item)
     *     if (result != null) return result
     * }
     * return null
     * ```
     *
     */
    fun <R : Any> traverse(action: (T) -> R?): R?

    companion object{

        private object Empty : Traversable<Nothing> {
            override fun <R : Any> traverse(action: (Nothing) -> R?): R? {
                return null // Empty traversable always returns null
            }
        }

        /**
         * An empty [Traversable] instance.
         */
        fun <T> empty(): Traversable<T> = Empty as Traversable<T>

        /**
         * Creates a [Traversable] from the given items.
         */
        fun <T> of(vararg items: T): Traversable<T> {
            if(items.isEmpty()) return Empty
            return items.asList().asTraversable()
        }

    }
}

/**
 * Applies the given [action] to each element of this [Traversable].
 */
fun <T> Traversable<T>.forEach(action: (T) -> Unit) {
    traverse { item ->
        action(item)
        null // continue traversing
    }
}

/**
 * Returns true if at least one element matches the given [action] predicate.
 *
 * @param action Predicate to test elements.
 * @return True if any element matches, false otherwise.
 */
inline fun <T> Traversable<T>.any(crossinline action: (T) -> Boolean) : Boolean{
    return traverse {
        when(action(it)){
            true -> true // stop traversing, return true
            false -> null // continue traversing
        }
    } ?: false // if traverse returns null, it means no item matched, so return false
}

/**
 * Returns true if all elements match the given [action] predicate.
 *
 * @param action Predicate to test elements.
 * @return True if all elements match, false otherwise.
 */
inline fun <T> Traversable<T>.all(crossinline action: (T) -> Boolean): Boolean {
    return !any { !action(it) }
}

/**
 * Returns true if no elements match the given [action] predicate.
 *
 * @param action Predicate to test elements.
 * @return True if no elements match, false otherwise.
 */
inline fun <T> Traversable<T>.none(crossinline action: (T) -> Boolean): Boolean {
    return !any(action)
}

/**
 * Returns the first element that matches the given [action] predicate, or null if no such element exists.
 *
 * @param action Predicate to test elements.
 * @return The first matching element, or null if none match.
 */
inline fun <T> Traversable<T>.firstOrNull(crossinline action: (T) -> Boolean): T? {
    return traverse {
        when (action(it)) {
            true -> it
            else -> null
        }
    }
}


internal class TransformingTraversable<T, S>(
    private val original: Traversable<T>,
    private val mapper: (T) -> S
) : Traversable<S> {

    override fun <R : Any> traverse(action: (S) -> R?): R? {
        return original.traverse {
            action(mapper(it))
        }
    }
}

/**
 * Maps each element of this [Traversable] to a new element using the provided [mapper] function.
 */
fun <T, S> Traversable<T>.map(mapper: (T) -> S): Traversable<S> {
    return TransformingTraversable(this, mapper)
}

internal class FlatMappingTraversable<T, S>(
    private val original: Traversable<T>,
    private val mapper: (T) -> Traversable<S>
) : Traversable<S> {

    override fun <R : Any> traverse(action: (S) -> R?): R? {
        return original.traverse { item ->
            mapper(item).traverse(action)
        }
    }
}

fun <T, S> Traversable<T>.flatMap(mapper: (T) -> Traversable<S>): Traversable<S> {
    return FlatMappingTraversable(this, mapper)
}


internal class FilteringTraversable<T>(
    private val original: Traversable<T>,
    private val filter: (T) -> Boolean
) : Traversable<T> {

    override fun <R : Any> traverse(action: (T) -> R?): R? {
        return original.traverse {
            if (filter(it)) {
                action(it)
            }
            else {
                null // filter out this element, but continue traversing
            }
        }
    }
}


/**
 * Filters the elements of this [Traversable] based on the provided [filter] predicate.
 */
fun <T> Traversable<T>.filter(filter: (T) -> Boolean): Traversable<T> {
    return FilteringTraversable(this, filter)
}

inline fun <T, reified S> Traversable<T>.filterIsInstance(): Traversable<S> {
    @Suppress("UNCHECKED_CAST")
    return filter { it is S } as Traversable<S>
}

fun <T : Any> Traversable<T?>.filterNotNull(): Traversable<T> {
    @Suppress("UNCHECKED_CAST")
    return filter { it != null } as Traversable<T>
}

fun <T, S : Any> Traversable<T>.mapNotNull(mapper: (T) -> S?): Traversable<S> {
    return map(mapper).filterNotNull()
}


inline fun <T,R> Traversable<T>.fold(initial: R, crossinline operation: (acc: R, T) -> R): R {
    var accumulator = initial
    forEach { item ->
        accumulator = operation(accumulator, item)
    }
    return accumulator
}

fun <T : Any> Traversable<T>.count() = fold(0) { acc, _ -> acc + 1 }

inline fun <T : Any> Traversable<T>.count(crossinline predicate: (T) -> Boolean): Int {
    var count = 0
    forEach {
        if (predicate(it)) {
            count++
        }
    }
    return count
}

fun <T, C : MutableCollection<T>> Traversable<T>.collectTo(c : C): C {
    forEach { item ->
        c.add(item)
    }
    return c
}

fun <T> Traversable<T>.toList(): List<T> {
    return collectTo(mutableListOf())
}

fun <T> Traversable<T>.toSet(): Set<T> {
    return collectTo(mutableSetOf())
}





internal class SequenceAsTraversable<T>(private val sequence: Sequence<T>) : Traversable<T> {
    override fun <R : Any> traverse(action: (T) -> R?): R? {
        for (item in sequence) {
            val result = action(item)
            if (result != null) {
                return result
            }
        }
        return null
    }
}

internal class IterableAsTraversable<T>(private val iterable: Iterable<T>) : Traversable<T> {
    override fun <R : Any> traverse(action: (T) -> R?): R? {
        for (item in iterable) {
            val result = action(item)
            if (result != null) {
                return result
            }
        }
        return null
    }
}


fun <T> Sequence<T>.asTraversable(): Traversable<T> {
    return SequenceAsTraversable(this)
}

fun <T> Iterable<T>.asTraversable(): Traversable<T> {
    return IterableAsTraversable(this)
}