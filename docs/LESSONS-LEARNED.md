# Lessons Learned - POP-ALL Experiment

**Date:** 2025-11-06
**Experiment:** Testing whether improved POP-ALL v2 prompt achieves 90%+ success rate

## The Journey: From 90% → 0% → TBD

### Act 1: The Hypothesis

**Starting point:**
- Baseline CLJ-PP (explicit POP): **90% success** (9/10)
- CLJ-PP + POP-ALL v1: **80% success** (8/10)

**The problem:** Fresh instances used POP-ALL correctly but then added trailing POPs:
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP   ← ERROR! Stack already empty
POP     ← ERROR!
```

**The hypothesis:** The original prompt didn't emphasize "no POPs after POP-ALL" strongly enough.

### Act 2: The Improved Prompt

Created `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md` with:
- **THE CRITICAL RULE FOR POP-ALL** section (top-level, unmissable)
- **Example 8: WRONG pattern** with ❌ showing POPs after POP-ALL
- **Error #1: POPs After POP-ALL** dedicated error section
- **"POP-ALL means STOP"** repeated 5+ times throughout
- **Final Reminders** section reinforcing the rule

**Expected outcome:** 90%+ success if prompting was the issue.

### Act 3: The Test Disasters

**Attempt 1: Permission requests (0/20 success)**
```
I need permission to write the test3.cljp file. Once granted, I'll create it with:
```

**Lesson learned:** `claude --print` invokes Claude Code, which asks for permission instead of printing code.

**Attempt 2: Markdown fences (0/20 success)**
```
```clojure
PUSH-( ns examples.test3 POP-ALL
...
```
```

**Lesson learned:** Claude Code wraps output in markdown formatting.

**Attempt 3: Missing jar file (0/20 success)**
```
Error: Could not find or load main class clojure.main
```

**Lesson learned:** The `bin/cljpp` script expects `bin/cljpp.jar` but `make uberjar` puts it in `target/`.

### Act 4: The Fixes

1. **Removed CLAUDE.md contamination**
   - Backed up detailed experiment notes
   - Created minimal CLAUDE.md with no CLJ-PP details

2. **Added post-processing pipeline**
   ```bash
   # Generate code
   claude --print "${PROMPT}\n\n${REQUIREMENTS}" > output.raw.cljpp

   # Remove markdown fences
   sed -e '/^```clojure$/d' -e '/^```$/d' output.raw.cljpp > output.cljpp

   # Transpile
   bin/cljpp output.cljpp > output.clj
   ```

3. **Fixed jar location**
   ```bash
   make uberjar
   cp target/cljpp.jar bin/cljpp.jar
   ```

4. **Created comprehensive docs**
   - `docs/TESTING-FRESH-INSTANCES.md` - All the gotchas
   - Documents permission requests, markdown fences, jar locations

### Act 5: The Final Test (In Progress)

Running `test-popall-v2-clean.sh` with:
- ✅ Minimal CLAUDE.md (no contamination)
- ✅ POP-ALL v2 improved prompt
- ✅ Markdown fence removal (`sed`)
- ✅ Working transpiler (`bin/cljpp.jar`)

**Results:** TBD (test currently running)

## Key Insights

### 1. `claude --print` != Pure LLM

`claude --print` invokes **Claude Code**, not a pure API call. This means:
- It tries to be helpful (asks for permissions, formats output)
- It reads `.claude/CLAUDE.md` (contamination risk)
- It behaves like an interactive coding assistant

**Implication:** Fresh instance tests need post-processing and isolation.

### 2. Test Your Test Setup

Before running 20 iterations:
```bash
# Test one iteration manually
claude --print "$(cat PROMPT.md)\n\nRequirements" > test.cljpp
cat test.cljpp  # Verify it's actual code!
```

If you see explanations, permission requests, or markdown fences → fix before scaling up.

### 3. The Complete Testing Pipeline

```bash
# 1. Isolate environment
mv CLAUDE.md CLAUDE.md.backup
echo "# Minimal notes" > CLAUDE.md

# 2. Ensure tools work
make uberjar
cp target/cljpp.jar bin/cljpp.jar

# 3. Test one iteration
./test-script.sh 1  # Verify output format

# 4. Scale up
./test-script.sh 20

# 5. Restore environment
mv CLAUDE.md.backup CLAUDE.md
```

### 4. The Irony

We spent more time fighting the test infrastructure than testing the hypothesis!

**Time breakdown:**
- Creating improved prompt: 30 minutes
- Discovering and fixing test issues: 2 hours
- Actually testing the hypothesis: TBD

**Lesson:** Infrastructure matters. Document your gotchas.

## Documentation Created

1. `docs/TESTING-FRESH-INSTANCES.md`
   - All the gotchas (permission requests, markdown fences, jar locations)
   - Complete testing pipeline
   - Best practices for fresh instance isolation

2. `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md`
   - Improved prompt with explicit warnings
   - "THE CRITICAL RULE" section
   - Wrong pattern examples with ❌

3. `experiments/POP-ALL-V2-HYPOTHESIS.md`
   - Hypothesis and expected outcomes
   - Success criteria (90%+ to match baseline)

4. `docs/LESSONS-LEARNED.md` (this file)
   - The journey from 90% → 0% → TBD
   - All the failures and fixes

## Open Questions

**If POP-ALL v2 achieves 90%+:**
- Prompting matters! Clear error examples work
- POP-ALL is viable with proper instruction
- Fresh instances can learn from WRONG patterns

**If POP-ALL v2 stays at ~80%:**
- Decision fatigue is fundamental
- Multiple options confuse fresh instances
- Baseline (explicit POP) is optimal

**Either way:**
- We learned a ton about testing fresh instances
- We documented all the gotchas
- Future experiments will be faster

## Next Steps

1. **Wait for test results** (currently running)
2. **Update EXPERIMENT-RESULTS.md** with findings
3. **Decide on POP-ALL future:**
   - If 90%+: Keep POP-ALL, use v2 prompt
   - If ~80%: Stick with baseline (explicit POP)

## The Meta-Lesson

**Testing is product development.**

We didn't just test a hypothesis - we:
- Built a testing framework
- Discovered edge cases
- Created documentation
- Developed best practices

This infrastructure will make ALL future experiments faster and more reliable.

**Time invested:** ~2.5 hours
**Value created:** Reusable testing framework + comprehensive docs
**ROI:** High (every future experiment benefits)

---

**Status:** Test currently running at `experiments/popall-v2-clean/run-20251106-190725/`
**Expected completion:** ~10 minutes (20 iterations × ~30s each)
