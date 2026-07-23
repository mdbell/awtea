# Instrument Testing Plan

## Overview

`awtea-instrument` grew from two detour kinds to a full transformation family
(July 2026): replacements, `@Before`/`@After`/`@Finally` advice, `@Guard`,
`@Filter`, `@FieldGet`/`@FieldSet`, `@ElementGet`/`@ElementSet`, `@Callers`,
`@DetourApplied`, `@DisableDetour`, `@BuildConstant`, and the zero-match
verifier. Every feature was verified manually: temporary fixture detours in the
scaperune webclient, a dev TeaVM build, grep over the emitted JS, then the
scaffolding deleted. That workflow proved the features but left no regression
net — everything is pinned only to TeaVM 0.13's output shapes as of the day it
was written.

This document is the test inventory for when the harness lands. **Do not build
another bespoke harness**: the existing deno-test-runner rig is semi-broken,
and the plan of record is to wait for the migration to TeaVM's own testing
infrastructure (teavm-junit), then port this inventory onto it.

## Two tiers of tests

### Tier 1: plain JVM unit tests (no TeaVM pipeline — can be added any time)

Registration-time validation in `DetourHacks` runs in the build JVM and needs
no compilation pipeline. `new DetourHacks(List.of(Fixture.class))` either
throws or produces a registry — assert on that directly:

| Fixture shape | Expected |
|---|---|
| Two detour annotations on one method (e.g. `@Before` + `@Filter`) | `IllegalArgumentException` "more than one detour annotation" |
| Registered class without `@DetourReceiver` | `IllegalArgumentException` suggesting `@DisableDetour` |
| Class-level / method-level `@DisableDetour` | registers nothing, no exception, info log only |
| Non-static advice/guard/filter/hook method | `IllegalArgumentException` |
| Advice with non-void return; guard without trailing `Interception`; guard/advice on `"<init>"` | `IllegalArgumentException` each |
| Filter whose trailing param ≠ return type, or void return | `IllegalArgumentException` |
| Field hook trailing param ≠ return type; wrong arity | `IllegalArgumentException` |
| Element hook without `int` index param before the value | `IllegalArgumentException` |
| `@Callers` with empty class list; `@Callers` with no detour annotation | `IllegalArgumentException` each |
| `@DetourApplied` on non-static / non-boolean method | build-time `IllegalStateException` (this one fires in `transformClass`, so it needs Tier 2 or a hand-built `ClassHolder`) |

### Tier 2: pipeline tests (need teavm-junit / a TeaVM compile)

Structure that worked well manually and should be kept: **one shared fixture
app exercising all features, compiled once per test class**, with many small
assertions against the emitted JS text and the reported problems. Failure-mode
tests (expected build errors) each need their own small compile.

Fixtures need: a target class with instance + static methods, a static and an
instance array field, a scalar instance field; a caller class; a detour class
per feature under test; a `main` that reaches everything (reachability is what
makes transforms and the zero-match verifier fire).

## Assertion recipes (observed TeaVM 0.13 JS shapes)

These are the exact shapes manual verification greped for. Assertions should
be tolerant of variable renumbering but strict about call presence, order, and
argument threading.

**@Before/@After** — advice brackets the original with identical args:

```js
nswd_RendererDetour_beforeRenderGame(var$17, $x, var$18, $y, var$19);
jc_Renderer_renderGame(var$17, $x, var$18, $y, var$19);
nswd_RendererDetour_afterRenderGame(var$17, $x, var$18, $y, var$19);
```

**@Guard** — carrier constructed, guard called, branch on `isCancelled`, the
original only on the not-cancelled path; both paths converge:

```js
var$29 = mmai_Interception__init_();
nswd_SceneDetour_render(var$27, ..., var$29);
if (!mmai_Interception_isCancelled(var$29)) { /* -> original call */ }
```

Also assert: a guard cancelling a *value-returning* original substitutes via
the typed accessor (`getInt`/`getObject`+cast) — this path had no in-tree
consumer and is the least-verified part of the guard feature.

**@Filter** — pipes the (possibly replaced) call's result; composes with
replacement:

```js
$tmp = nswd_ItemSpriteDetour_method421(...);   // the REPLACEMENT call
var$13 = $tmp;
$sprite = nswd_ItemSpriteDetour_tempVerifyFilter(..., var$13);
```

**@Finally** — hook on both exits, rethrow preserved, filters inside the
protected region:

```js
try {
    var$13 = j_..._method421(...);
} catch ($$e) {
    nswd_ItemSpriteDetour_afterBake(...);
    $rt_throw(var$13);
}
// filter + normal-path hook in a second protected region:
$sprite = ..._filterBake(..., var$13);
nswd_ItemSpriteDetour_afterBake(...);
```

**@FieldGet/@FieldSet** — reference-level plumbing:

```js
$widget.$anInt2193 = nswd_..._onSlotWrite($widget, $i_0_);           // instance set
(nswd_..._onDirtyFlagsRead(j_..._aBooleanArray3083)).data[$i] = 1;   // element store fires the GET hook
```

**@ElementGet/@ElementSet** — index + value threading, instance as self:

```js
j_..._aBooleanArray3083.data[$i] = nswd_..._onDirtyFlagWrite($i, 1);
$RSInterface.$anIntArray2179.data[$i] = nswd_..._onItemIdWrite($RSInterface, $i, ...);
if (!nswd_..._onDirtyFlagRead($i, j_..._aBooleanArray3083.data[$i])) { ... }
```

Also assert the *scope contract*: an element access whose array arrives via a
method parameter must NOT be hooked (local traceability only), and an access
via a local alias (`var$3[var$4] = hook(...)`) MUST be.

**@DetourApplied** — probe body compiled to `return 1;` when the class is
registered; stays `return 0;` when the class is `@DisableDetour`d or absent.

**@BuildConstant** — placeholder replaced by the supplied value in the string
pool; unsupplied-key probe fails the build; supplied-but-unconsumed key
produces a warning from `unusedValueVerifier()`.

**Zero-match verifier** — strict mode: a detour naming a nonexistent method
fails the build with a message containing both sides of the binding; lenient
mode reports the same text as a warning. `@Callers` pointing at a class with
no matching site must drive the strict verifier to failure (this is the
caller-filter negative test).

**Conflict rules (build errors at transform time)** — same call site:
guard+guard, guard+finally, finally+finally, guard+replacement, guard+filter.
Field-hook and element-hook **type drift** (hook's declared type vs the
field's actual type) must fail the build, not skip.

## Behavioral contracts the tests must pin (learned the hard way)

- `BasicBlockSplitter` allows splitting a block only once, but a split-off
  tail may be split again — guard/finally rely on the two-split idiom, and a
  second `@Finally` on one site would hit the splitter's
  `IllegalArgumentException` if the conflict check ever regressed.
- `fixProgram()` ignores blocks created after the splitter initialized —
  hand-built phis (guard) and handler blocks (finally) depend on this.
- Advice/guard/filter/replacement on one site compose in a fixed order:
  control-flow wrap first, then advice, filters, replacement. `@Before`/
  `@After`/`@Filter` land *inside* a `@Finally`'s protected region.
- Array-typed fields: element stores compile to a field *read* + element
  store, so `arr[i] = x` fires `@FieldGet`, never `@FieldSet`.
- Guards allocate one `Interception` per call (green-thread suspension makes
  a shared carrier unsafe) — keep guards off per-tile hot paths; a test can't
  assert the perf policy, but it can assert the allocation is per-call.
- The element-hook origin map must be built on the *untransformed* program or
  `@FieldGet` on the same field breaks the trace chain.

## Current status

- No automated tests exist for awtea-instrument; the deno-test-runner rig in
  awtea-graphics is unrelated and semi-broken anyway.
- Tier 1 tests have no infrastructure dependency and can be added whenever.
- Tier 2 waits on the teavm-junit migration; `teavm-tooling` is already a
  test dependency of awtea-graphics if an interim TeaVMTool-based compile is
  ever wanted, but see the plan of record above.
