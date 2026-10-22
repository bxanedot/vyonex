# VyonEx JSON Layer Design

## Goal

Replace the bundled JSON parser with a VyonEx-owned JSON layer while preserving the behavior used by the current Java extractor code.

## Scope

- Add a VyonEx-owned JSON API in `com.vyonex.internal.json`, backed by the existing `kotlinx.serialization-json` dependency.
- Migrate current JSON call sites from the bundled package to VyonEx without changing the public `com.vyonex` API or extractor behavior.
- Remove the bundled parser source, its attribution row, and its standalone Apache license copy after the call sites compile against the replacement.
- Keep ordinary Maven dependencies and the existing GPL-3.0-only project license unchanged.

This is the first subproject of the larger clean-room rewrite. It does not remove code derived from the merged extractor projects, the EJS resources, the Zemer config URL, or their corresponding notices; those remain until their own replacements are completed.

## Compatibility surface

The VyonEx JSON package supplies only operations used by current source: ordered JSON objects and arrays, typed accessors and defaults, strict object/array parsing from text or streams, JSON serialization, and nested request builders. It uses the existing Kotlin serialization engine, adds no dependencies, and exposes no JSON types through the public VyonEx API.

Parsing errors are reported as `JsonParserException`. Number values must preserve integer precision and support the numeric conversions used by callers. Object and array serialization must escape strings correctly and round-trip nested values, nulls, booleans, and numbers.

## Migration

Replace imports from the bundled parser package with VyonEx JSON imports. Keep existing service code call sites stable where they use the required surface; implement the layer independently. Delete the old package only after no production or test source imports it.

## Verification

- Add offline tests first for nested object/array parsing, typed accessors and defaults, large integers, malformed input, string escaping, builder nesting, and serialization round trips.
- Run the focused JSON tests, then the full Gradle test suite and `jar` task.
- Scan tracked source and the built JAR for the retired package and parser file paths.
- Confirm `ATTRIBUTION.md` still credits remaining adapted code and data sources; remove only the replaced parser entry and its license copy once its source is gone.

## Constraints and risks

- No public API signatures may change.
- No network-backed test fixtures; JSON fixtures are synthetic and stored locally.
- The extractor code still has upstream lineage after this subproject, so the repository must retain its applicable notices until later rewrites are complete.
- Java/Kotlin interop and numeric coercion are the main compatibility risks; tests must cover those before deleting the old parser.
