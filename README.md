
[![](https://jitpack.io/v/ezrnest/mathsymk.svg)](https://jitpack.io/#ezrnest/mathsymk)

This project aims to provide a modern and easy-to-use computer algebra and symbolic calculation library in Kotlin.
This project tries to cover a wide range of topics in mathematics, including but not limited to:
* algebraic structures
* linear algebra
* symbolic calculation
* number theory and combinatorics
* calculus
* analytic geometry
* differential geometry

This library enables users to treat various mathematical objects in a unified way and provides a set of utilities to manipulate them. 
For example, one can consider polynomials over a field, matrices over a ring, etc.
See the Quick Start section.

The project is still under development. 
Currently, it includes
- Interfaces for algebraic structures, such as `Group`, `Ring`, `Field`, etc.
- Basic implementations of algebraic structures 
  - `Fraction`, `Complex`, etc.
  - `Polynomial`: 
    - Algebraic operations
    - gcd, factorization, etc.
  - `Multinomial`: Algebraic operations
- Linear algebra
  - `Vector`: Algebraic operations, dot product, cross product, etc.
  - `Matrix`
    - Algebraic operations, matrix multiplication
    - Determinant, inverse, etc
    - LU decomposition, Smith normal form, etc.
  - `Tensor`
    - Basic operations, slicing, broadcasting, etc.
    - Algebraic operations, einsum, etc.
- Utilities for number theory and combinatorics
  - `gcd`, `lcm`, `chineseRemainder`, `powMod`, etc.
  - Iterating over permutations, combinations, etc.
  - `Permutation`: cyclic decomposition, reverse count, etc.

# Installation

Currently, we provide the library through JitPack.
To use this library, you need to add the following to your build file.

Build-from-source requirement: JDK 25 (toolchain 25).

#### Gradle (Kotlin DSL):
* Add the following to your root `build.gradle.kts` file at the end of the repositories section:

```kotlin
maven { url = uri("https://jitpack.io") }
```
* Add the dependency to your module's `build.gradle.kts` file:

```kotlin
implementation("com.github.ezrnest:mathsymk:v0.0.2")
```
#### Maven:
* Add the following to your `pom.xml` file:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
* Add the dependency:

```xml
<dependency>
    <groupId>com.github.ezrnest</groupId>
    <artifactId>mathsymk</artifactId>
    <version>v0.0.2</version>
</dependency>
```

# Quick Start

We provide a few examples to demonstrate the usage of this library.

### Fraction
#### Arithmetic operations:
```kotlin
val a = Fraction(1, 2)
val b = Fraction(1, 3)
listOf(a + b, a - b, a * b, a / b) // 5/6, 1/6, 1/6, 3/2
listOf(a.pow(2), a.pow(-1)) // 1/4, 2
```

#### Fraction fields:
```kotlin
// First example: fractions over integers, just as plain fractions
val Z = Models.ints()
with(RFraction.over(Z)) {
    val f1 = frac(3, 4)
    val f2 = frac(1, 2)
    f1 + f2 // 5/4
}

// Second example: fractions over polynomials
val polyF = Polynomial.over(Z) // Polynomials over Z
with(polyF) {
    with(RFraction.over(polyF)) { // Fraction of polynomials over Z
        val f1 = (1 + x) / (1 + 2.x) // an overloaded operator `/` to create a fraction
        val f2 = (2 + 3.x) / (1 + 2.x)
        f1 + f2 // (3 + 4x)/(1 + 2x)
        f1 / f2 // (1 + x)/(2 + 3x)

        val f3 = x / (1 + 2.x)
        f1 + f3 // 1

        val f4 = (1 + x) / (1 + 3.x)
        f1 + f4 // (5*x^2 + 7*x + 2)/(6*x^2 + 5*x + 1)
    }
}
```


### Complex numbers

#### Complex of Double:
```kotlin
val z1 = Complex128(1.0, 2.0)
val z2 = Complex128(3.0, 4.0)
listOf(z1 + z2, z1 - z2, z1 * z2, z1 / z2) // (4.0, 6.0), (-2.0, -2.0), (-5.0, 10.0), (0.44, 0.08)
listOf(z2.mod,z2.arg) // 5.0, 0.93
```

#### Complex over Various Models:
```kotlin
val Z = Models.ints()
val GaussianInt = Complex.over(Z)
with(GaussianInt){
    val z1 = 1 + 2.i
    val z2 = 3 + 4.i
    listOf(z1 + z2, z1 - z2, z1 * z2) // (4, 6), (-2, -2), (-5, 10)
}

val Q = Models.fractions()
val complexQ = Complex.over(Q) // Complex numbers with rational components
with(Q){
    with(complexQ) {
        val z1 = frac(1, 2) + frac(1, 3).i
        val z2 = frac(1, 4) + frac(1, 5).i
        listOf(z1 + z2, z1 - z2, z1 * z2)
        // (3/4, 8/15), (1/4, 2/15), (7/120, 11/60)
    }
}
```

### Polynomial
```kotlin
val Z97 = Models.intModP(97) // Integers mod 97, Z/97Z, a field
val polyZ = Polynomial.over(Z97) // Polynomials over Z/97Z
with(polyZ) {
    val f = x+1 // predefined variable x polyZ
    val g = x-1
    val p1 = f * g // x^2 - 1
    val p2 = f pow 2 // (x+1)^2 = x^2 + 2x + 1
    gcd(p1,p2).toMonic() // x + 1 
}
```

### Multinomial




### Matrix and vector
```kotlin
val ℤ = Models.ints()
with(Matrix.over(ℤ)) {
  val n = 3
  val A = Matrix(n) { i, j -> (5 * cos(i + 2.0 * j)).toInt() } // generate a non-singular matrix
  A.det() // determinant
  val u = Vector(n) { i -> i + 1 }
  A * u // matrix-vector multiplication
  val B = A * A.T // Matrix multiplication and transpose
  u.T * B * u // quadratic form
}
```
#### Characteristic polynomial
```kotlin
val ℤ = Models.ints()
val n = 4
with(Matrix.over(ℤ,n)){
  val A = Matrix(n) { i, j -> i + 2 * j }
  val p = A.charPoly() // the characteristic polynomial of A, p(λ) = det(λI - A)
  println(p.toString(ch = "λ"))
  println("Substitute A into p(λ) = det(λI - A), is it zero?")
  println(isZero(p.substitute(A, this@with))) // p(A) = 0, a matrix of zeros
}
```


### Tensor

#### Basic usage
```kotlin
val a = Tensor(2, 3, 2, 5) { (i, j, k, l) -> i + j } // create a 4D tensor with shape (2, 3, 2, 5)
val b = Tensor.ofFlat(intArrayOf(2,3,2,5), 0 until 60) // another way to create a tensor
// a is a 4D tensor with shape (2, 3, 2, 5)
a[intArrayOf(1, 2, 1, 3)] // get a single element
a[0, 1, 1, 2] // get a single element, operator overloaded version, need `import io.github.ezrnest.linear.get`
a[-1, 1..2, null, 0..<5 step 2] // slicing, need `import io.github.ezrnest.linear.get`
a.slice(-1, 1..2, null, 0..<5 step 2) // slicing, another way
a[1, Tensor.DOTS, 1, Tensor.NEW_AXIS, 2] // support omitting some axes and adding new axes
```

#### Tensor Multiplication
```kotlin
with(Tensor.over(Models.ints())) {
  val a = Tensor.ofFlat(intArrayOf(3, 4, 5), 0 until 60)
  val b = Tensor.ofFlat(intArrayOf(4, 3, 2), 0 until 24)
  // The following three ways give the same result:
  val res0 = zeros(5, 2).also { res0 ->
    for ((i, j, k, n) in IterUtils.prodIdx(intArrayOf(5, 2, 3, 4))) {
      res0[i, j] += a[k, n, i] * b[n, k, j] // direct computation
    }
  }
  val res1 = a.permute(2, 1, 0).matmul(b, 2) // matmul at the last 2 axes
  val res2 = einsum("kni,nkj->ij", a, b) // einsum
}

```

#### More samples can be found in the `samples` directory.

# Features

## Stability

Current module stability levels and known unimplemented runtime paths are documented in:
- `docs/API_STABILITY.md`


## Number Models
This library provides a set of interfaces for algebraic structures, such as `Group`, `Ring`, `Field`, etc.
We refer to concrete implementations of these interfaces as **models**.
We list a few examples of algebraic structures and their models:
- `Monoid`: `String` (with concatenation), `List`, `Set`, etc.
- `Group`: `Permutation`, `Matrix`(invertible), etc.
- `Ring`: `Int`, `Polynomial`, etc.
- `Field`: `Double`, `Fraction`, `Complex`, etc.

## Flexibility of choosing models
Many mathematical objects can be treated in different ways depending on the context, 
or the underlying **model**. 
For example, we can consider polynomials over a ring, a field, or an integral domain.
This library provides the flexibility to choose the model for the objects, 
allowing users to work with different models in a unified way.
One can easily build up a matrix of integers or a matrix of polynomials over a field.


# Development

Currently, this project will be migrated and redesigned from my legacy math lib [Ancono](https://github.com/140378476/Ancono),
but the new project will be more modular and flexible.

I am very glad to receive any feedback or suggestions.
Please feel free to open an issue or pull request.
Any contribution is welcome.

## Next Steps
- Preliminary implementation of symbolic calculation
- Quotient rings and fields
- Finite fields
- Geometry 2D and 3D


## Future Roadmap
- [ ] Preliminary implementation of symbolic calculation
- [ ] Geometry related
- [ ] Implement more algebraic structures
- [ ] Symbolic differentiation and integration
