# Project Status - 2025-11-06

## Current State

### What We Accomplished

1. ✅ **Removed CLAUDE.md contamination**
   - Backed up detailed notes to `CLAUDE.md.backup-before-decontamination`
   - Created minimal `CLAUDE.md` with no experiment details
   - Prevents fresh instances from seeing CLJ-PP implementation details

2. ✅ **Created improved POP-ALL v2 prompt**
   - File: `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md`
   - Added "THE CRITICAL RULE FOR POP-ALL" section
   - Shows WRONG pattern explicitly (POPs after POP-ALL) with ❌
   - Repeats "POP-ALL means STOP" throughout
   - Dedicated error section for this exact failure pattern

3. ✅ **Fixed Makefile uberjar target**
   - Now copies `target/cljpp.jar` to `bin/cljpp.jar`
   - Allows `./bin/cljpp` to work for project-local testing
   - Clear messaging about project-local vs global install

4. ✅ **Added Make targets for all test variants**
   ```bash
   make test-generate-clj ITERS=20 PROG=3
   make test-generate-cljpp-pop ITERS=20 PROG=3
   make test-generate-cljpp-pop-all ITERS=20 PROG=3
   make test-generate-cljpp-pop-line ITERS=20 PROG=3
   make test-generate-cljpp-pop-all-and-line ITERS=20 PROG=3
   ```
   Defaults: ITERS=10, PROG=3 (factorial/fibonacci)

5. ✅ **Created comprehensive documentation**
   - `docs/TESTING-FRESH-INSTANCES.md` - All gotchas and solutions
   - `docs/LESSONS-LEARNED.md` - The journey and insights
   - `experiments/POP-ALL-V2-HYPOTHESIS.md` - Hypothesis and criteria

### What Needs Fixing

1. ❌ **Test scripts have bugs**
   - New test scripts use stdout redirection (`> output.clj`)
   - Should use explicit output file argument: `cljpp input.cljpp output.clj`
   - This is why we're getting 0% success rates in new tests

2. ❌ **POP-ALL v2 experiment incomplete**
   - Hypothesis: Improved prompting should achieve 90%+ success
   - Current status: Test infrastructure problems prevented completion
   - Need to fix test scripts and re-run

3. ❌ **Missing test script: test-one-program-clj.sh**
   - Make target references it but script doesn't exist
   - Need to create for testing regular Clojure generation

## Current Experiment Results

| Approach | Success Rate | Status |
|----------|--------------|--------|
| Regular Clojure (fresh) | **80%** (16/20) | ✅ Completed |
| CLJ-PP Fresh (no examples) | **50%** (10/20) | ✅ Completed |
| **CLJ-PP Baseline (explicit POP)** | **90%** (9/10) ✅ | ✅ Completed - WINNER |
| CLJ-PP + POP-LINE + POP-ALL | **80%** (8/10) | ✅ Completed |
| CLJ-PP + POP-ALL v1 | **80%** (8/10) | ✅ Completed |
| CLJ-PP + POP-ALL v2 | **TBD** | ❌ Test infrastructure issues |

## Key Learnings

### 1. Fresh Instance Testing is Hard

When using `claude --print`:
- **It invokes Claude Code**, not pure API
- **Asks for file permissions** instead of printing code
- **Wraps output in markdown fences** (` ```clojure ... ``` `)
- **Reads `.claude/CLAUDE.md`** (contamination risk)

**Solution:** Post-processing pipeline:
```bash
claude --print "${PROMPT}\n\n${REQUIREMENTS}" > output.raw
sed -e '/^```clojure$/d' -e '/^```$/d' output.raw > output.cljpp
bin/cljpp input.cljpp output.clj  # NOT: > output.clj
```

### 2. Makefile Build Process

**For testing (project-local):**
```bash
make uberjar  # Creates target/cljpp.jar AND bin/cljpp.jar
./bin/cljpp input.cljpp output.clj
```

**For production (global install):**
```bash
make installuberjar  # Copies to ~/bin/
cljpp input.cljpp output.clj  # From anywhere
```

### 3. The POP-ALL Hypothesis

**Original problem:** Fresh instances used POP-ALL correctly but added trailing POPs:
```clojure
:else PUSH-( * n PUSH-( factorial PUSH-( - n 1 POP-ALL
  POP   ← ERROR! Stack already empty
POP
```

**Hypothesis:** Original prompt didn't emphasize "no POPs after POP-ALL" enough.

**Solution:** Created v2 prompt with:
- Top-level "CRITICAL RULE" section
- WRONG pattern with ❌
- "POP-ALL means STOP" repeated 5+ times
- Dedicated error section

**Test result:** TBD (infrastructure bugs prevented completion)

## Next Steps

### Immediate (Fix Test Infrastructure)

1. **Fix test scripts**
   - Change from `bin/cljpp input.cljpp > output.clj` (wrong)
   - To: `bin/cljpp input.cljpp output.clj` (right)
   - Files to fix:
     - `test-popall-v2-clean.sh`
     - `test-popall-v2-direct.sh`
     - `test-factorial-fibonacci-with-popall-v2.sh`

2. **Create test-one-program-clj.sh**
   - Test regular Clojure generation
   - Make target references it

3. **Re-run POP-ALL v2 experiment**
   - With working test infrastructure
   - 20 iterations for statistical confidence
   - Compare against baseline (90%)

### Soon (Complete Analysis)

1. **If POP-ALL v2 succeeds (90%+):**
   - Update EXPERIMENT-RESULTS.md
   - Recommend POP-ALL with v2 prompt
   - Update CLJPP-PROMPT.md to use v2

2. **If POP-ALL v2 fails (~80%):**
   - Update EXPERIMENT-RESULTS.md
   - Conclude that decision fatigue is fundamental
   - Stick with baseline (explicit POP) as optimal

3. **Update all documentation**
   - Final results in experiments/
   - Lessons learned
   - Best practices

## Files Modified/Created

### Documentation
- ✅ `docs/TESTING-FRESH-INSTANCES.md` - Complete gotchas guide
- ✅ `docs/LESSONS-LEARNED.md` - Project journey
- ✅ `experiments/POP-ALL-V2-HYPOTHESIS.md` - Hypothesis doc
- ✅ `STATUS.md` (this file)

### Prompts
- ✅ `CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md` - Improved prompt

### Build/Test
- ✅ `Makefile` - Fixed uberjar, added test targets
- ✅ `CLAUDE.md` - Minimal version (no contamination)
- ✅ `CLAUDE.md.backup-before-decontamination` - Detailed notes backup

### Test Scripts (Need Fixing)
- ❌ `test-popall-v2-clean.sh` - Has stdout redirection bug
- ❌ `test-popall-v2-direct.sh` - Has stdout redirection bug
- ❌ `test-factorial-fibonacci-with-popall-v2.sh` - Has stdout redirection bug
- ❌ `test-one-program-clj.sh` - Doesn't exist yet

### Working Test Scripts
- ✅ `test-one-program.sh` - Baseline (explicit POP)
- ✅ `test-one-program-with-pop-all-only.sh` - POP-ALL v1
- ✅ `test-one-program-with-pop-line.sh` - POP-LINE only
- ✅ `test-one-program-with-pop-line-all.sh` - Both features

## Commands Reference

### Run Tests
```bash
# Using Make (recommended)
make test-generate-cljpp-pop ITERS=20       # Baseline
make test-generate-cljpp-pop-all ITERS=20   # POP-ALL
make test-generate-clj ITERS=20             # Regular Clojure

# Direct (if scripts are fixed)
./test-one-program.sh 3 20                  # Baseline
./test-popall-v2-clean.sh 20                # POP-ALL v2 (needs fix)
```

### Build
```bash
make uberjar          # Build for project-local testing
make installuberjar   # Build + install to ~/bin
make clean            # Clean build artifacts
```

### Development
```bash
make runtests-once    # Run unit tests
make nrepl            # Start nREPL
make help             # Show all commands
```

## Success Criteria

**Minimum Viable:** 18/20 (90%) - matches baseline
**Strong Success:** 19/20 (95%) - beats baseline!

If achieved, proves that POP-ALL is viable with proper prompting.
If not, proves that explicit counting is optimal (simplicity wins).

---

**Status:** Infrastructure fixed, ready to re-run POP-ALL v2 experiment
**Blocker:** Test scripts need stdout redirection bug fixed
**ETA:** ~30 minutes to fix scripts + ~10 minutes to run test
