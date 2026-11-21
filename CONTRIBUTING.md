# Contributing

`terminal-window-gui` is a Kotlin/JVM library built on `terminal-window`. The
library is passive: it never owns a window, thread, event loop, or GL context.

## Environment

- JDK 25, with `java` on `PATH`.
- Kotlin 2.4.21 and the checked-in Gradle 9.9.1 wrapper.
- Linux GL tests need Xvfb, Mesa, and GLFW's X11 libraries.
- macOS example runs need `-XstartOnFirstThread`.

## Local gates

Use the same sequence as CI:

```sh
./gradlew spotlessApply
./gradlew check
```

Run component and GL checks:

```sh
xvfb-run -a -s "-screen 0 1280x1024x24" ./gradlew integrationTest
xvfb-run -a -s "-screen 0 1280x1024x24" ./gradlew fullTest
```

Run the full build, examples, Dokka, local publication, and the automatic-module
check:

```sh
bash ci/build.sh
bash ci/check-module.sh
```

Check documentation links, the README line budget, and API/docs gates:

```sh
bash ci/check-docs.sh
./gradlew :library:test --tests '*ApiSurfaceTest' --tests '*WidgetConformanceTest'
```

Dokka runs with `failOnWarning` and `reportUndocumented`; a public declaration
without KDoc fails the build. Do not suppress these gates.

## Documentation

- Public declarations need KDoc with `@since 1.0.0` and `@param`, `@return`,
  or `@throws` where applicable.
- Update the owning page under `docs/`; do not duplicate a contract across
  several files.
- Add every user-visible change to `CHANGELOG.md` under `[Unreleased]`.
- Do not claim window ownership, a built-in loop, background repaint, text
  attributes, wide characters, IME, images, or OS clipboard in v1.
- README code blocks must compile or be marked `text`; the quickstart is
  mirrored by `ReadmeQuickstartTest`.
- Do not add performance tables or benchmark claims.

## Tests and examples

- Component tests use `GuiTestHarness` and structural rows, not screenshots.
- GL tests are tagged `gl`, run under Xvfb, and assert `drawCalls > 0`.
- Property/fuzz tests use fixed seeds and a reference model.
- `-PupdateGoldens` is local only; CI never updates goldens.
- No timing assertions and no benchmark module.
- Examples build without a display; only their smoke run opens a window.
- Add a regression test for a bug when the existing suite does not cover the
  path. Do not add tests for wiring, wording, or tautologies.

## API and dependencies

- Public API changes follow [compatibility](docs/compatibility.md): additions
  are minor, removals are major, and data-class shapes are frozen. Update
  [`public-api.txt`](library/src/test/resources/public-api.txt) and
  [`public-api.md`](docs/public-api.md) in the same review.
- No public inline functions, mutable size fields, `!!`, or coroutines.
- `ApiSurfaceTest` must pass without weakening the inventory. An `internal`
  helper leaking into a public signature is a source fix, not a test exception.
- Never expose `org.lwjgl.*`, GLFW, GL, window handles, or service types.
- No `terminal-window` changes in 1.x. The host owns GLFW, GL, and the frame.
- Do not add a dependency without an ADR. The library POM must not contain
  native classifiers.
- No Private Use Area code points; use the semantic `Shape` roles.

## Release

Releases are local: `release/1.0`, a signed `v1.0.0` tag, `publishToMavenLocal`,
signature verification, Central Portal upload, manual publish, and a clean
consumer check. CI never receives signing keys or Central tokens. See
[ADR-0012](docs/adr/0012-release-and-publishing.md) and the section 15 runbook.

## Commit style

Use a short imperative subject, for example `Add section 14 API audit`. Keep
one logical change per commit. Do not commit generated output, native jars, or
`.gradle/`. Agents must leave work uncommitted; see [AGENTS.md](AGENTS.md).

## License

Contributions are accepted under MIT. The bundled font is CC0-1.0 with
attribution. Do not copy GPL-licensed source into this repository.
