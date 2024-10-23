package io.github.ezrnest.mathsymk.model.struct

/**
 * Describes a number model which is suitable for a field.
 */
interface FieldModel<T : FieldModel<T>> : DivisionRingModel<T> {

    override val isInvertible: Boolean
        get() = !isZero
}