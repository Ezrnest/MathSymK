package io.github.ezrnest.mathsymk.structure

interface InclusionTo<T, S:Any> {
    fun include(t : T) : S
}

// not possible to define this interface:
// Accidental override: The following declarations have the same JVM signature (plus(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;):

//interface InclusionToAddGroup<T, S:Any> : InclusionTo<T, S>,AddGroup<S> {
//    operator fun T.unaryPlus() : S = include(this)
//
//    operator fun T.plus(s : S) : S = include(this) + s
//
//    operator fun S.plus(t : T) : S = this + include(t)
//
//    operator fun T.minus(s : S) : S = include(this) - s
//
//    operator fun S.minus(t : T) : S = this - include(t)
//}
//
//
//interface InclusionToMulSemigroup<T, S:Any> : InclusionTo<T, S>,MulSemigroup<S> {
//    operator fun T.times(s : S) : S = include(this) * s
//
//    operator fun S.times(t : T) : S = this * include(t)
//}
//
//
//interface InclusionToRing<T, S:Any> : InclusionToAddGroup<T, S>,InclusionToMulSemigroup<T,S>,Ring<S> {
//}
//
//
//interface InclusionToMulGroup<T, S:Any> : InclusionToMulSemigroup<T, S>,MulGroup<S> {
//    operator fun T.div(s : S) : S = include(this) / s
//
//    operator fun S.div(t : T) : S = this / include(t)
//}
//
//interface InclusionToDivisionRing<T, S:Any> : InclusionToRing<T, S>,InclusionToMulGroup<T,S>,DivisionRing<S> {
//}