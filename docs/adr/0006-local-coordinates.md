# ADR-0006: Local widget coordinates and `PaintContext` transforms

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

Widgets form a tree. A parent clips its children and positions them inside its
content box. If every widget stored absolute screen bounds, moving a container
would require walking and rewriting every descendant, hit testing would need
parent arithmetic anyway, and nested clipping would be error-prone.

The host matrix is addressed in absolute screen cells. Overlays and pointer
events also arrive in screen cells or framebuffer coordinates. The GUI needs a
single translation boundary between the local tree and the screen.

## Decision

We will store widget bounds in **local coordinates relative to the parent's
content box**.

- The root widget's local origin is `(0, 0)`, and its bounds are
  `(0, 0, columns, rows)`.
- A child's `arrange` receives a rectangle in the parent's coordinate space;
  the child stores it and lays out its own children relative to its content
  box.
- `measure` and `arrange` use integer cell units. There is no sub-cell layout.
- `paint` uses widget-local coordinates. A `PaintContext` carries an origin
  stack and a clip stack.
- `PaintContext.pushOrigin(dx, dy)` adds a local offset. `pushClip(rect)`
  intersects a local clip with the current clip after translating it by the
  current origin. Both stacks are fixed-depth and validated.
- A shape or text drawing call at local `(x, y)` writes at
  `origin + (x, y)` in the `CellCanvas`. The canvas writes absolute screen
  cells and enforces the half-open clip and matrix bounds.
- The only allowed transform is integer translation. There is no scaling,
  rotation, shear, or fractional placement.
- Overlays are stored in screen coordinates because they can escape their
  anchor and are painted after the root. Pointer coordinates are converted to
  a widget-local coordinate only at the hit-test/focus boundary.
- Hit testing walks the tree, subtracts the accumulated local origin, and
  rejects points outside the current clip.
- Clips are half-open: a rectangle `(x, y, width, height)` covers columns
  `x .. x + width - 1` and rows `y .. y + height - 1`.

The `CellCanvas` is a low-level absolute-cell writer. Widget code never writes
absolute cells directly; it uses `PaintContext`.

## Consequences

### Positive

- Moving a container does not mutate descendant bounds.
- Nested clipping and scrolling compose with one origin/clip stack.
- Golden tests can assert local child bounds and translated screen cells
  independently.
- The model matches terminal cell addressing without introducing a general
  2-D transform system.

### Negative

- Every paint and hit test pays for an origin/clip stack walk.
- Widgets cannot use absolute screen positions; custom widgets must cooperate
  with the local model.
- Overlays need a separate screen-coordinate path.
- Fractional coordinates and sub-cell pointer precision are unavailable in
  v1.

### Neutral

- `CellCanvas` remains public for callers and tests; its direct absolute API is
  intentional.
- A future transform system can be added only as a new ADR, not by changing
  the meaning of existing local bounds.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Absolute bounds on every widget | Parent moves require descendant rewrites and make clipping fragile. |
| Fractional layout | The terminal matrix is integer cells; fractional placement would need rounding rules everywhere. |
| Full 2-D affine transform | No scale/rotation use case; adds complexity and non-deterministic rounding. |
| Paint directly in screen coordinates | Widgets would need to know their absolute position and parent clip manually. |
| Store both local and absolute bounds | Two sources of truth that can drift after layout. |

## References

- `GUIDE.md` coordinate and shape rules.
- `docs/guide/04-layout-and-widget-tree.md` local bounds.
- `docs/guide/03-fonts-shapes-canvas-text.md` `CellCanvas` and `PaintContext`.
- `terminal-window` `TerminalMatrix` absolute cell addressing.
