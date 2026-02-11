# MathSymK Agent Guide

This document is a quick operational guide for contributors and coding agents working in this repository.

## 1. Project Snapshot

- Name: `MathSymK`
- Type: Kotlin/JVM library for symbolic math and algebraic structures
- Build system: Gradle Kotlin DSL (single module)
- Group/Version: `io.github.ezrnest` / `0.0.2`
- Java toolchain: 25
- Kotlin plugin: 2.3.10
- Gradle wrapper: 9.1.0

Primary source roots:
- `src/main/kotlin`
- `src/test/kotlin`
- `src/samples/kotlin`

## 2. Module Map (Main Code)

Top-level package: `io.github.ezrnest.mathsymk`

Major domains:
- `structure`: abstract algebra interfaces and laws (`Group`, `Ring`, `Field`, etc.)
- `model`: concrete math models (`Fraction`, `Complex`, `Polynomial`, `Multinomial`, etc.)
- `linear`: linear algebra and tensors (`Vector`, `Matrix`, `Tensor`)
- `symbolic`: symbolic expression system, rewrite/match/transform rules
- `numTh`: number theory utilities
- `discrete`: combinatorics and permutations
- `geometry`: plane/space primitives
- `function`, `util`: shared utilities and helpers

## 3. Tests and Examples

Tests are under `src/test/kotlin` and currently cover:
- model layer: fractions/complex/polynomial/multinomial/quaternion
- linear layer: matrix/vector/tensor
- discrete and number theory utilities

Samples are under `src/samples/kotlin`.
A dedicated `samples` source set is configured and depends on `main` output.

## 4. Build and Run Commands

Use wrapper commands from repository root.

- Run all tests:
  - `sh gradlew test`
- Build jars:
  - `sh gradlew build`
- Clean + test:
  - `sh gradlew clean test`

Notes:
- `gradlew` may not have execute bit in some environments; `sh gradlew ...` is safe.
- `tasks.test` uses JUnit Platform.

## 5. Dependency and Repository Notes

Current configured repositories:
- `mavenCentral()`
- Aliyun public mirror
- `jitpack.io`

Publishing-related config exists for Maven Central (OSSRH), but signing/publication credentials are placeholders in `gradle.properties`.
Do not commit real secrets.

## 6. Current Development Context

- Build is currently configured for Java toolchain 25.
- Gradle/Kotlin have been upgraded in build scripts (Kotlin 2.3.10, Gradle 9.1.0).
- If future dependency upgrades are done, re-run full tests before release.

## 7. Suggested Agent Workflow

1. Read `build.gradle.kts` and affected package(s) before editing.
2. Prefer minimal, localized changes; preserve API behavior unless task requires breaking change.
3. Run `sh gradlew test` after code changes.
4. For API-facing changes, update README/sample snippets if impacted.
5. Avoid touching signing/publishing secrets and local proxy config unless explicitly requested.

## 8. High-Risk Areas

- `symbolic` rewrite/matching logic: behavior-sensitive and easy to introduce subtle regressions.
- `linear` tensor indexing/slicing/einsum operations: edge cases and shape compatibility.
- Generic algebraic abstractions in `structure`/`model`: type constraints and operator overload consistency.

When modifying these areas, add or adjust tests in the corresponding package.

## 9. Symbolic Development Notes (Current)

### 9.1 Existing Symbolic Test Coverage Added

Symbolic-related tests now include:
- `src/test/kotlin/symbolic/NodeTest.kt`
  - Node symbol mapping behavior (`Node1/Node2/Node3.mapSymbol`).
- `src/test/kotlin/symbolic/MatcherTest.kt`
  - Reference binding consistency for repeated matcher refs.
- `src/test/kotlin/symbolic/RuleSetDSLTest.kt`
  - DSL validation for unbound references in rule results.
  - No-op rule (`x -> x`) should not be registered.
- `src/test/kotlin/symbolic/TreeDispatcherTest.kt`
  - Wildcard/fixed/branch dispatch basics and early-stop behavior.
- `src/test/kotlin/symbolic/AlgReduceTest.kt`
  - Core algebraic reduction smoke tests for `ComputePow`, trig special angles, and product merge.

### 9.2 Known Behavioral Sensitivities

- `symbolic/Node.kt`: `mapSymbol` correctness is critical (node symbol + child symbol mapping).
- `symbolic/SimRuleDSL.kt`: rule target/result reference consistency must be enforced.
- `symbolic/TreeDispatcher.kt`: dispatch semantics differ by matcher category (wildcard/fixed/branch).

### 9.3 Testing Policy for Underspecified Symbolic Semantics

When semantics are not finalized (example: `0^0`), do not lock tests to current behavior.
- Prefer asserting stable identities only (e.g., `0^positive -> 0`).
- Leave explicitly underspecified cases unconstrained, with a short comment in test code.
- If product/design decisions later define those semantics, add strict assertions then.

### 9.4 Practical Workflow for Symbolic Changes

1. Add/adjust focused symbolic tests first.
2. Run targeted symbolic tests:
   - `sh gradlew test --tests 'symbolic.*'`
3. Then run full suite:
   - `sh gradlew test`
4. If a test reveals a real bug, prefer a minimal fix and keep regression coverage.
