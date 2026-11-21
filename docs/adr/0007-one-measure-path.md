# ADR-0007: One measure path; no mutable Swing-style sizes

- **Status:** Accepted
- **Date:** 2026-11-21
- **Deciders:** project owner
- **Supersedes:** none

## Context

Terminal layout is integer-cell layout with a small set of constraints. Swing's
mutable `preferredSize`, `minimumSize`, and `maximumSize` fields create
ordering bugs: a setter can invalidate layout silently, a child can report a
size its parent did not ask for, and a cached size can outlive the state that
produced it.

The GUI needs one deterministic contract that custom widgets and built-in
widgets share, and it must support flex rows/columns, clipping, scrolling, and
dialogs with bounded widths.

## Decision

We will use one measure path:

```kotlin
public fun measure(constraints: Constraints): IntSize
```

`Constraints` carries inclusive min/max cell dimensions. The parent passes
constraints to each child; the child returns its measured size; the parent
arranges the child in a local rectangle. There is no second intrinsic-size
path and no public size setter.

- Preferred size is computed from content and style during measurement; it is
  never a stored mutable field.
- Minimum size is a content clamp or an explicit `ConstrainedBox` wrapper.
- Maximum size comes from parent constraints or an explicit
  `ConstrainedBox`/`SizedBox` wrapper.
- `Length` expresses `Fixed`, `Fill`, `Fraction`, and `Wrap` in the main axis.
- `Flexible` and `Expanded` distribute leftover main-axis space.
- `Row`, `Column`, `Grid`, `Wrap`, `Stack`, `Padding`, `Align`, `Center`,
  `SizedBox`, `ConstrainedBox`, `Spacer`, and `ScrollView` use this path.
- Measurement is cached only for the current layout pass. State changes
  invalidate the cache; a widget does not expose a stale size hint.
- Layout is idempotent: measuring and arranging the same tree with the same
  constraints and state produces the same bounds.
- Widgets may clamp their result to the received constraints; they may not
  return a size outside them.
- `ScrollView` treats content as unbounded in the scroll axis and clips the
  viewport; the viewport itself is measured by its parent.

No public `preferredSize`, `minimumSize`, `maximumSize`, `setSize`, or
`resizeTo` field exists. `SizedBox` and `ConstrainedBox` are the explicit
wrappers for caller-imposed dimensions.

## Consequences

### Positive

- One custom-widget contract, one cache rule, and one set of invariants.
- Layout tests can assert bounds without reconciling several size APIs.
- Content changes cannot silently leave a stale preferred size.
- Flex and scroll behavior are expressed through constraints, not setters.

### Negative

- Widgets must recompute preferred content size when state changes.
- A caller cannot set a widget size in place; it must wrap it.
- Measurement can be more expensive than reading a cached mutable field.
- Some Swing habits do not translate directly.

### Neutral

- `SizeHints`-like storage is internal to a layout pass and never public.
- Scroll models clamp to measured content; they do not own preferred sizes.
- A future intrinsic-sizing fast path would require an ADR and must preserve
  the single public contract.

## Alternatives

| Alternative | Why rejected |
|---|---|
| Mutable `preferredSize` / `minimumSize` / `maximumSize` | Silent invalidation, stale caches, and ordering bugs. |
| Separate `preferredSize()` and `measure()` methods | Two contracts that can disagree. |
| Parent assigns child size | Children cannot report content needs for scrolling and wrapping. |
| Absolute pixel layout | The matrix is integer cells; no responsive flex. |
| Swing layout managers | Kotlin value model, terminal cells, and local coordinates differ. |

## References

- `GUIDE.md` sizing model and standard testing matrix.
- `docs/guide/04-layout-and-widget-tree.md` `Constraints` and `IntSize`.
- Flutter `BoxConstraints` as the primary inspiration for one constraint-based
  measure path: <https://api.flutter.dev/flutter/rendering/BoxConstraints-class.html>.
