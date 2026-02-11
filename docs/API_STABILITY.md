# API Stability and Known Gaps

This document defines the current stability level of MathSymK modules and lists known unimplemented runtime paths.

## Stability Levels

- Stable: core APIs with regular tests and no known runtime `TODO()` branches in common paths.
- Beta: usable APIs with partial coverage or known edge-case gaps.
- Experimental: APIs under active design; behavior and signatures may change.

## Module Status

- `model`: Beta
  - Widely used core types are available (`Fraction`, `Complex`, `Polynomial`, `Multinomial`).
  - There are still unimplemented branches in model construction/parsing helpers.
- `linear`: Beta
  - Matrix/Vector/Tensor features are implemented and tested.
  - Some advanced/optimization paths are marked TODO (non-blocking for common use).
- `discrete`: Beta
  - Permutation and core combinatorics are available.
  - Some advanced combinatorics areas are still evolving.
- `numTh`: Stable (current scope)
  - Core number-theory utilities are present and tested.
- `structure`: Stable (interface layer)
  - Algebraic abstractions are mostly complete as interfaces.
- `symbolic`: Experimental
  - Core expression/rule infrastructure exists.
  - Multiple runtime `TODO()` branches remain; APIs may evolve.
- `geometry`: Experimental
  - Basic point/segment primitives exist; coverage is limited.
- `function`, `util`: Beta

## Runtime `TODO()` List (main source set)

The following locations currently contain executable `TODO()`/`TODO("Not yet implemented")` in `src/main/kotlin`:

- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Models.kt:592`
- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Models.kt:596`
- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Models.kt:600`
- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Models.kt:604`
- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Models.kt:608`
- `src/main/kotlin/io/github/ezrnest/mathsymk/model/Multinomial.kt:590`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/SimRuleDSL.kt:177`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/ExprCal.kt:53`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/ExprCal.kt:120`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/alg/ExprCalReal.kt:46`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/alg/ExprCalReal.kt:163`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/alg/ExprCalReal.kt:167`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/SimUtils.kt:89`
- `src/main/kotlin/io/github/ezrnest/mathsymk/symbolic/SimUtils.kt:96`

## Upgrade Policy

- For release readiness, do not add new public API methods with runtime `TODO()`.
- For each `TODO()` removal, add or update tests in `src/test/kotlin` in the same domain.
- Keep this document synchronized with code after each iteration.
