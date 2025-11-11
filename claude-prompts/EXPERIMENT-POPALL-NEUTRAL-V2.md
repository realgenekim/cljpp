# CLJ-PP: Preventing POP Underflow with POP-ALL

## The Problem: POP Underflow Errors

CLJ-PP requires matching every `PUSH-` with a `POP`. The most common error is **POP underflow**:

```
{:code :underflow, :msg "POP with empty stack", :line 31}
```

This happens when you emit more POPs than PUSHes. You lose track of the count.

## The Solution: POP-ALL

**POP-ALL closes all open containers at once.** It prevents underflow by eliminating the need to count POPs precisely.

## CLJ-PP Syntax

```
PUSH-(  # Open list
PUSH-[  # Open vector
PUSH-{  # Open map
POP     # Close one container
POP-ALL # Close ALL containers (prevents underflow)
```

## When To Use POP vs POP-ALL

### Use POP for intermediate closes

When there's more content coming at the same level:

```clojure
PUSH-( defn add PUSH-[ a b POP    ← Close vector, defn still open
  PUSH-( + a b POP                 ← Close + call, defn still open
POP                                ← Close defn
```

### Use POP-ALL to finish forms

When you're completely done with a form, use POP-ALL instead of counting POPs:

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL    ← Closes + call AND defn (no counting!)
```

**Advantage:** No risk of underflow. POP-ALL closes everything at once.

## Examples

### Example 1: Simple Function

```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP-ALL
```

**Without POP-ALL (error-prone):**
```clojure
PUSH-( defn add PUSH-[ a b POP
  PUSH-( + a b POP
POP    ← Easy to forget this! → Underflow if you add one too many
```

**With POP-ALL (safe):**
- Just use POP-ALL at the end
- No counting required
- No underflow risk

### Example 2: Nested Calls

```clojure
PUSH-( defn double-inc PUSH-[ x POP
  PUSH-( * 2 PUSH-( inc x POP POP-ALL
```

**Breakdown:**
- `PUSH-[ x POP` - parameters
- `PUSH-( inc x POP` - inner call
- `POP-ALL` - close outer call AND defn (prevents underflow)

### Example 3: Deep Nesting

```clojure
PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
```

**Without POP-ALL:** Would need 3 POPs (easy to miscount)
**With POP-ALL:** One operation closes all three levels

### Example 4: Factorial

```clojure
PUSH-( defn factorial PUSH-[ n POP
  PUSH-( if PUSH-( <= n 1 POP
    1
    PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP POP POP-ALL
```

**Pattern:**
- Use POP for intermediate closes (parameters, condition)
- Use POP-ALL at the very end (no counting, no underflow)

### Example 5: Let Binding

```clojure
PUSH-( let PUSH-[
  user PUSH-{ :name "Alice" :age 30 POP
POP
  PUSH-( println user POP-ALL
```

**Pattern:**
- `POP` after map (bindings vector still open)
- `POP` after bindings (let still open)
- `POP-ALL` at end (closes println AND let)

## Key Rules

1. **Use POP** when more content is coming at the same level
2. **Use POP-ALL** when completely done with a form
3. **After POP-ALL**, the stack is empty - only new top-level forms can follow

## Common Pattern

```clojure
# Intermediate closes - use POP
PUSH-[ args POP           ← Body coming
PUSH-[ bindings POP       ← Body coming

# Final close - use POP-ALL
PUSH-( final-expr POP-ALL ← Everything done, no underflow
```

## Preventing Errors

**Without POP-ALL (underflow risk):**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( + x 1 POP
POP
POP    ← Too many! → Underflow error
```

**With POP-ALL (safe):**
```clojure
PUSH-( defn foo PUSH-[ x POP
  PUSH-( + x 1 POP-ALL    ← Can't underflow, closes everything
```

## Error Messages

- `"POP with empty stack"` → Underflow (too many POPs)
- `"Unclosed containers at EOF"` → Missing POP-ALL at end

**POP-ALL prevents the first error** by closing all containers at once.

## CRITICAL: Format Requirements

**Your first token MUST be `PUSH-` or `(`. Do NOT write any explanatory text before code.**

❌ **WRONG:**
```
I'll write this using CLJ-PP.

PUSH-( defn factorial...
```

✅ **RIGHT:**
```clojure
PUSH-( defn factorial...
```

## Summary

**POP-ALL eliminates underflow risk** by closing all containers at once. Use it at the end of forms to avoid counting POPs precisely.
