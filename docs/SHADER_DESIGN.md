# Awtea Shaders Bytecode VM

## Overview

This VM executes shaders on a stack machine using **fixed-point integer
arithmetic** for all values.\
Constants, uniforms, and pixel inputs are drawn from separate tables. The VM is
extensible for SIMD in the future.

**All numeric stack values are 32-bit signed integers in 16.16 fixed-point
format**\
(e.g., 0x00010000 = 1.0, 0x00008000 = 0.5, 0xFFFFFFFF = -0.0000153...)

## Constants

```c
#define MAX_CONSTS 128
#define MAX_UNIFORMS 128
#define MAX_INPUTS 2

#define INPUT_U 0
#define INPUT_V 1
// ... May be expanded later

#define MAX_STACK_SIZE 16

#define NATIVE_FUNC_COUNT 0 // Currently none, just reserved
```

## Opcodes

| Name                 | Code | Stack Args          | Stack Output          | Description                                                            |
| -------------------- | ---- | ------------------- | --------------------- | ---------------------------------------------------------------------- |
| **Stack Load**       |      |                     |                       |                                                                        |
| LOAD_CONST _n_       | 0x01 |                     | [const[n]]            | Pushes constant value (from shader's constant table)                   |
| LOAD_UNIFORM _n_     | 0x02 |                     | [uniform[n]]          | Pushes current uniform                                                 |
| LOAD_INPUT _n_       | 0x03 |                     | [input[n]]            | Per-pixel interpolators: u, v, screen_x, ...                           |
| POP                  | 0x04 | [a]                 | -                     | Discard top stack value                                                |
| DUP                  | 0x05 | [a]                 | [a, a]                | Duplicate top value                                                    |
| SWAP                 | 0x06 | [a, b]              | [b, a]                | Swap top two                                                           |
| **Arithmetic**       |      |                     |                       | _All ops are fixed-point_                                              |
| ADD                  | 0x10 | [a, b]              | [a+b]                 | Fixed-point addition                                                   |
| SUB                  | 0x11 | [a, b]              | [a-b]                 | Fixed-point subtraction                                                |
| MUL                  | 0x12 | [a, b]              | [a*b>>16]             | Fixed-point multiply (16.16)                                           |
| DIV                  | 0x13 | [a, b]              | [a<<16/b]             | Fixed-point division (16.16)                                           |
| MOD                  | 0x14 | [a, b]              | [a % b]               | Integer modulo                                                         |
| NEG                  | 0x15 | [a]                 | [-a]                  | Fixed-point negation                                                   |
| MIN                  | 0x16 | [a, b]              | [min(a,b)]            | Minimum value                                                          |
| MAX                  | 0x17 | [a, b]              | [max(a,b)]            | Maximum value                                                          |
| ABS                  | 0x18 | [a]                 | [abs(a)]              | `a = a < 0 ? -a : a`                                                   |
| CLAMP                | 0x19 | [a, b, c]           | [clamp(a,b,c)]        | clamps the value a between b (min) and c (max)                         |
| SIGN                 | 0x1A | [a]                 | [signum(a)]           | `a == 0 ? 0 : a < 0 ? -1 : 1`                                          |
| VEC_OP _n_ _op_      | 0x1B | [... x0 x1 x2 x3 s] | [... x0' x1' x2' x3'] | Vectorized op across n values                                          |
| **Comparison/Logic** |      |                     |                       | _Results are int32—0 for false, 1 for true_                            |
| EQ                   | 0x20 | [a, b]              | [a == b]              | Integer/fixed-point equality                                           |
| NEQ                  | 0x21 | [a, b]              | [a != b]              | Inequality                                                             |
| LT                   | 0x22 | [a, b]              | [a < b]               | Less-than                                                              |
| LE                   | 0x23 | [a, b]              | [a <= b]              | Less-or-equal                                                          |
| GT                   | 0x24 | [a, b]              | [a > b]               | Greater-than                                                           |
| GE                   | 0x25 | [a, b]              | [a >= b]              | Greater-or-equal                                                       |
| AND                  | 0x26 | [a, b]              | [a & b]               | Bitwise and                                                            |
| OR                   | 0x27 | [a, b]              | [a \| b]              | Bitwise or                                                             |
| NOT                  | 0x28 | [a]                 | [~a]                  | Bitwise not                                                            |
| **Branch/Flow**      |      |                     |                       | _Offsets are signed int16_                                             |
| JUMP offset          | 0x30 |                     |                       | Unconditional jump (relative)                                          |
| JZ offset            | 0x31 | [a]                 |                       | Jump relative if a==0 ("false")                                        |
| JNZ offset           | 0x32 | [a]                 |                       | Jump relative if a!=0 ("true")                                         |
| CALL addr            | 0x33 |                     |                       | Call subroutine (reserved - not impl)                                  |
| RET                  | 0x34 |                     |                       | Return from subroutine (reserved - not impl)                           |
| CALL_NATIVE _n_      | 0x35 | [a, b, c, ..]       | [result]              | Calls native function _n_                                              |
| **Conditional**      |      |                     |                       | _TBD: Maybe expand this section, e.g `ISZERO`, `ISNEG` etc_            |
| SELECT               | 0x40 | [cond, a, b]        | [cond?a:b]            | If cond (nonzero), push a else b                                       |
| **Texture/Draw**     |      |                     |                       | _Note that all surface operations do _not_ have an associated context_ |
| SAMPLE_SURFACE n     | 0x50 | [u, v]              | [r,g,b,a]             | Sample surface n at (u,v), push 4 fixed-point RGBA [0-255<<16]         |
| SET_COLOR            | 0x51 | [r,g,b,a]           |                       | Set output color (RGBA, int/fixed, top 4 stack vals)                   |
| **Program**          |      |                     |                       |                                                                        |
| END                  | 0xFF |                     |                       | End of program                                                         |

### Vector Operations

These are (sub) opcodes for the `VEC_OP` - they mirror their non-vector
counterparts, but operate on `n` elements on top of the stack instead of single
ops.

| Code | Operation | Arguments  |
| ---- | --------- | ---------- |
| 0x01 | ADD       | [delta]    |
| 0x02 | SUB       | [delta]    |
| 0x03 | MUL       | [scalar]   |
| 0x04 | DIV       | [scalar]   |
| 0x05 | MOD       | [modulus]  |
| 0x06 | NEG       | -          |
| 0x07 | MIN       | -          |
| 0x08 | MAX       | -          |
| 0x09 | CLAMP     | [min, max] |
| 0x0A | SIGN      | -          |

#### Example

```
.const one 1
.const two 2
.const three 3
.const delta 10

LOAD_CONST one
LOAD_CONST two
LOAD_CONST three
; stack before: [1, 2, 3]    (bottom to top)
LOAD_CONST delta
; stack before: [1, 2, 3, 10]   (bottom to top)

VEC_OP 3 0x01   ; Apply ADD with delta=10 to 3 values

; stack after: [11, 12, 13]
```

---

## Data Model

This section describes the core runtime data structures and value types used by
the Awtea Shaders Bytecode VM. A clear data model ensures correct interpreter
behavior and makes it easier to extend the VM in future versions.

---

### Value Representation

All values in the VM (stack, constants, uniforms, inputs, outputs) are **signed
32-bit integers using a 16.16 fixed-point format**.

- **Examples:**
  - Integer 1.0 = `0x00010000`
  - Fractional 0.5 = `0x00008000`
  - Integer -2 = `0xFFFE0000`
- All math, logic, and sampler operations expect/furnish values in this format
  unless otherwise noted.

---

### Data Tables & Buffers

#### 1. Constants Table

- Fixed at shader compile time.
- Indexed by `LOAD_CONST`.
- Read-only.

```c
int32_t constants[MAX_CONSTS];
// Provided in the shader struct/header
```

#### 2. Uniforms Table

- Set per-draw/dispatch by the host.
- Indexed by `LOAD_UNIFORM`.
- Constant within a shading batch.

```c
int32_t uniforms[MAX_UNIFORMS];
// Provided by host/environment before draw/dispatch
```

#### 3. Inputs Table

- Per-pixel (or per SIMD batch) data set by the rasterizer or host.
- Indexed by `LOAD_INPUT`.
- The set and interpretation of values is host- and shader-dependent, typically:
  - INPUT_U: u coordinate (fixed-point, e.g., texture)
  - INPUT_V: v coordinate (fixed-point, e.g., texture)
  - Future: x/y position, barycentrics, depth, etc.

```c
int32_t inputs[MAX_INPUTS];
// Recomputed/calculated for each pixel (or batch)
```

#### 4. Operand Stack

- The primary execution stack.
- All operations push/pop from this stack.
- Stack overflow/underflow is a validation and runtime error.

```c
int32_t stack[MAX_STACK_SIZE];
uint32_t sp; // stack pointer (top = stack[sp-1])
```

#### 5. Native Function Table

- Holds pointers/callbacks to host/native functions for `CALL_NATIVE`
  instructions.
- Table indexed by function number.
- Will not be inital implementation

```c
// C example, up to NATIVE_FUNC_COUNT
int (*native_funcs[NATIVE_FUNC_COUNT])(int32_t* stack, int argc);
```

#### 6. Surfaces/Textures Descriptor Table

- Array of surface descriptors provided by the host.
- Indexed by `SAMPLE_SURFACE n`.
- Surface descriptors hold pixel buffer pointer, size, format, stride, etc.
- Not allocated/owned by the VM—host must ensure lifetime. (see `awt_surface.h`)

```c
Surface surfaces[MAX_SURFACES];
```

#### 7. Output Buffer

- The output color for each pixel after `SET_COLOR`.
- Usually RGBA (4 channels, 8 bits each, or host-specific format).
- Output written to the destination surface
  - if destination is RGB, A is ignored/undefined

---

### Interpreter State Structure (Sketch)

A typical execution context for the VM might look like:

```c
typedef struct {
    const int32_t* constants;
    uint32_t const_count;
    const int32_t* uniforms;
    uint32_t uniform_count;
    const int32_t* inputs;
    uint32_t input_count;
    int32_t* stack;
    uint32_t stack_size;
    Surface* destSurface;
    // native function table, etc.
    // Program counter, etc.
    uint32_t pc;
    ...
} ShaderContext;
```

---

### Value Lifetime

- **constants/uniforms:** Lifetime is at least that of the draw call or shader
  dispatch.
- **inputs:** Valid only for the duration of one pixel or batch.
- **stack:** Only used during interpretation of a single shader invocation.
- **output color:** Written once per pixel, after `SET_COLOR`.

---

### Example Data Flow for a Pixel

1. Host prepares `inputs[]` (e.g., sets u,v for the current pixel).
2. VM loads constants and uniforms, if not already set.
3. VM stack is initialized empty.
4. Interpreter runs the bytecode, resolving data references via these tables.
5. Upon `SET_COLOR`, VM writes output color, then returns.

This explicit data model makes the VM robust, easy to extend (e.g., for new
input channels, SIMD, or host-native data sharing), and safe for arbitrary user
shaders.

---

## Exception Model

The exception model defines how errors, invalid operations, and boundary
conditions are handled during shader bytecode execution and validation. Since
the Awtea Shaders Bytecode VM is designed for reliable and potentially
embedded/raster use cases, its exception model is pragmatic and aims at
deterministic, non-crashing behavior.

---

### Kinds of Exceptions

**1. Validation Errors**

- Occur during static analysis of bytecode before execution.
- Examples: unknown opcode, invalid branch target, stack underflow at any code
  point, input/constant/uniform out of range.

**2. Runtime Errors**

- Occur during interpretation/execution.
- Examples: division by zero, stack over/underflow, illegal surface/sample,
  accessing an unregistered native function.

**3. Host/Environment Errors**

- Occur outside of VM itself (e.g., missing output buffers, null surface
  pointers, memory errors).
- Usually must be handled by the host/rasterizer.

---

### Exception Handling Strategy

**1. During Validation:**

- On encountering a validation error, the shader must be rejected and not
  executed.
- The host should receive an error code or exception and optionally an error
  struct indicating the fault and bytecode offset.

**2. During Execution:**

- The VM attempts to remain robust; on runtime error, the VM:
  - Immediately aborts current pixel/batch execution.
  - Returns an error code to the host/rasterizer.
  - The surrounding frame/draw may be continued or aborted at the host’s
    discretion.

**3. Host Integration:**

- The host is responsible for propagating or logging error status, and may take
  corrective action (skip, retry, fallback shader, etc.).

---

### Error Codes / Reporting

| Error Type                   | Suggested Code | Description                                  |
| ---------------------------- | -------------- | -------------------------------------------- |
| OK                           | 0              | No error                                     |
| VALIDATION_UNKNOWN_OPCODE    | 1              | Unrecognized bytecode                        |
| VALIDATION_INVALID_OPERAND   | 2              | Bad const/uniform/input/surface index        |
| VALIDATION_STACK_ERROR       | 3              | Static stack underflow/overflow in code path |
| VALIDATION_BRANCH_ERROR      | 4              | Out-of-bounds branch/jump                    |
| VALIDATION_TERMINATION_ERROR | 5              | No reachable END instruction                 |
| EXEC_STACK_OVERFLOW          | 10             | Stack pointer out of bounds                  |
| EXEC_STACK_UNDERFLOW         | 11             | Stack pointer out of bounds                  |
| EXEC_DIV_BY_ZERO             | 12             | Division or mod by zero                      |
| EXEC_INVALID_SURFACE         | 13             | Surface/table index out of bounds            |
| EXEC_NATIVE_FN_ERROR         | 14             | Native fn not registered or errored          |
| EXEC_GENERAL_ERROR           | 99             | Unspecified or unknown execution error       |

---

### Exception Propagation

- **During validation**: function returns error code and, if possible, bytecode
  offset and context.
- **During execution**:
  - Function returns error code, may provide bytecode offset of fault, and
    optionally set output color to a diagnostic value.
  - Host code must check status after each dispatch and decide to continue,
    abort, or fallback.

---

### Suggested API Usage

```c
int awtea_shader_validate(const ShaderBytecode* bc, ShaderValidationResult* out_err);
int awtea_shader_execute(const ShaderContext* ctx, uint8_t* out_color, ShaderExecResult* out_err);
```

Where `out_err` supplies code, message, and (if detected) bytecode offset.

---

### Special Considerations

- Undefined or unsafe opcodes should never be executed due to validation.
- If error recovery is desired, host may skip over failures and substitute
  neutral output.
- For deterministic rendering (critical in reproducible graphics), exception
  behavior should be minimal, explicit, and visible in host logs or debug
  output.

---

## Inputs and Outputs

This section describes how data flows into and out of the Awtea Shader Bytecode
VM for each shader invocation—typically per pixel, per scanline, or per SIMD
batch.

---

### Inputs

Inputs provide per-pixel or per-invocation data. These are passed via the
`inputs[]` array, and retrieved in the shader with `LOAD_INPUT n`.

#### Standard Input Conventions

| Index | Macro      | Description                                | Source                        |
| ----- | ---------- | ------------------------------------------ | ----------------------------- |
| 0     | INPUT_U    | u coordinate (texture u, 16.16 fixed)      | Calculated by rasterizer/host |
| 1     | INPUT_V    | v coordinate (texture v, 16.16 fixed)      | Calculated by rasterizer/host |
| 2     | RESERVED_1 | reserved for future use (screen/logical X) | Calculated by rasterizer/host |
| 3     | RESERVED_2 | reserved for future use (screen/logical Y) | Calculated by rasterizer/host |
| 4+    | -          | Custom per-pixel varyings                  | As determined by host/shader  |

- By default, only INPUT_U and INPUT_V are required; others may be enabled as
  your pipeline grows.
- All input values are 32-bit signed integers in 16.16 fixed-point.
- Inputs are provided by the rasterizer or calling host code for every pixel (or
  batch, when using SIMD).
- Custom inputs can include barycentric coordinates, per-vertex attributes,
  masks, or any interpolated parameter.

#### Example: Rasterizer Fills Inputs Per Pixel

```c
int32_t inputs[MAX_INPUTS];
inputs[INPUT_U] = u_fp; // e.g. (x * 0x10000) / w
inputs[INPUT_V] = v_fp; // e.g. (y * 0x10000) / h
// Optionally: inputs[RESERVED_1] = ...; inputs[RESERVED_2] = ...;

// Then, for each pixel:
awtea_shader_execute(&shader, inputs, input_count, ...);
```

---

### Outputs

The **primary output** of a shader invocation is the RGBA color, written via the
`SET_COLOR` opcode.

#### SET_COLOR Behavior

- `SET_COLOR` expects the top 4 stack values to be (in order):\
  `[R (fixed), G (fixed), B (fixed), A (fixed)]`
- The VM must clamp and convert each channel from 16.16 fixed-point
  (`0x00000000` to `0x00FF0000` for 0..255) to 8-bit unsigned integer
  (`0..255`).
- The converted and clamped RGBA values are written to the output surface,
  which, together with the pixel's coordinates, determines the final rendered
  value in the surface/framebuffer.

#### Output Model Summary

- One shader invocation produces one output color (RGBA) for a pixel.
- The host/rasterizer copies this to the destination framebuffer/surface as
  appropriate.
- **Note: Any future changes (e.g SIMD support) will require this to be
  updated**

---

### Uniforms and Constants

- **Uniforms**: Constant for a draw/dispatch; loaded with `LOAD_UNIFORM n`.
- **Constants**: Built into compiled shader; loaded with `LOAD_CONST n`.
- Both are int32 (16.16 fixed-point), and are read-only in the VM.

---

### Example Shader Execution Flow

1. For each pixel:
   - Host computes and fills the `inputs[]` array.
   - Host optionally sets uniforms.
   - Interpreter runs the shader, producing output via `SET_COLOR`.
   - Host writes output color to surface.
2. Repeat for all pixels (or in SIMD batches, updating inputs[] for each batch).

---

### Extending Inputs/Outputs

- You may increase `MAX_INPUTS` to support more custom variables/varyings.
- Future: Multiple output values (e.g., for multi-target or auxiliary buffers)
  can be added with new opcodes (e.g., `SET_AUX`, `SET_MASK`).

---

## Native Extensions

Native extensions allow the shader VM to call out to high-performance,
pre-registered host (C, maybe JS) functions, enabling shaders to leverage math
routines, complex operations, or custom host functionality not expressible in
simple bytecode. This provides a clean and efficient mechanism for extending VM
capabilities without expanding the core opcode set.

---

### Overview

- **Opcode:** `CALL_NATIVE n` (`0x35`)
- **Mechanism:**\
  At interpreter dispatch, the VM calls the native function registered at index
  `n`, passing the current stack and expected argument count.
- **Host responsibility:**\
  Register all needed native functions before shader execution.
- **Validation:**\
  All referenced native function IDs in the shader must be registered and
  checked during shader validation.

---

### Native Function Table

- At context setup/initialization, the host provides a table of function
  pointers/callbacks.
- Each entry follows a standard calling convention, e.g.:
  ```c
  typedef int32_t (*AwteaNativeFunc)(ShaderContext* ctx, int argc);
  AwteaNativeFunc native_funcs[NATIVE_FUNC_COUNT];
  ```
- For fixed-point VM, all arguments and return values are 32-bit signed, in
  16.16 fixed-point, unless otherwise agreed.

---

### Opcode Semantics

| Name          | Code | Stack Args        | Stack Output | Description                                          |
| ------------- | ---- | ----------------- | ------------ | ---------------------------------------------------- |
| CALL_NATIVE n | 0x35 | [arg0, arg1, ...] | [result]     | Call native function n with N arguments, push result |

- The number of arguments for each native function is by convention, established
  at registration or documented in the host table.

---

### Function Signature

```c
// Example: single-argument "sin" function for fixed-point
int32_t fixed_sin(ShaderContext* ctx, int argc) {
    assert(argc == 1);
    int32_t angle = pop_stack(ctx);  // Fixed-point input
    return my_fixed_sin_impl(angle); // Implementation returns fixed-point
}
```

- More complex functions may pop/push more/fewer arguments.

---

### Registration Example

```c
// Host code, before shader execution:
native_funcs[0] = fixed_sin;
native_funcs[1] = fixed_exp;
// ... up to NATIVE_FUNC_COUNT
```

---

### Interpreter Dispatch Example

```c
case OP_CALL_NATIVE: {
    uint8_t fn_id = code[pc++];
    AwteaNativeFunc fn = native_funcs[fn_id];
    int argc = ...;     // TBD: Determine argument count for fn_id
    assert(fn);         // Validation guarantees this
    assert(sp >= argc); // Sufficient stack arguments
    int32_t result = fn(context, argc);
    push_stack(context, result);
    break;
}
```

---

### Validation

- All `CALL_NATIVE n` must refer to a valid registered function
  (`n < NATIVE_FUNC_COUNT`).
- During validation, each function use must conform to the stack discipline
  (sufficient arguments, expected return value).

---

### Use Cases

- Trigonometric and transcendental functions (sin, cos, log, exp)
- Procedural random number generation
- Per-pixel host utilities (dithering, gamma-correction)
- Extension to support special surface sampling or custom material logic
- Prototyping new opcodes

---

### Security/Robustness

- Only functions registered by the host and explicitly enabled are callable by
  shaders.
- No dynamic linking or unsafe pointers in the VM; registered functions are
  sandboxed by the host.

---

### Limitations

- NATIVE_FUNC_COUNT is a build-time constant (can be extended).
- Stack arguments and returns are always fixed-point 32-bit integers.
- No reentrant or recursive calls by default.

---

### Example Shader Snippet

```
LOAD_INPUT 0       ; push u
CALL_NATIVE 0      ; call native_func[0] (e.g., sin)
SET_COLOR
END
```

---

## Validation

Each shader bytecode should be validated before execution to prevent interpreter
errors, undefined behavior, or potential security vulnerabilities. Validation
ensures that the bytecode sequence is well-formed and will not cause stack
under/overflow or illegal memory/resource access.

### What to Validate

- **Opcode validity**: All opcodes are defined and supported.
- **Operand ranges**:
  - `LOAD_CONST n` n < MAX_CONSTS
  - `LOAD_UNIFORM n` n < MAX_UNIFORMS
  - `LOAD_INPUT n` n < MAX_INPUTS
  - `SAMPLE_SURFACE n` n < NUM_SURFACES (see `awt_raster_internal.h`)
  - `CALL_NATIVE n` n < NATIVE_FUNC_COUNT (currently 0)
- **Stack discipline**:
  - Ensure each instruction's stack requirements are always satisfied (e.g.,
    don’t pop from empty stack).
  - Track stack height statically through control-flow to ensure no underflow,
    and that the stack height is balanced across all program paths (no
    leaks/overflows).
  - Don't exceed the VM's maximum stack depth (see `MAX_STACK_SIZE`).
- **Branch targets**:
  - `JUMP`/`JZ`/`JNZ` offsets must not leave the bytecode region.
  - Branches should not allow jumps into the middle of multi-byte instructions.
- **Proper termination**:
  - There should be at least one `END` opcode, and control flow should
    eventually reach `END`.
- **SET_COLOR**:
  - Should only be executed when at least 4 values are on the stack.
- **Program length**:
  - Bytecode length doesn’t exceed implementation limits.
- **Optional/subroutine checks**:
  - If using `CALL`/`RET`, validate subroutine entry points and that all returns
    are valid.

### Optional: Additional Semantic Validation

- **Unused/Unreachable code** warning: unused bytecode after `END`.
- **Uninitialized stack values**: If analysis shows a value could be popped
  without being initialized.
- **Constant/uniform/input access**: Referenced indices are in-range for this
  shader.

### Validation Algorithm (Pseudocode)

1. Initialize stack height = 0 at entry.
2. Step through all opcodes, simulating stack effects:
   - For each instruction: Add/push expected outputs, remove/pops as per opcode
     spec.
   - If stack underflows at any point ⇒ INVALID.
   - If stack ever exceeds AWT_SHADER_STACK_MAX ⇒ INVALID.
3. For control flow (jumps, branches):
   - Simulate stack at branch/target destinations.
   - If stack discipline is ever ambiguous (height mismatch along different
     branches), either reject or log a warning.
   - Ensure all code reachable from entry and termination(s).
4. At `END`: Stack should be empty (or as specified by calling convention).
5. For each const/uniform/input/surface index used: check against table sizes
   allowed.

### When to Validate

- On shader compilation/loading—before accepting or executing bytecode.
- Modify validation as new instructions/opcodes are introduced.

### Validation Errors

- On validation failure, the shader should be rejected and an error
  thrown/logged with the opcode and bytecode offset where the error was
  detected.

## Limitations

The current version of the Awtea Shaders Bytecode VM is designed to be simple,
robust, and efficient for 2D raster shader execution with fixed-point
arithmetic. As such, it comes with several intentional limitations. These are
important to understand for both shader authors and VM integrators, as they
define the baseline behavior and expectations for the system.

---

### Arithmetic and Value Representation

- **Fixed-point only:** All numeric operations are 32-bit signed integers in
  16.16 fixed-point. There is no hardware or software floating-point support.
- **No 64-bit or double-precision math.**
- **Division/modulo:** Division and modulo by zero are validation/runtime
  errors; no defined result for these cases.

---

### Opcode and Program Structure

- **Limited opcode set:** Only the documented instructions are supported—no
  trigonometric, transcendental, or bitwise-rotate operations unless provided
  via native extensions.
- **No recursion or true function calls:** CALL/RET are reserved but not
  implemented. No call stack, only linear or loop control flow.
- **Native extension count is fixed** at build time (and may be zero), with no
  dynamic loading.
- **No dynamic memory allocation or pointers** within the VM.
- **No user-definable output variables:** Only one output color (RGBA) per
  invocation is standard; cannot write multiple targets or buffers.

---

### Limits and Resource Caps

- **Fixed resource sizes:**
  - `MAX_CONSTS`, `MAX_UNIFORMS`, `MAX_INPUTS`, `MAX_STACK_SIZE`, and
    `NATIVE_FUNC_COUNT` are compile-time constants.
  - The VM will not allocate more than the configured number of table entries or
    stack slots.
- **Stack depth:** `MAX_STACK_SIZE` is small (e.g., 16). Deeply nested or highly
  complex expressions may exceed this and will be rejected or fail at runtime.
- **No per-invocation memory/heap:** Only stack and input values are available
  per shader run.

---

### Graphics and Sampling

- **All sampling is point-sample, no filtering/linear/anisotropic modes** built
  into VM (can be implemented in host or via native extension).
- **Surfaces must be pre-allocated and valid**; VM does not handle image
  allocation, resizing, or format conversion.
- **Output is always RGBA;** non-RGBA destinations require host conversion or
  adaptation.
- **No alpha blending, Z-buffering, or per-pixel coverage/mask** features in the
  core VM.

---

### Concurrency and SIMD

- **Single-threaded; no reentrancy or parallel VM invocations** (unless
  multiple, independent VM instances are spun up by the host).
- **SIMD or batch execution is not yet implemented** but planned in
  extensibility.
- **No atomic or synchronization instructions**.

---

### Native Extensions

- **No untrusted/dynamic code loading:** All native functions must be
  pre-registered at application build/initialization.
- **No return of struct or non-int values:** Only int32 fixed-point return;
  complex types must be emulated in multiple calls.
- **No recursion or callbacks from native back into VM.**
- **Undefined behavior if stack discipline violated:** Native extensions must
  obey calling/stack convention.

---

### Shader Model and Semantics

- **No access to host state/env:** Shaders cannot read random memory, host time,
  or other system properties except what is passed via uniforms or native
  functions.
- **No explicit support for texture mipmapping, LOD, texture arrays, cubemaps,
  or hardware-specific features.**
- **No persistent or inter-pixel communication:** Each invocation is
  independent.
- **No assertion of determinism for native extensions:** Host code may cause
  side effects.

---

### Validation

- **Validators do not currently support full structured control-flow analysis.**
  Unusual bytecode (e.g., with complex flow graphs) may be rejected
  conservatively or require careful authoring.

---

### Extensibility

- **Many advanced features are left for future versions** (see Extensibility
  section).
- Current design is intentionally minimal to ease porting, debugging, and host
  integration.

---

Shader and host designers should keep these limitations in mind and ensure user
code and host app adapt as appropriate. Future versions may relax or extend
these, but **compatibility with this baseline is expected**.

## Extensibility

Awtea Shaders Bytecode VM is explicitly designed for future growth while
maintaining a simple initial core. This section describes ways the VM can evolve
to meet new use cases and anticipated demands from more advanced graphics,
compute, and host integration scenarios.

---

### Planned & Potential Extensions

#### 1. SIMD and Batch Execution

- **SIMD Interpreter:** Support batch execution of multiple pixels ("lanes") in
  parallel using vectorized instructions and mask logic.
- **Per-lane branching/masking:** Add predicated select, masked loops, and
  conditional execution for divergence within SIMD groups.
- **Inputs/outputs as SIMD vectors:** Adapt inputs/outputs structures to allow
  passing arrays or packed vectors for wide execution.

#### 2. Expanded Opcode Set

- **Math and transcendental functions:** Add `SIN`, `COS`, `TAN`, `LOG`, `EXP`,
  etc. as built-in or via native extensions.
- **Bit manipulation and utility ops:** Rotates, shifts, count-leading-zero,
  AND/OR/XOR, etc.
- **Specialized image ops:** Bilinear filtering, gather, scatter, address
  computation, swizzle, etc.
- **Custom output opcodes:** Set additional outputs, masks, or surfaces (for
  MRT, effects).

#### 3. User-Defined Functions

- **Implement subroutine support:** Enable `CALL` and `RET` for local (bytecode)
  functions, with a call frame/call stack.
- **Function tables:** Add a table of offsets or symbolic names, allowing easier
  linking and code reuse.

#### 4. Native Extension Improvements

- **Dynamic native function count:** Allow runtime registration beyond fixed
  build-time cap.
- **Extended argument/return convention:** Support multiple returns or
  struct-like returns.
- **Support non-int32 types:** Future versions could allow passing fixed-point
  vectors, booleans, or even float if needed.

#### 5. Surface and Sampling Model Extensions

- **Multiple texture units:** Support more (or variable/host-specified) surface
  counts.
- **Sampling modes:** Linear/bilinear/anisotropic sampling, mirror/repeat/clamp
  addressing.
- **Mipmap and LOD selection for textures.**

#### 6. Advanced Control Flow

- **Loop constructs:** Higher-level constructs or enhanced validation for
  bounded loops (`FOR`, `WHILE`, etc.).
- **Structured conditionals:** Hints or direct support for `IF/ELSE/ENDIF`
  blocks to improve bytecode clarity and validation.

#### 7. Output and Framebuffer Extensions

- **Multiple render targets:** Ability to write to several framebuffers/surfaces
  from a single shader call.
- **Auxiliary outputs:** Motion vectors, masks, Z-buffer, pick IDs, etc.

#### 8. Data-driven or External Data Access

- **Host data sharing:** Explicit host/user buffer interfaces for shader
  read/write.
- **Constant or uniform buffer expansion:** Larger or dynamically-sized buffers,
  or indirect access types.

#### 9. Meta-programming and Reflection

- **Shader introspection:** Ability to query required uniforms, constants,
  native calls, or other dependencies via an API.
- **Bytecode versioning and compatibility:** Documented upgrade path, forward
  compatibility, and deprecation mechanisms.

#### 10. Scripting/Host Language Bindings

- **Debugging and tracing hooks:** Better support for interactive tools and
  testing.

---

### Compatibility and Future-proofing

- **Versioning:** Every new feature or opcode should carry an explicit minimum
  VM version.
- **Optional features and negotiation:** Host and shaders can query/enumerate
  the features/extensions supported to enable graceful fallback.

### Feature Addition Policy

- All features should:
  - Preserve backward compatibility.
  - Not compromise validation and safety guarantees.
  - Be clearly documented with sample usage and edge-case handling.

## Assembler Specification

This section describes the syntax and expectations for an assembler that
compiles Awtea Shader Assembly Language (ASAL) into VM bytecode.

---

### High-Level Goals

- Simple, human-readable syntax for shader authors and tools.
- Direct mapping between assembly mnemonics and opcodes.
- Support for constants, uniforms, inputs, labels, and comments.
- Deterministic assembly: same input always produces same output.

---

### General Format

- **One instruction per line** (except labels and directives).
- **Whitespace** and **comments** (`;`, `#`, `//`) are ignored after an
  instruction.
- **Case-insensitive** for instructions and labels.
- **Labels** end with a colon at the beginning of a line.

---

### Instruction Format

```
MNEMONIC [arg1 [arg2 ...]]    ; optional comment
```

- Instructions are taken verbatim from the opcode table.
- Arguments are integers, names, or label references.

**Examples:**

```
LOAD_INPUT 0            ; Push u
LOAD_CONST 1
MUL
SET_COLOR
END
```

### Labels

- Labels mark code locations for jumps and branches.
- Must begin at start of line and end with a colon.
- Can be referenced as jump/call targets.

**Example:**

```
loop_start:
    LOAD_INPUT 0
    LOAD_CONST 4
    LT
    JZ loop_end
    ; ... body ...
    JUMP loop_start
loop_end:
    END
```

---

### Constants, Uniforms, and Inputs

- Constants, uniforms, and input indices are referenced by numeric index or
  macro.
- Pseudo-ops like `.const`, `.uniform`, are permitted to define symbolic names.

**Example:**

Constants:

```
.const PI 0x0003243F      ; defines macro-like PI = 0x0003243F
LOAD_CONST PI
```

Uniforms:

```
.uniform u_brightness ; defines a uniform - assemblier will emit a name -> index mapping
LOAD_UNFORM u_brightness
```

Inputs:

```
; There is no .input psudo-op, instead there are reserved names like:
LOAD_INPUT in_u
LOAD_INPUT in_v
```

---

### Sample Assembly Program

```
; Solid blue output
LOAD_CONST 0         ; R = 0
LOAD_CONST 0         ; G = 0
LOAD_CONST 0xFF0000  ; B = 255.0 (fixed)
LOAD_CONST 0x10000   ; A = 1.0 (fixed)
SET_COLOR
END
```

---

### Pseudo-ops and Directives

- **.const name value:** Defines a constant with a symbolic name.
- **.uniform name index:** Maps a symbolic name to a uniform index.
- **.surface name index:** Maps a symbolic name to a surface index.
- **.include "file.asal"**: Include another file (tool-specific - may not be
  present in inital impl).

---

### Comments

- Start with `;`, `//` or `#`. Everything after either symbol is ignored until
  end of line.

```
LOAD_INPUT 0    # u coordinate
LOAD_INPUT 1    ; v coordinate
//ABS           ; Commented out op
```

---

### Example With Control Flow

```
.const WHITE 0x00FFFFFF
.const OPAQUE 0x10000

LOAD_INPUT in_u     ; u
LOAD_CONST 0x8000   ; 0.5 in 16.16
LT                  ; u < 0.5?
LOAD_CONST WHITE
LOAD_CONST 0        ; black
SELECT
LOAD_CONST OPAQUE
SET_COLOR
END
```

---

### Encoding and Output

- Assembler must output a binary file (or buffer) comprising a header (TBD:
  format. Needs to include uniforms), followed by instructions in VM bytecode as
  defined in the spec.
- Label references are resolved into numeric addresses or offsets.
- Optional: support outputting a listing file mapping source and bytecode.

---

### Error Reporting

- The assembler should report:
  - Line/column of syntax errors.
  - Undefined label, macro, or pseudo-op references.
  - Invalid opcode or argument.
  - Stack underflow/overflow conditions detectable at assembly time.

---

### Extensibility

- As new opcodes, pseudo-ops, or features are added to the VM spec,
  corresponding assembler support must also be added.

---

### Example Tool Invocation

```
awtea-assemble my_shader.asal -o my_shader.bin
```

## Assembler Header Format

This section specifies the binary header layout emitted by the assembler at the
start of every Awtea Shader Bytecode file. This header precedes the actual
bytecode and provides metadata to support loading, validation, linking, and
runtime execution.

---

### Goals

- Self-describing: allows runtime or tools to quickly identify shader type,
  version, and required resources.
- Extensible: room for future entries/sections.
- Deterministic: always present, same layout for same assembler+spec revision.
- Allows mapping from symbolic names (used in `.uniform`, `.surface`, `.const`
  directives) to runtime indices.

---

### Header Layout (Initial Version)

**All fields are little-endian unless otherwise noted. All sizes are in bytes.**

| Offset | Size | Type                                           | Name/Description                          |
| ------ | ---- | ---------------------------------------------- | ----------------------------------------- |
| 0x00   | 4    | uint32_t                                       | Magic number (e.g. `0x41575453` = 'AWTS') |
| 0x04   | 2    | uint16_t                                       | VM version (major.minor, e.g. 0x0100)     |
| 0x06   | 2    | uint16_t                                       | Bytecode offset (from start of file)      |
| 0x08   | 2    | uint16_t                                       | Number of constants (N_CONSTS)            |
| 0x0A   | 2    | uint16_t                                       | Number of uniforms (N_UNIFORMS)           |
| 0x0C   | 2    | uint16_t                                       | Number of inputs (N_INPUTS)               |
| 0x0E   | 2    | uint16_t                                       | Number of surfaces (N_SURFACES)           |
| 0x10   | 4*N  | int32_t[]                                      | Constants table (N = N_CONSTS)            |
| 0xXX   | 4*M  | int32_t[]                                      | Uniform default values (M = N_UNIFORMS)   |
| 0xYY   | ?    | Name table (see below)                         |                                           |
| 0xZZ   | ...  | Bytecode section (begins at 'bytecode offset') |                                           |

---

### Name Table (Symbol Map)

The Name table is optional, and if emitted from the assembler, names are stored
here for each resource table, mapping symbolic name (from `.uniform name`, etc.)
to table index.

**Format:**

The table is prefixed with

| Offset | Size | Type    | Name/Description                     |
| ------ | ---- | ------- | ------------------------------------ |
| 0x00   | 1    | uint8   | The type of the symbol               |
| 0x01   | 2    | uint16  | Position of the symbol in it's table |
| 0x03   | 1    | uint8   | The Length of the name in bytes      |
| 0x04   | n    | char[n] | The name of the symbol               |

**Resource type values:**

- 0 = Sentinel, end of list
- 1 = constant
- 2 = uniform
- 3 = input
- 4 = surface

If names are not used, this section may be empty

---

### Example (Packed, canonical order):

```
[Magic] [Version] [Bytecode offset]
[ConstCount] [UniformCount] [InputCount] [SurfaceCount]
[Constants]                  -- N * 4 bytes
[UniformDefaults]            -- M * 4 bytes
{Name Table}
[Bytecode Instructions...]   -- Starting at Bytecode offset
```

---

### Minimal Example

For a shader with 2 constants, 1 uniform, 2 inputs, 1 surface, and 12 bytes of
bytecode:

```
Offset  Data
0       0x53 0x54 0x57 0x41          ; Magic: 'AWTS'
4       0x00 0x01                    ; VM version 1.0
6       0x28 0x00                    ; Bytecode offset = 40
8       0x02 0x00                    ; N_CONSTS = 2
10      0x01 0x00                    ; N_UNIFORMS = 1
12      0x02 0x00                    ; N_INPUTS = 2
14      0x01 0x00                    ; N_SURFACES = 1
16      [const0 4 bytes]
20      [const1 4 bytes]
24      [uniform0 default 4 bytes]
28      [name table...]
40      [bytecode...]
```

_(Offsets and field locations grow linearly with higher counts.)_

---

### Bytecode Section

- All instructions are byte-packed as specified in the opcode table, with
  arguments as defined (opcode, then args, then next opcode).
- Label resolution is already performed; jumps/calls use numeric offsets.

---

### Future / Extensions

- TBD

---

### Reading Procedure (Host/VM)

1. Parse magic/version/offsets.
2. Load constants, uniforms, inputs, surfaces.
3. Build lookup tables for symbolic names (if present).
4. Begin interpreting the bytecode at the indicated offset.

---
