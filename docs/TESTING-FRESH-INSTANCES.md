# Testing Fresh Claude Instances - Critical Gotchas

## The Problem: Claude Code vs Claude API

When testing fresh instances with `claude --print`, you're actually invoking **Claude Code**, not the Claude API. This has important implications for experiment design.

## Gotcha #1: File Permission Requests

**Symptom:** Your test gets 0% success rate, and generated files contain text like:
```
I need permission to write the test3.cljp file. Once granted, I'll create it with:

**factorial function:**
- Opens with `cond`
...
```

**Root cause:** Claude Code interprets prompts as interactive coding sessions, not as simple "print this code" requests. When given detailed instructions, it tries to be helpful by:
1. Asking for permission to write files
2. Explaining what it will do before doing it
3. Providing summaries and context

**The fix:** Add explicit instruction in your prompt:
```bash
PROGRAM_SPEC="Your requirements here...

IMPORTANT: Output ONLY the CLJ-PP code. No explanations, no asking for permission,
just the code starting with PUSH-( ns examples.test3 POP-ALL"
```

## Gotcha #2: CLAUDE.md Contamination

**Symptom:** Fresh instances perform better or worse than expected, or seem to "know" about your experiment.

**Root cause:** `claude --print` loads your `.claude/CLAUDE.md` file, which may contain:
- Detailed notes about CLJ-PP experiments
- Success rate data from previous tests
- Hypotheses about what works or doesn't work
- Implementation details about POP-LINE, POP-ALL, etc.

Fresh instances aren't actually "fresh" - they have context from CLAUDE.md!

**The fix:** Before running experiments:
```bash
# Backup your detailed notes
mv CLAUDE.md CLAUDE.md.backup-before-experiment

# Create minimal CLAUDE.md with no experiment details
cat > CLAUDE.md << EOF
# CLJ-PP Tokenizer Project

This project implements a stack-based intermediate format for generating Clojure code.

## Commands

\`\`\`bash
make runtests-once
\`\`\`
EOF
```

## Gotcha #3: Long Prompts Trigger Conversational Mode

**Symptom:** Claude explains what it's going to do instead of just doing it.

**Root cause:** Very long, detailed prompts (like our 400-line CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md) make Claude Code think it's in an interactive session.

**The fix:** Add explicit "just print code" instruction at the END of your prompt:
```
IMPORTANT: You are being called via `claude --print`. Output ONLY the code,
starting immediately with the first PUSH- token. No explanations, no asking
for permission, no commentary.
```

## Best Practices for Fresh Instance Testing

### 1. Isolate the Test Environment

```bash
# Before experiment
mv CLAUDE.md CLAUDE.md.backup
echo "# Minimal project notes" > CLAUDE.md

# After experiment
mv CLAUDE.md.backup CLAUDE.md
```

### 2. Be Explicit About Output Format

Always end your test prompt with:
```
OUTPUT FORMAT: Print ONLY the code. No explanations. Start with: PUSH-( ns ...
```

### 3. Test Your Test Setup First

Before running 20 iterations:
```bash
# Test one iteration manually
claude --print "$(cat YOUR-PROMPT.md)\n\nYour requirements" > test.cljpp
cat test.cljpp  # Verify it's actual code, not explanations
```

If you see explanations or permission requests, add more explicit instructions.

### 4. Use Timestamped Output Directories

Never overwrite previous test runs:
```bash
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="experiments/test-name/run-${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"
```

This lets you compare results across experiments.

### 5. Log Everything

```bash
echo "Test details" | tee -a "$OUTPUT_DIR/log.txt"
```

This creates both console output and a permanent log file.

## Example: Correct Test Script Structure

```bash
#!/bin/bash
set -e

# 1. Setup
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_DIR="experiments/my-test/run-${TIMESTAMP}"
mkdir -p "$OUTPUT_DIR"

# 2. Load prompt
PROMPT=$(cat MY-PROMPT.md)

# 3. Define requirements with EXPLICIT output instruction
REQUIREMENTS="Your requirements here...

IMPORTANT: Output ONLY the code. No explanations, no permission requests.
Start immediately with: PUSH-( ns ..."

# 4. Test loop
for i in $(seq 1 20); do
    echo "Iteration $i..."

    # Generate code
    claude --print "${PROMPT}\n\n${REQUIREMENTS}" > "$OUTPUT_DIR/iter${i}.cljpp" 2>&1

    # Verify it's actual code (not explanation text)
    if head -1 "$OUTPUT_DIR/iter${i}.cljpp" | grep -q "^PUSH-"; then
        echo "  ✓ Got code"
        # Continue with transpile/test...
    else
        echo "  ✗ Got explanation text instead of code"
    fi
done

# 5. Save results
cat > "$OUTPUT_DIR/RESULTS.md" << EOF
# Results
...
EOF
```

## Common Failure Patterns

### Pattern 1: Permission Request

```
I need permission to write the file...
```

**Fix:** Add "Output ONLY the code" to your prompt.

### Pattern 2: Explanation Before Code

```
I'll create a factorial function that:
- Uses cond for base case
- Recurses with n-1

PUSH-( ns ...
```

**Fix:** Add "Start immediately with PUSH-" to your prompt.

### Pattern 3: Code Wrapped in Markdown Fences

```
```clojure
PUSH-( ns examples.test3 POP-ALL
...
```
```

**Fix:** Add "Do not use markdown code fences" to your prompt:
```
IMPORTANT: Output ONLY the CLJ-PP code. Do not wrap in ```clojure blocks.
Start immediately with: PUSH-( ns ...
```

### Pattern 4: No Code at All

```
To write this in CLJ-PP format, I would use...
```

**Fix:** Change prompt from instructional/educational tone to directive tone:
- ❌ "Here's how to write CLJ-PP..."
- ✅ "Write this code in CLJ-PP format. Output starts here:"

## Verifying Fresh Instance Isolation

To verify your fresh instances are truly isolated:

1. **Check CLAUDE.md is minimal** - No experiment details
2. **Test one iteration manually** - Verify output format
3. **Compare first vs last iteration** - Should show no learning curve
4. **Random sample check** - Different fresh instances should make similar errors

If later iterations perform better than early ones, you likely have:
- Context leaking between iterations
- CLAUDE.md containing relevant information
- Shell history contaminating prompts

## Success Metrics

A properly isolated fresh instance test should show:
- **Consistent error patterns** across iterations
- **No improvement** from iteration 1 to iteration 20
- **Similar failures** when re-run with different random seed

If you see improvement over iterations, your instances aren't fresh!

## The Complete Solution: Post-Processing Pipeline

Since `claude --print` invokes Claude Code (which tries to be helpful), you need a post-processing pipeline:

```bash
# Generate code
claude --print "${PROMPT}\n\n${REQUIREMENTS}" > output.raw.cljpp 2>&1

# Remove markdown fences
sed -e '/^```clojure$/d' -e '/^```$/d' output.raw.cljpp > output.cljpp

# Now transpile the cleaned output
bin/cljpp output.cljpp > output.clj
```

This handles all the gotchas:
- Markdown code fences → stripped
- Permission requests → fail fast (no code to strip)
- Explanatory text → fails transpile (shows in error analysis)

## Summary: The Golden Rules

1. **Minimal CLAUDE.md** - No experiment contamination
2. **Post-process output** - Strip markdown fences with `sed`
3. **Test your test** - Verify one iteration manually first
4. **Log everything** - Timestamped directories, never overwrite
5. **Verify isolation** - Check for learning curves (there shouldn't be any)
6. **Save raw output** - Keep `.raw.cljpp` for debugging

Following these rules ensures your "fresh instance" experiments actually test fresh instances, not instances with hidden context.
