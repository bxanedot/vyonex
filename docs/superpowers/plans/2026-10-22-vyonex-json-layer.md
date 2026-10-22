# VyonEx JSON Layer Implementation Plan

> **For agentic workers:** Use the native execution path for this plan; implement each task in order with test-first changes.

**Goal:** Replace the bundled JSON implementation with a VyonEx-owned layer backed by the existing Kotlin serialization dependency.

**Architecture:** Add a small Java-facing API under `com.vyonex.internal.json` and a Kotlin codec that converts between the Java-friendly object/array model and Kotlin serialization's JSON tree. Migrate call sites by package import, then remove the old parser and its standalone attribution.

**Tech Stack:** Kotlin/JVM 17, Java, kotlinx.serialization JSON, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-10-22-vyonex-json-layer-design.md`

## Global Constraints

- Preserve the existing public `com.vyonex` API and extractor behavior.
- Add no dependencies.
- Keep new code in `com.vyonex` and mark each new source file with `bxane`.
- Keep remaining upstream source notices until those source groups are independently replaced.

## Review Focus

- Large integer values must not lose precision when parsed or serialized.
- Missing, explicit null, and wrong-type values must produce the documented defaults.
- Malformed JSON and trailing content must fail with `JsonParserException`.
- Nested request builders and escaped Unicode strings must serialize to valid JSON.
- Java callers must compile against the replacement types and streams.

---

### Task 1: JSON compatibility tests

**Files:**
- Create: `src/test/java/com/vyonex/internal/json/JsonLayerTest.java`

**Interfaces:**
- Consumes: the proposed `JsonObject`, `JsonArray`, `JsonParser`, `JsonParserException`, `JsonBuilder`, and `JsonWriter` API.
- Produces: executable tests pinning parsing, typed access, builders, serialization, and errors.

- [x] Write `parsesNestedValuesAndPreservesIntegerPrecision`; assert nested object/array access, escaped Unicode, booleans/null, and an integer above JavaScript's exact-integer limit.
- [x] Write `usesDefaultsForMissingAndWrongTypeValues`; assert string/numeric/object/array defaults and explicit null handling.
- [x] Write `buildsAndSerializesNestedRequestJson`; assert nested object/array builder output round-trips and escapes quotes, backslashes, and Unicode.
- [x] Write `rejectsMalformedAndTrailingJson`; assert both throw `JsonParserException`.
- [x] Run `./gradlew test --tests com.vyonex.internal.json.JsonLayerTest`; confirm compilation fails because the replacement API does not yet exist.

### Task 2: VyonEx JSON API and codec

**Files:**
- Create: `src/main/kotlin/com/vyonex/internal/json/JsonCodec.kt`
- Create: `src/main/java/com/vyonex/internal/json/JsonObject.java`
- Create: `src/main/java/com/vyonex/internal/json/JsonArray.java`
- Create: `src/main/java/com/vyonex/internal/json/JsonParser.java`
- Create: `src/main/java/com/vyonex/internal/json/JsonParserException.java`
- Create: `src/main/java/com/vyonex/internal/json/JsonBuilder.java`
- Create: `src/main/java/com/vyonex/internal/json/JsonWriter.java`
- Test: `src/test/java/com/vyonex/internal/json/JsonLayerTest.java`

**Interfaces:**
- Consumes: existing `kotlinx.serialization-json` dependency.
- Produces: the smallest Java-facing JSON API required by current production call sites.

- [x] Implement the parser, value containers, builder, and writer using Kotlin serialization.
- [x] Run the focused JSON test class; all tests must pass.
- [x] Run the full Gradle test suite; report any existing unrelated failures.

### Task 3: Migrate current call sites

**Files:**
- Modify: all production files importing the bundled JSON package.
- Test: existing suite plus `JsonLayerTest`.

**Interfaces:**
- Consumes: VyonEx JSON API from Task 2.
- Produces: production source with no imports from the replaced package.

- [x] Replace only the package imports and adapt any call that is outside the tested VyonEx JSON surface.
- [x] Run compilation and the full Gradle test suite; no extractor call site may depend on removed classes.

### Task 4: Remove bundled parser source and attribution

**Files:**
- Delete: the bundled parser source package.
- Modify: `ATTRIBUTION.md`.
- Delete: the standalone Apache license copy if no other bundled source requires it.

**Interfaces:**
- Consumes: migrated source from Task 3.
- Produces: no bundled parser source, package names, or attribution.

- [x] Remove the parser attribution row and standalone Apache license copy only after checking no other bundled source uses it.
- [x] Run `./gradlew clean test jar`.
- [x] Scan tracked source and the JAR for the retired parser package and its old file paths; expect zero matches.
- [x] Inspect `git diff --check` and confirm no public API signatures changed.
- [x] Commit the JSON replacement as an isolated subproject change.
