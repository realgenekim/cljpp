#!/bin/bash

# Comprehensive CLJ-PP Experiment Runner
# Tests all 20 programs with 3 different approaches:
# 1. Regular Clojure (baseline)
# 2. CLJ-PP with explicit POP only
# 3. CLJ-PP with POP-ALL (improved prompt v2)

set -e

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RESULTS_DIR="experiments/comprehensive-test-${TIMESTAMP}"
mkdir -p "${RESULTS_DIR}"

echo "========================================"
echo "Comprehensive CLJ-PP Experiment"
echo "Testing all 20 programs with 3 approaches"
echo "Results will be saved to: ${RESULTS_DIR}"
echo "========================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
clj_success=0
cljpp_pop_success=0
cljpp_popall_success=0

# Function to extract prompt for a specific program number
get_prompt() {
    local num=$1
    awk -v n="$num" '
        /^Program [0-9]+:/ {
            if (prog == n) exit
            prog = $2
            prog = substr(prog, 1, length(prog)-1)  # Remove colon
            if (prog == n) { printing=1; print; next }
        }
        printing && /^Program [0-9]+:/ { exit }
        printing { print }
    ' test-prompts.txt
}

# Test all 20 programs
for i in {1..20}; do
    PROGRAM_NUM=$(printf "%02d" "$i")

    echo "========================================" | tee -a "${RESULTS_DIR}/log.txt"
    echo "Testing Program ${PROGRAM_NUM}" | tee -a "${RESULTS_DIR}/log.txt"
    echo "========================================" | tee -a "${RESULTS_DIR}/log.txt"

    # Get the prompt for this program
    PROGRAM_PROMPT=$(get_prompt "$i")

    if [ -z "$PROGRAM_PROMPT" ]; then
        echo "Skipping ${PROGRAM_NUM} - no prompt found" | tee -a "${RESULTS_DIR}/log.txt"
        continue
    fi

    # Extract program description (first line)
    DESCRIPTION=$(echo "$PROGRAM_PROMPT" | head -1)
    echo "Description: $DESCRIPTION" | tee -a "${RESULTS_DIR}/log.txt"
    echo "" | tee -a "${RESULTS_DIR}/log.txt"

    # Create full program spec
    PROGRAM_SPEC="Write a Clojure program for this task:

$PROGRAM_PROMPT

Requirements:
- Use namespace examples.program${i}
- Write idiomatic Clojure
- Output ONLY the code, starting with (ns or PUSH-(
- No explanations or markdown"

    # Test 1: Regular Clojure
    echo "Test 1/3: Regular Clojure..." | tee -a "${RESULTS_DIR}/log.txt"
    claude --print "$PROGRAM_SPEC" > "${RESULTS_DIR}/${PROGRAM_NUM}-clj.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-clj.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-clj.clj"
    if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-clj.clj\")" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Regular Clojure: SUCCESS${NC}" | tee -a "${RESULTS_DIR}/log.txt"
        clj_success=$((clj_success + 1))
    else
        echo -e "${RED}✗ Regular Clojure: FAILED${NC}" | tee -a "${RESULTS_DIR}/log.txt"
    fi

    # Test 2: CLJ-PP with explicit POP only
    echo "Test 2/3: CLJ-PP (explicit POP)..." | tee -a "${RESULTS_DIR}/log.txt"
    PROMPT_POP=$(cat CLJPP-PROMPT.md)
    claude --print "${PROMPT_POP}

${PROGRAM_SPEC}" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.cljpp"
    if bin/cljpp "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.cljpp" "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.clj" 2>/dev/null; then
        if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-pop.clj\")" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ CLJ-PP (POP): SUCCESS${NC}" | tee -a "${RESULTS_DIR}/log.txt"
            cljpp_pop_success=$((cljpp_pop_success + 1))
        else
            echo -e "${RED}✗ CLJ-PP (POP): LOAD ERROR${NC}" | tee -a "${RESULTS_DIR}/log.txt"
        fi
    else
        echo -e "${RED}✗ CLJ-PP (POP): TRANSPILE ERROR${NC}" | tee -a "${RESULTS_DIR}/log.txt"
    fi

    # Test 3: CLJ-PP with POP-ALL (v2 improved prompt)
    echo "Test 3/3: CLJ-PP (POP-ALL v2)..." | tee -a "${RESULTS_DIR}/log.txt"
    PROMPT_POPALL=$(cat CLJPP-PROMPT-WITH-POP-ALL-ONLY-v2.md)
    claude --print "${PROMPT_POPALL}

${PROGRAM_SPEC}" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.raw" 2>&1
    sed -e '/^```clojure$/d' -e '/^```$/d' "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.raw" > "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.cljpp"
    if bin/cljpp "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.cljpp" "${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.clj" 2>/dev/null; then
        if clojure -M -e "(load-file \"${RESULTS_DIR}/${PROGRAM_NUM}-cljpp-popall.clj\")" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ CLJ-PP (POP-ALL): SUCCESS${NC}" | tee -a "${RESULTS_DIR}/log.txt"
            cljpp_popall_success=$((cljpp_popall_success + 1))
        else
            echo -e "${RED}✗ CLJ-PP (POP-ALL): LOAD ERROR${NC}" | tee -a "${RESULTS_DIR}/log.txt"
        fi
    else
        echo -e "${RED}✗ CLJ-PP (POP-ALL): TRANSPILE ERROR${NC}" | tee -a "${RESULTS_DIR}/log.txt"
    fi

    echo "" | tee -a "${RESULTS_DIR}/log.txt"
done

# Final Results
echo "========================================" | tee -a "${RESULTS_DIR}/log.txt"
echo "FINAL RESULTS" | tee -a "${RESULTS_DIR}/log.txt"
echo "========================================" | tee -a "${RESULTS_DIR}/log.txt"
echo "" | tee -a "${RESULTS_DIR}/log.txt"

clj_pct=$((clj_success * 100 / 20))
pop_pct=$((cljpp_pop_success * 100 / 20))
popall_pct=$((cljpp_popall_success * 100 / 20))

echo "Regular Clojure:        ${clj_success}/20 (${clj_pct}%)" | tee -a "${RESULTS_DIR}/log.txt"
echo "CLJ-PP (explicit POP):  ${cljpp_pop_success}/20 (${pop_pct}%)" | tee -a "${RESULTS_DIR}/log.txt"
echo "CLJ-PP (POP-ALL v2):    ${cljpp_popall_success}/20 (${popall_pct}%)" | tee -a "${RESULTS_DIR}/log.txt"
echo "" | tee -a "${RESULTS_DIR}/log.txt"

# Create summary file
cat > "${RESULTS_DIR}/RESULTS.md" << EOF
# Comprehensive CLJ-PP Experiment Results

**Date:** $(date)
**Test Set:** All 20 programs from test-output-clj-round2/

## Results Summary

| Approach | Success Rate | Percentage |
|----------|--------------|------------|
| Regular Clojure | ${clj_success}/20 | ${clj_pct}% |
| CLJ-PP (explicit POP) | ${cljpp_pop_success}/20 | ${pop_pct}% |
| CLJ-PP (POP-ALL v2) | ${cljpp_popall_success}/20 | ${popall_pct}% |

## Changes in This Experiment

1. **Removed CLAUDE.md contamination** - Minimal CLAUDE.md with no CLJ-PP details
2. **Improved POP-ALL prompt (v2)** - Added explicit warnings about POPs after POP-ALL
3. **Tested all 20 programs** - Full test suite instead of just factorial/fibonacci

## Key Improvements in POP-ALL v2 Prompt

- Added "THE CRITICAL RULE FOR POP-ALL" section
- Showed WRONG pattern explicitly (POPs after POP-ALL)
- Emphasized "POP-ALL means STOP"
- Added Error Example 1: "POPs After POP-ALL"
- Made it crystal clear that nothing comes after POP-ALL

## Detailed Logs

See log.txt for full output of all tests.

## Files Generated

- \`${i}-clj.clj\` - Regular Clojure output
- \`${i}-cljpp-pop.cljpp\` - CLJ-PP with explicit POP
- \`${i}-cljpp-pop.clj\` - Transpiled from POP variant
- \`${i}-cljpp-popall.cljpp\` - CLJ-PP with POP-ALL v2
- \`${i}-cljpp-popall.clj\` - Transpiled from POP-ALL variant
EOF

echo "Results saved to: ${RESULTS_DIR}/RESULTS.md"
echo "Full log available at: ${RESULTS_DIR}/log.txt"
