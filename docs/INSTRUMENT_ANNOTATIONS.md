# Instrument Annotations Reference

## Overview

`awtea-instrument` rewrites TeaVM IR at build time, driven by annotations on
detour classes. This is the family reference: what each annotation does, its
signature conventions, how they compose, and the sharp edges. Everything here
runs at build time — none of it exists at runtime except the code you wrote.

A detour class is registered with `DetourHacks` (directly, or via the
`META-INF/awtea.detours` resource list) and carries:

```java
@DetourReceiver(target = TargetClass.class)  // who this class detours
@NoDetours                                    // don't rewrite call sites inside this class
public class SomethingDetour { ... }
```

`@NoDetours` on the class is what lets detour bodies call their originals
without recursing. On a single method it opts that method out of registration.

## Call detours

All method-binding annotations name the original by string (`value`, empty =
same name as the hook method). Instance originals take a leading
`Target self` parameter; static originals don't. Zero-match verification
(below) guards every string binding.

### `@DetourMethod` — replacement

```java
@DetourMethod("foo")           // target.foo(a, b) : R
public static R foo(Target self, A a, B b) { ... }   // must call through if the original should run

@DetourMethod(constructor = true)   // new Target(a) -> factory
public static Target create(A a) { ... }
```

The call site is redirected to the hook; the hook owns everything, including
calling the original. Still the right tool when you need try/finally *state*
around arguments the advice forms can't express — but check `@Finally` first.

### `@Before` / `@After` — advice

```java
@Before("foo") public static void beforeFoo(Target self, A a, B b) { ... }
@After("foo")  public static void afterFoo(Target self, A a, B b) { ... }
```

Inserted adjacent to the call; the original runs untouched. `@After` runs
only on normal completion. Matched by name + parameter types (the original's
return type doesn't participate).

### `@Finally` — both-exits advice

```java
@Finally("foo") public static void cleanupFoo(Target self, A a, B b) { ... }
```

The call site is wrapped in a synthesized catch-all: the hook runs after
normal completion *and* when the original throws, then the exception is
rethrown (still catchable by the method's own enclosing handlers). Java
`finally` semantics, including the sharp edge: a hook that throws on the
exception path supersedes the original exception.

### `@Guard` — conditional bypass

```java
@Guard("foo")
public static void guardFoo(Target self, A a, B b, Interception ci) {
    if (shouldSkip()) ci.cancel(replacementValue);   // or ci.cancel() for void
}
```

Runs before the call and decides whether the original runs; `cancel(value)`
substitutes the result (boxed for primitives, unboxed at the call site).
**Allocates one `Interception` per call** — deliberate, because guards can
suspend on TeaVM green threads and a shared carrier could be corrupted — so
keep guards off per-tile/per-frame-inner-loop hot paths; use `@DetourMethod`
there. Cannot express try/finally around the original.

### `@Filter` — result mutation

```java
@Filter("foo")                 // target.foo(a) : R
public static R filterFoo(Target self, A a, R result) { return result; }
```

Receives the original's return value as the trailing parameter; whatever it
returns becomes the call's result. Exact-descriptor matched (the result
parameter doubles as the declared return type). Void originals can't be
filtered — use `@After`. Filters chain; they compose with replacements
(piping whatever the rewritten call produces).

### `@Body` — method-body replacement

```java
@Body("foo")                   // replaces Target.foo's BODY, not its call sites
public static R foo(Target self, A a, B b) {
    ...
    return self.foo(a, b);     // inside the hook's declaring class this is
}                              // rewritten to the preserved original body
```

Every invocation path is hooked — virtual dispatch through supertypes,
interface calls, reflection, calls inside `@NoDetours` classes — because the
target method's body itself becomes a delegation to the hook; the original
body survives as a public synthetic sibling (`foo$original`). Call-through
rewriting applies **only inside the hook's declaring class**. Sharp edges:
subclass overrides are separate bodies and are not hooked; constructors and
native/abstract methods are unsupported; `@Callers` cannot combine with it
(there is no call site). Call-site detours on the same method still bind
their sites and run in front of the body hook.

## Field and element hooks

Bound to a named field on the `@DetourReceiver` target. Name + form matched;
a **type disagreement is a build error** (drift), never a silent skip.

```java
@FieldGet("someField")  public static T onRead(Target self, T value) { return value; }
@FieldSet("someField")  public static T onWrite(Target self, T value) { return value; }
```

Reads pipe what the reader sees; writes pipe what actually gets written.
Array-typed fields hook the *reference*: `arr[i] = x` compiles to a field
read + element store, so it fires `@FieldGet`, not `@FieldSet`.

```java
@ElementGet("someArrayField") public static T onElemRead(Target self, int index, T value) { return value; }
@ElementSet("someArrayField") public static T onElemWrite(Target self, int index, T value) { return value; }
```

Element hooks bind an access only when its array reference is **locally
traceable** within the enclosing method to a read of the named field
(directly or through simple assignments). References arriving via parameters,
phi merges, or other fields are silently untouched — pair with the zero-match
verifier, and prefer seam hooks where completeness is load-bearing.

## Modifiers

### `@Callers` — call-site restriction

```java
@Before("renderGame")
@Callers(ToSort.class)
public static void beforeRenderGame(...) { ... }
```

Stacks with any detour annotation; restricts binding to sites inside the
listed classes. Class literals, so renames refactor with the code.
Class-level granularity only, on purpose.

### `@DisableDetour` — explicit off-switch

On a class: the whole detour class is skipped (quietly, and its
`@DetourApplied` probes stay false). On a method: just that mapping. Always
prefer this over commenting out `@DetourReceiver` — a registered class
*missing* its receiver is a build error precisely so accidental omission
can't silently disable anything.

## Build-time constants

All three share the pattern: a static probe method whose body is exactly the
placeholder, rewritten in place at build time. An untransformed build (plain
JVM, plugin absent) keeps the source default.

```java
@DetourApplied                       // true iff this detour class was registered this build
public static boolean isApplied() { return false; }

@BuildConstant("compile.time")       // value supplied by the build plugin
public static String compileTime() { return "unknown"; }

@BuildFlag("dev")                    // boolean flag supplied by the build plugin
public static boolean dev() { return false; }
```

`@BuildConstant`/`@BuildFlag` values come from a `BuildConstants` transformer:

```java
BuildConstants constants = new BuildConstants()
        .set("compile.time", timestamp)
        .setFlag("dev", isDevBuild);      // supply flags in EVERY build, never only when true
host.add(constants);
host.add(constants.unusedValueVerifier()); // warns on supplied-but-unconsumed keys
```

Rules learned the hard way:

- **One `BuildConstants` instance per build.** Every registered instance
  scans every probe, and a reachable probe whose key the instance doesn't
  carry is a build error — two instances therefore fail on each other's keys.
- A reachable probe with no supplied value is a build error (probe/plugin
  drift); flag keys share a namespace with constant keys.
- `@BuildFlag`'s payoff is dead-code stripping: `if (Flags.dev()) {...}`
  constant-folds and the branch (including its string constants) vanishes
  from builds where the flag is false — **but only at
  `OptimizationLevel.BALANCED` or above**. At NONE the branch remains,
  inert but shipped.

The `EmbedTransformer` in awtea-graphics (`@ShaderSource`/`@CSSSource`) is
the same mechanism with the value loaded from a classpath resource.

## Zero-match verification

```java
DetourHacks detours = new DetourHacks(classes);
host.add(detours);
host.add(detours.zeroMatchVerifier(true));   // true = errors, false = warnings
```

After dependency analysis completes, every registered detour that bound zero
sites is reported — a renamed original or drifted signature is how a detour
silently un-hooks. Use strict mode for an application's own detours; lenient
for opportunistic library sets whose target APIs an app may never use.
Intentionally dead detours get `@DisableDetour`, not a tolerated zero-match.

## Composition rules (per call site)

| Combination | Result |
|---|---|
| `@Before`/`@After` + anything | compose; land *inside* a guard's conditional or a `@Finally`'s try |
| `@Filter` + `@DetourMethod` | compose (filter pipes the replacement's result) |
| `@Filter` + `@Filter` | chain (order = registration order) |
| `@Guard` + `@Guard`/`@Finally`/`@DetourMethod`/`@Filter` | build error |
| `@Finally` + `@Finally` | build error |
| `@Body` + `@Callers` | build error (no call site to filter) |
| `@Body` + call-site detours on the same method | compose: sites bind first, then the body hook |
| Two detour annotations on one method | build error |

Transform order per site: control-flow wrap (guard/finally) first, then
advice, then filters, then replacement.

## Invariants for transformer authors

- Static detour calls must be `InvocationType.SPECIAL`. Converting an
  instance call to a static one without switching the type leaves a VIRTUAL
  invoke with a null instance — invalid IR that only surfaces as an NPE in
  TeaVM's optimizer at BALANCED+ ("InvokeInstruction.getInstance() is null").
  This bug is why this codebase historically pinned optimization to NONE.
- Prefer editing javac-produced constants in place over synthesizing
  programs; hand-built IR must honor renderer invariants the model API
  doesn't enforce (e.g. variable 0 is reserved).
- `BasicBlockSplitter`: a block splits once, but a split-off tail can split
  again; `fixProgram()` ignores blocks you created yourself, which is what
  makes hand-built phis and handler blocks safe.

See `INSTRUMENT_TESTING.md` for the test inventory that should eventually pin
all of this.
