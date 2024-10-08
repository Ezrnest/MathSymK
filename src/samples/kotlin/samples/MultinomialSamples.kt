package samples

import io.github.ezrnest.model.Multinomial
import io.github.ezrnest.model.Models


fun buildMultinomial(){
    val Z = Models.ints() // Integers, Z
    val mult = Multinomial.over(Z)
    with(mult) {
        val m = x - 2 * y - y * z + 4 * x.pow(2)
        println(m)
    }
}

fun main() {
    buildMultinomial()
}