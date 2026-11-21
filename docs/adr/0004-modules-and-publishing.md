# ADR-0004: `:library` and `:examples`, no benchmark module

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

The library needs a published artifact and runnable proof that a caller can
integrate it with `terminal-window` without the library owning a window. A
single module would mix public API, tests, host natives, example applications,
and publication configuration.

The project is not performance-bound in the same way as the host renderer:
the GUI's correctness is measured by structural cell grids, layout invariants,
and event behavior. A timing suite would add hardware-dependent flakiness and
would not prove a widget is correct.

## Decision

We will use two Gradle modules:

| Module | Role | Published |
|---|---|---|
| `:library` | Public `terminal-window-gui` artifact, tests, Dokka, publication | Yes |
| `:examples` | Gallery, TODO, dashboard, custom widget, smoke proofs, host natives | No |

There is no `:benchmarks` module and no timing gate.

Rules:

1. `:library` does not depend on `:examples`.
2. `:examples` depends on `:library` and on the host runtime/natives.
3. Only `:library` is signed and published to Maven Central.
4. `:examples` may open a `TerminalWindow`; the library may not.
5. Build logic shared by both modules lives in the root build and `ci/*.sh`.
6. Golden and component tests live in `:library` test source, not in
   `:examples`.
7. The Gradle project list must contain `:`, `:library`, and `:examples` only.

## Consequences

### Positive

- Published metadata contains only the library and its host dependency.
- Examples can use host natives without leaking them into the consumer POM.
- Correctness gates are deterministic and independent of GPU speed.
- A clean consumer can be checked against `:library` alone.

### Negative

- Two module builds and a root aggregator add configuration overhead.
- Example-only code cannot be reused as library code without moving it and
  reviewing its API.
- No benchmark module means no performance trend report; the project accepts
  that gap.

### Neutral

- Section 02 creates the Gradle settings and module builds.
- Section 13 owns the examples and their smoke task.
- Section 15 publishes only `:library`.

## Alternatives

| Alternative | Why rejected |
|---|---|
| One module | Mixes examples, natives, tests, and publication; consumers may resolve example dependencies. |
| Publish `:examples` | Examples are not a supported API and would double the release surface. |
| Add `:benchmarks` | The GUI is correctness-bound; timing asserts are excluded by the testing policy. |
| Put examples in `:library` test sources | Tests are not runnable applications and would ship with the artifact metadata. |
| Use a composite build | Adds a second build lifecycle for no benefit at this size. |

## References

- `GUIDE.md` modules and no-benchmark invariant.
- `docs/guide/01-charter-scope-and-roadmap.md`, steps 4 and 11.
- `terminal-window` `settings.gradle.kts` module pattern.
