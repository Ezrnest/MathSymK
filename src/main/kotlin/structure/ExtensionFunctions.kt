package cn.mathsymk.structure

inline fun <T, C : EqualPredicate<T>, R> C.eval(block: C.() -> R): R = this.run(block)
