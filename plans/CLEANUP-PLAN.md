# Repository Cleanup Plan

**Date:** 2025-11-06

## Current State

The root directory is cluttered with prompts, logs, test scripts, and documentation files that should be organized into proper subdirectories.

## Proposed Structure

```
├── README.md                    # Keep - main project docs
├── CLAUDE.md                    # Keep - project instructions for Claude
├── Makefile                     # Keep - build system
├── deps.edn                     # Keep - Clojure deps
├── build.clj                    # Keep - build script
├── tests.edn                    # Keep - test config
├── .gitignore                   # Keep
│
├── claude-prompts/              # NEW - All LLM generation prompts
│   ├── CLJPP-PROMPT.md         # v1 - The winner (80%)
│   ├── CLJPP-PROMPT-v4.md      # v4 - Failed hybrid
│   ├── CLJPP-PROMPT-v5.md      # v5 - Failed #() fix
│   ├── CLJPP-PROMPT-WITH-POP-ALL-ONLY.md       # v2 original
│   ├── CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md    # v2 (75%)
│   ├── CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md    # v3 (60%)
│   ├── CLJPP-PROMPT-WITH-POP-LINE-ALL.md
│   └── CLJPP-PROMPT-WITH-POP-LINE-ALL-v2.md
│
├── docs/                        # Keep - Documentation
│   ├── TESTING-GUIDE.md        # MOVE FROM ROOT
│   ├── POSTMORTEM-v4.md        # MOVE FROM ROOT
│   ├── PROMPT-TUNING-NOTES.md  # MOVE FROM ROOT
│   ├── README-v4.md            # MOVE FROM ROOT (archive)
│   ├── cljp-readability-analysis.md
│   ├── cljp-specification.md
│   ├── EXPERIMENT-REPRODUCIBILITY-PLAN.md
│   ├── LESSONS-LEARNED.md
│   ├── llm-analysis.md
│   ├── should-cljp-be-readable.md
│   └── TESTING-FRESH-INSTANCES.md
│
├── test-data/                   # NEW - Test inputs/outputs
│   ├── test-prompts.txt        # MOVE FROM ROOT
│   └── test-prompts-cljpp.txt  # MOVE FROM ROOT
│
├── bin/                         # Keep - Executables
├── src/                         # Keep - Source code
├── test/                        # Keep - Unit tests
├── experiments/                 # Keep - Test results (timestamped)
├── plans/                       # Keep - Design docs
├── OLD/                         # Keep - Deprecated scripts
│
└── ARCHIVE/                     # NEW - One-off logs and deprecated files
    ├── build-prompt.sh         # MOVE - One-time utility
    ├── test-with-metadata.sh   # MOVE - Deprecated test script
    ├── fresh-experiment-log.txt         # MOVE - Old log
    ├── fresh-experiment-cljpp-log.txt   # MOVE - Old log
    └── CLAUDE.md.backup-before-decontamination  # MOVE
```

## Actions

### 1. Create new directories
```bash
mkdir -p claude-prompts
mkdir -p test-data
mkdir -p ARCHIVE
```

### 2. Move prompt files
```bash
mv CLJPP-PROMPT*.md claude-prompts/
```

### 3. Move documentation files to docs/
```bash
mv TESTING-GUIDE.md docs/
mv POSTMORTEM-v4.md docs/
mv PROMPT-TUNING-NOTES.md docs/
mv README-v4.md docs/
```

### 4. Move test data
```bash
mv test-prompts.txt test-data/
mv test-prompts-cljpp.txt test-data/
```

### 5. Handle shell scripts in root

**Decision: Delete deprecated scripts, no archiving needed**

```bash
# Delete - Superseded by bb bin/test-variant.clj
rm test-with-metadata.sh

# Keep if useful, otherwise delete
rm build-prompt.sh  # One-time utility for prompt building
```

### 6. Archive old log files
```bash
mv fresh-experiment-log.txt ARCHIVE/
mv fresh-experiment-cljpp-log.txt ARCHIVE/
mv CLAUDE.md.backup-before-decontamination ARCHIVE/
```

### 7. Delete test output directories (regenerate if needed)
```bash
# These are generated artifacts - can be deleted
rm -rf test-output/
rm -rf test-output-clj/
rm -rf test-output-clj-round2/
rm -rf test-tuning/
```

### 8. Update references

Files that reference prompts need to be updated:

**bin/test-variant.clj:**
```clojure
(def variants
  {:clj    {:file nil :name "Regular Clojure"}
   :pop    {:file "claude-prompts/CLJPP-PROMPT.md" :name "CLJ-PP (explicit POP)"}
   :popall {:file "claude-prompts/CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md" :name "CLJ-PP (POP-ALL v2)"}
   :v3     {:file "claude-prompts/CLJPP-PROMPT-WITH-POP-ALL-ONLY-v3.md" :name "CLJ-PP (POP-ALL v3)"}
   :v4     {:file "claude-prompts/CLJPP-PROMPT-v4.md" :name "CLJ-PP v4 (Hybrid)"}
   :v5     {:file "claude-prompts/CLJPP-PROMPT-v5.md" :name "CLJ-PP v5 (v1 + #() fix)"}})
```

**experiments/EXPERIMENT-RESULTS.md:**
- Update prompt file paths in the results table
- Change `CLJPP-PROMPT.md` → `claude-prompts/CLJPP-PROMPT.md`

**README.md:**
- Update link to TESTING-GUIDE.md: `docs/TESTING-GUIDE.md`

**Any scripts in bin/ or OLD/:**
- Search for references to moved files

### 9. Update .gitignore

Add to .gitignore:
```
# Test outputs (regenerated)
test-output/
test-output-*/
test-tuning/

# Build artifacts
target/
.cpcache/
META-INF/

# Editor files
.lsp/
.clj-kondo/
```

## Benefits

1. **Root directory cleanup** - Only essential files (README, CLAUDE.md, Makefile, configs)
2. **Clear organization** - Prompts in one place, docs in another
3. **Test data separated** - test-data/ for inputs, experiments/ for results
4. **Archive for one-offs** - Historical artifacts preserved but out of the way
5. **Deletable test outputs** - Can regenerate anytime with test scripts

## Implementation Order

1. Create directories
2. Move files (prompts → docs → test-data → archive)
3. Delete test-output directories
4. Update file references in code
5. Update .gitignore
6. Test that bin/test-variant.clj still works
7. Commit changes

## Verification

After cleanup, root directory should contain only:
```
README.md
CLAUDE.md
Makefile
deps.edn
build.clj
tests.edn
.gitignore
bin/
src/
test/
experiments/
plans/
docs/
claude-prompts/
test-data/
ARCHIVE/
OLD/
.git/
.claude/
.bd/
.beads/
```

Clean, organized, and easy to navigate!
