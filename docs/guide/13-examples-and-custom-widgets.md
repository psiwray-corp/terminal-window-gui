# 13 — Examples and custom widgets

**Phase:** delivery · **Depends on:** 12 · **Blocks:** 14

## Goal

Runnable proof that the library is a passive surface and that a caller can
integrate it with `terminal-window` on the caller's terms. Gallery, TODO, and
dashboard examples, a custom widget, screenshots, and a headless smoke path.

## Context budget

Read: `GUIDE.md`, this file, sections 06 and 12 public types, and
`terminal-window` `ReadmeQuickstart`, `examples/`, and
`terminal-window-cmdline` `AppLoop.kt` for the integration shape (not to copy
ownership into `:library`). Do not read sibling sections. Expected output:
example applications, a custom widget, screenshots, and smoke tests.

## Deliverables

- `examples/src/main/kotlin/.../ExampleLoop.kt` (example-only host loop).
- `examples/src/main/kotlin/.../gallery/GalleryMain.kt`.
- `examples/src/main/kotlin/.../todo/TodoMain.kt`.
- `examples/src/main/kotlin/.../dashboard/DashboardMain.kt`.
- `examples/src/main/kotlin/.../custom/Sparkline.kt` and `CustomDemo.kt`.
- Gradle run tasks: `:examples:gallery`, `:examples:todo`,
  `:examples:dashboard`, `:examples:custom`.
- `:examples:smoke` task or `--smoke` argument that renders a fixed number of
  frames and exits.
- `docs/assets/gallery.png`, `todo.png`, `dashboard.png`, `custom.png`,
  regenerated together with `./gradlew :examples:screenshots`.
- Tests: `SparklineTest` (golden), `ExamplesSmokeTest` (gl tag) or a
  `:examples:smoke` task run under Xvfb.

## Steps

1. **Example loop (examples only).** `ExampleLoop` owns the service, window,
   owner thread, input listener, and frame cadence. It mirrors
   `terminal-window-cmdline`:
   - `TerminalWindowService.initialize()` on the process main thread;
   - open a window with `GuiFonts.bundled()`, requested 80×24/120×40, theme
     colors;
   - release the context and run the owner loop on a dedicated thread;
   - add an `InputListener` that calls `GuiInput` + `surface.dispatch` and
     records the last pointer cell for scroll;
   - each loop iteration: `window.frame()`, compare matrix size and
     `surface.resize`, `surface.tick(System.nanoTime())`, and if
     `surface.needsPaint` paint `CellCanvas(window.matrix)`;
   - apply `surface.pointerShape` to `window.setCursor` only when it changes;
   - apply `ScreenHost.currentTitle` to `window.setTitle` only when it changes;
   - sleep ~15 ms, handle close, drain the last paint, release the context, and
     close the service.
   This class lives in `:examples`; `:library` never contains an app loop.
2. **Gallery.** Show every v1 widget in a scrollable page: panels, labels,
   separators, badges, icons, buttons, checkboxes, radios, switch, toolbar,
   text fields, search, text area, log view, select, list, table, tree, tabs,
   split pane, form, status bar, menu bar, dialog launchers, tooltip target,
   toast launcher, progress, spinner, banner. Controls to switch theme
   (dark/light/high-contrast) and shape set (Unicode/ASCII) at runtime. Show
   the current grid size and a keyboard-help overlay. This is the visual
   catalog and the manual proof of the shape fallback.
3. **TODO.** A small application: list of tasks, add/edit form, delete
   confirmation dialog, menu bar, status bar, keyboard shortcuts. Demonstrates
   selection, text editing, dialogs, menus, commands, focus restore, and
   responsive layout at 80×24.
4. **Dashboard.** Tabs, table, progress bars, log view, banner, status bar.
   At least two responsive breakpoints: compact (< 100 columns) and wide.
   Demonstrates virtualization, follow-tail, and theme switching.
5. **Custom widget.** `Sparkline` paints a one-row chart using
   `PROGRESS_*` shapes and `PaintContext`; no hardcoded glyph and no widget
   subclass beyond `measure`/`paint`. `CustomDemo` composes it with labels and
   a legend. This proves extension without exposing internals.
6. **Smoke mode.** Every example accepts `--smoke`: open the window, paint and
   frame 10 times, then close with exit code 0. No input injection, no
   screenshot diff. Use a watchdog (for example 20 s) so CI cannot hang.
7. **Screenshots.** Capture the gallery, TODO, and dashboard at a fixed grid
   size and commit them under `docs/assets/`. Document the font, theme, and
   terminal size used. Screenshots are visual proof, not test assertions.
8. **Gradle.** Wire host LWJGL natives as `runtimeOnly`, set
   `applicationDefaultJvmArgs`/JavaExec args to the shared `runtimeJvmArgs`,
   and add one task per example. The `:examples:build` task must not require a
   display. The smoke task runs under Xvfb in CI.
9. **Tests.** `SparklineTest` uses `GuiTestHarness` and a golden grid.
   `ExamplesSmokeTest` (tag `gl`) or the smoke task opens a real window under
   Xvfb, runs 10 frames, asserts a draw call, and closes. Examples compile on
   Linux, macOS, and Windows; only the GL smoke runs on Linux.
10. **CI.** Extend `ci/integration-test.sh` to run the example smoke after the
    library integration suite. Keep the gate in the script, not YAML.
11. Update the `GUIDE.md` progress entry. Leave the tree uncommitted.

## Acceptance

- [x] `ExampleLoop` lives only in `:examples`; `:library` still has no window,
      loop, thread, timer, or GL code.
- [x] All four example tasks run from the repository root with the bundled
      font and host natives.
- [x] `--smoke` exits 0 after a fixed number of frames within the watchdog.
- [x] Gallery switches theme and Unicode/ASCII shape sets without restarting.
- [x] TODO demonstrates list, editing, dialog, menu, command, status, and
      focus restore.
- [x] Dashboard demonstrates tabs, table, progress, log follow-tail, banner,
      and at least two responsive layouts.
- [x] `Sparkline` uses only `PaintContext`/`Shape` and has a golden test.
- [x] Screenshots exist and show the documented theme/font/size.
- [x] The Xvfb smoke observes a draw call and exits cleanly.
- [x] No benchmark module or timing assertion is added.
- [x] Examples build on Windows/macOS even if the GL smoke is Linux-only.

## Pitfalls

- Do not move `ExampleLoop` into `:library`; ownership is the point.
- Paint after `window.frame()` so resize is applied; accept one tick of paint
  latency.
- Do not busy-spin; sleep ~15 ms when idle.
- Do not call `requestRepaint` from the library; painting changes cells.
- Do not leave the owner thread or service running on exit.
- Use `GuiFonts.bundled()`; the stock 95-glyph ASCII font lacks the standard
  shapes.
- Keep screenshots at a stable grid size and theme; do not commit terminal
  chrome.
- Smoke mode must not depend on keyboard/mouse input.
- Windows/macOS builds must not require Xvfb; only the GL smoke does.

## Plan seed

> Plan section 13 only. Build `:examples` with a caller-owned `ExampleLoop`,
> Gallery, TODO, Dashboard, and a custom `Sparkline` using `PaintContext`.
> Add Gradle run/smoke tasks, committed screenshots, a golden custom-widget
> test, and an Xvfb smoke that paints and frames. The library remains passive.
